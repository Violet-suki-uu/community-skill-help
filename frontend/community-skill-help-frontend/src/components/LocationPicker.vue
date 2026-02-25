<template>
  <div class="location-picker">
    <div class="input-wrap">
      <el-input
        v-model.trim="inputValue"
        :placeholder="placeholderText"
        clearable
        @input="onInputValueChange"
        @focus="onInputFocus"
        @clear="onInputClear"
      >
        <template #append>
          <el-button :loading="locating" @click="locateCurrentPosition">定位</el-button>
        </template>
      </el-input>

      <el-card v-if="showSuggestionPanel" class="suggestion-panel" shadow="never">
        <div class="suggestion-head">{{ suggestionTitle }}</div>
        <div v-if="suggestions.length" class="suggestion-list">
          <button v-for="item in suggestions" :key="item.id" class="suggestion-item" type="button" @mousedown.prevent="selectSuggestion(item)">
            <div class="name">{{ item.name }}</div>
            <div class="address">{{ item.address }}</div>
          </button>
        </div>
        <div v-else class="empty">暂无候选地点</div>
      </el-card>
    </div>

    <div ref="mapRef" class="map" :style="{ height: mapHeightStyle }"></div>

    <div class="footer">
      <span>{{ statusText }}</span>
      <span v-if="selectedPayload">经纬度：{{ selectedPayload.lng.toFixed(6) }}, {{ selectedPayload.lat.toFixed(6) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// 组件说明：地图选点组件。作用：完成地址搜索、地图点选与定位。
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from "vue";
import { ElMessage } from "element-plus";
import { loadAmapSdk } from "../utils/amap";

interface LocationPayload {
  lng: number;
  lat: number;
  address: string;
  adcode: string;
  cityName: string;
}

interface SuggestionItem {
  id: string;
  name: string;
  address: string;
  lng?: number;
  lat?: number;
  adcode?: string;
  cityName?: string;
}

const props = withDefaults(
  defineProps<{
    mapHeight?: string;
    placeholder?: string;
    autoLocate?: boolean;
    initialValue?: Partial<LocationPayload> | null;
  }>(),
  {
    mapHeight: "300px",
    placeholder: "请输入小区/学校/商圈",
    autoLocate: true,
    initialValue: null,
  }
);

const emit = defineEmits<{
  (event: "change", payload: LocationPayload): void;
}>();

const DEFAULT_CENTER: [number, number] = [116.397428, 39.90923];
const CANDIDATE_LIMIT = 10;

const mapRef = ref<HTMLDivElement | null>(null);
const mapInstance = shallowRef<any>(null);
const markerInstance = shallowRef<any>(null);
const geocoderInstance = shallowRef<any>(null);
const autoCompleteInstance = shallowRef<any>(null);
const placeSearchInstance = shallowRef<any>(null);

const inputValue = ref("");
const suggestionTitle = ref("该点附近推荐地址");
const suggestions = ref<SuggestionItem[]>([]);
const showSuggestionPanel = ref(false);
const locating = ref(false);
const statusText = ref("正在初始化地图...");
const selectedPayload = ref<LocationPayload | null>(null);
const mapReady = ref(false);

const mapHeightStyle = computed(() => props.mapHeight);
const placeholderText = computed(() => props.placeholder);

let skipKeywordSearch = false;

function createDebounce<T extends (...args: any[]) => void>(fn: T, delay: number) {
  let timer: number | null = null;
  const wrapped = (...args: Parameters<T>) => {
    if (timer !== null) {
      window.clearTimeout(timer);
    }
    timer = window.setTimeout(() => {
      timer = null;
      fn(...args);
    }, delay);
  };
  wrapped.cancel = () => {
    if (timer !== null) {
      window.clearTimeout(timer);
      timer = null;
    }
  };
  return wrapped as T & { cancel: () => void };
}

function createThrottle<T extends (...args: any[]) => void>(fn: T, delay: number) {
  let lastTime = 0;
  let timer: number | null = null;
  let trailingArgs: Parameters<T> | null = null;

  const run = (...args: Parameters<T>) => {
    const now = Date.now();
    const wait = delay - (now - lastTime);
    if (wait <= 0) {
      if (timer !== null) {
        window.clearTimeout(timer);
        timer = null;
      }
      lastTime = now;
      fn(...args);
      return;
    }

    trailingArgs = args;
    if (timer !== null) return;
    timer = window.setTimeout(() => {
      timer = null;
      lastTime = Date.now();
      if (trailingArgs) {
        fn(...trailingArgs);
        trailingArgs = null;
      }
    }, wait);
  };

  run.cancel = () => {
    if (timer !== null) {
      window.clearTimeout(timer);
      timer = null;
    }
    trailingArgs = null;
  };

  return run as T & { cancel: () => void };
}

const debouncedSearch = createDebounce((keyword: string) => {
  void searchByKeyword(keyword);
}, 300);

const throttledReGeocode = createThrottle((lng: number, lat: number, addressHint?: string, shouldOpenSuggestionPanel = true) => {
  void resolveAddressAndNearby(lng, lat, addressHint, shouldOpenSuggestionPanel);
}, 300);

function setInputSafely(value: string) {
  skipKeywordSearch = true;
  inputValue.value = value;
}

function formatCity(city: unknown, province: string) {
  if (typeof city === "string" && city.trim()) return city;
  if (Array.isArray(city) && city.length) {
    const first = city[0];
    if (typeof first === "string") return first;
  }
  return province || "";
}

function toNumber(value: unknown) {
  const n = Number(value);
  return Number.isFinite(n) ? n : NaN;
}

function extractLngLat(location: any): { lng: number; lat: number } | null {
  if (!location) return null;

  if (typeof location.getLng === "function" && typeof location.getLat === "function") {
    return { lng: Number(location.getLng()), lat: Number(location.getLat()) };
  }

  if (typeof location.lng === "number" && typeof location.lat === "number") {
    return { lng: location.lng, lat: location.lat };
  }

  if (Array.isArray(location) && location.length >= 2) {
    const lng = toNumber(location[0]);
    const lat = toNumber(location[1]);
    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      return { lng, lat };
    }
  }

  if (typeof location === "string" && location.includes(",")) {
    const [rawLng, rawLat] = location.split(",");
    const lng = toNumber(rawLng);
    const lat = toNumber(rawLat);
    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      return { lng, lat };
    }
  }

  return null;
}

function applyMarkerAndCenter(lng: number, lat: number, center = true) {
  if (!markerInstance.value || !mapInstance.value) return;
  markerInstance.value.setPosition([lng, lat]);
  if (center) {
    mapInstance.value.setCenter([lng, lat]);
  }
}

function emitSelection(payload: LocationPayload) {
  selectedPayload.value = payload;
  emit("change", payload);
}

async function geocodeByLngLat(lng: number, lat: number) {
  if (!geocoderInstance.value) throw new Error("Geocoder not ready");
  return new Promise<any>((resolve, reject) => {
    geocoderInstance.value.getAddress([lng, lat], (status: string, result: any) => {
      if (status === "complete" && result?.regeocode) {
        resolve(result.regeocode);
        return;
      }
      reject(new Error("Reverse geocode failed"));
    });
  });
}

async function searchAutoComplete(keyword: string) {
  if (!autoCompleteInstance.value) return [] as any[];
  return new Promise<any[]>((resolve) => {
    autoCompleteInstance.value.search(keyword, (status: string, result: any) => {
      if (status !== "complete") {
        resolve([]);
        return;
      }
      resolve(Array.isArray(result?.tips) ? result.tips : []);
    });
  });
}

async function searchPlace(keyword: string) {
  if (!placeSearchInstance.value) return [] as any[];
  return new Promise<any[]>((resolve) => {
    placeSearchInstance.value.search(keyword, (status: string, result: any) => {
      if (status !== "complete") {
        resolve([]);
        return;
      }
      resolve(Array.isArray(result?.poiList?.pois) ? result.poiList.pois : []);
    });
  });
}

function normalizeTipsToSuggestions(items: any[]) {
  return items
    .map((item: any, index: number): SuggestionItem => {
      const point = extractLngLat(item.location);
      const cityName = formatCity(item.cityname, "");
      const district = String(item.district || "").trim();
      const address = String(item.address || district || cityName || "未知地址");
      return {
        id: `tip-${index}-${item.id || item.name || ""}`,
        name: String(item.name || "未知地点"),
        address,
        lng: point?.lng,
        lat: point?.lat,
        adcode: String(item.adcode || ""),
        cityName,
      };
    })
    .filter((item: SuggestionItem) => item.name && item.name !== "未知地点")
    .slice(0, CANDIDATE_LIMIT);
}

function normalizePoisToSuggestions(items: any[]) {
  return items
    .map((poi: any, index: number): SuggestionItem => {
      const point = extractLngLat(poi.location);
      const district = String(poi.address || poi.district || "").trim();
      return {
        id: `poi-${index}-${poi.id || poi.name || ""}`,
        name: String(poi.name || "附近地点"),
        address: district || String(poi.type || "附近地点"),
        lng: point?.lng,
        lat: point?.lat,
        adcode: String(poi.adcode || ""),
        cityName: String(poi.cityname || poi.city || ""),
      };
    })
    .slice(0, CANDIDATE_LIMIT);
}

async function resolveAddressAndNearby(lng: number, lat: number, addressHint?: string, shouldOpenSuggestionPanel = true) {
  try {
    const regeocode = await geocodeByLngLat(lng, lat);
    const component = regeocode.addressComponent || {};
    const cityName = formatCity(component.city, String(component.province || ""));
    const adcode = String(component.adcode || "");
    const formatted = String(regeocode.formattedAddress || "");
    const address = addressHint || formatted || `${lng.toFixed(6)}, ${lat.toFixed(6)}`;

    setInputSafely(address);
    suggestionTitle.value = "该点附近推荐地址";
    suggestions.value = normalizePoisToSuggestions(Array.isArray(regeocode.pois) ? regeocode.pois : []);
    showSuggestionPanel.value = shouldOpenSuggestionPanel && suggestions.value.length > 0;

    emitSelection({
      lng,
      lat,
      address,
      adcode,
      cityName,
    });

    statusText.value = "已完成选点，可继续微调。";
  } catch {
    const fallbackAddress = addressHint || `${lng.toFixed(6)}, ${lat.toFixed(6)}`;
    setInputSafely(fallbackAddress);
    suggestions.value = [];
    showSuggestionPanel.value = shouldOpenSuggestionPanel;
    emitSelection({
      lng,
      lat,
      address: fallbackAddress,
      adcode: "",
      cityName: "",
    });
    statusText.value = "地址解析失败，请手动选择。";
    ElMessage.warning("地址解析失败，请手动选择");
  }
}

async function moveToCoordinate(lng: number, lat: number, addressHint?: string, shouldOpenSuggestionPanel = true) {
  applyMarkerAndCenter(lng, lat, true);
  throttledReGeocode(lng, lat, addressHint, shouldOpenSuggestionPanel);
}

async function locateByBrowser() {
  if (!navigator.geolocation) {
    throw new Error("Browser geolocation unavailable");
  }

  return new Promise<{ lng: number; lat: number }>((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          lng: position.coords.longitude,
          lat: position.coords.latitude,
        });
      },
      () => {
        reject(new Error("Browser geolocation rejected"));
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000,
      }
    );
  });
}

async function locateByAmap() {
  if (!mapInstance.value || !window.AMap) {
    throw new Error("AMap geolocation unavailable");
  }

  return new Promise<{ lng: number; lat: number }>((resolve, reject) => {
    window.AMap.plugin("AMap.Geolocation", () => {
      const geolocation = new window.AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000,
        zoomToAccuracy: false,
      });

      geolocation.getCurrentPosition((status: string, result: any) => {
        if (status === "complete" && result?.position) {
          const point = extractLngLat(result.position);
          if (point) {
            resolve(point);
            return;
          }
        }
        reject(new Error("AMap geolocation failed"));
      });
    });
  });
}

async function locateCurrentPosition() {
  if (!mapReady.value) return;
  locating.value = true;
  statusText.value = "正在定位...";
  try {
    const point = await locateByBrowser();
    await moveToCoordinate(point.lng, point.lat);
  } catch {
    try {
      const point = await locateByAmap();
      await moveToCoordinate(point.lng, point.lat);
    } catch {
      statusText.value = "定位失败，请手动选择。";
      ElMessage.warning("定位失败，请手动选择");
    }
  } finally {
    locating.value = false;
  }
}

async function searchByKeyword(keyword: string) {
  const normalized = keyword.trim();
  if (!normalized) return;

  suggestionTitle.value = "关键词联想结果";
  const tips = normalizeTipsToSuggestions(await searchAutoComplete(normalized));
  if (tips.length) {
    suggestions.value = tips;
    showSuggestionPanel.value = true;
    return;
  }

  const pois = normalizeTipsToSuggestions(await searchPlace(normalized));
  suggestions.value = pois;
  showSuggestionPanel.value = true;
}

async function resolveSuggestionCoordinates(item: SuggestionItem) {
  if (typeof item.lng === "number" && typeof item.lat === "number") {
    return { lng: item.lng, lat: item.lat };
  }

  const pois = await searchPlace(item.name);
  const target = pois.find((poi) => extractLngLat(poi.location));
  if (!target) {
    return null;
  }
  return extractLngLat(target.location);
}

async function selectSuggestion(item: SuggestionItem) {
  const point = await resolveSuggestionCoordinates(item);
  if (!point) {
    ElMessage.warning("无法解析该地点坐标，请选择其他候选");
    return;
  }

  showSuggestionPanel.value = false;
  const hint = [item.name, item.address].filter(Boolean).join(" ");
  await moveToCoordinate(point.lng, point.lat, hint, false);
}

function onInputValueChange(value: string) {
  if (skipKeywordSearch) {
    skipKeywordSearch = false;
    return;
  }

  const keyword = value.trim();
  if (!keyword) {
    suggestionTitle.value = "该点附近推荐地址";
    showSuggestionPanel.value = suggestions.value.length > 0;
    return;
  }
  debouncedSearch(keyword);
}

function onInputFocus() {
  showSuggestionPanel.value = suggestions.value.length > 0;
}

function onInputClear() {
  suggestions.value = [];
  showSuggestionPanel.value = false;
}

function onMapClick(event: any) {
  const point = extractLngLat(event?.lnglat);
  if (!point) return;
  void moveToCoordinate(point.lng, point.lat);
}

async function initMap() {
  if (!mapRef.value) return;
  const AMap = await loadAmapSdk();

  mapInstance.value = new AMap.Map(mapRef.value, {
    zoom: 14,
    center: DEFAULT_CENTER,
  });

  markerInstance.value = new AMap.Marker({
    position: DEFAULT_CENTER,
    anchor: "bottom-center",
  });
  mapInstance.value.add(markerInstance.value);
  mapInstance.value.on("click", onMapClick);

  geocoderInstance.value = new AMap.Geocoder({
    extensions: "all",
  });

  autoCompleteInstance.value = new AMap.AutoComplete({
    city: "全国",
    citylimit: false,
  });

  placeSearchInstance.value = new AMap.PlaceSearch({
    city: "全国",
    citylimit: false,
    pageSize: CANDIDATE_LIMIT,
    pageIndex: 1,
  });

  mapReady.value = true;
  statusText.value = "地图已就绪，请选择地址。";

  const initialLng = toNumber(props.initialValue?.lng);
  const initialLat = toNumber(props.initialValue?.lat);
  if (Number.isFinite(initialLng) && Number.isFinite(initialLat)) {
    const addressHint = String(props.initialValue?.address || "");
    await moveToCoordinate(initialLng, initialLat, addressHint || undefined);
    return;
  }

  if (props.autoLocate) {
    await locateCurrentPosition();
  }
}

watch(
  () => props.initialValue,
  (value) => {
    if (!value || !mapReady.value) return;
    const lng = toNumber(value.lng);
    const lat = toNumber(value.lat);
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return;
    const addressHint = String(value.address || "");
    void moveToCoordinate(lng, lat, addressHint || undefined);
  },
  { deep: true }
);

onMounted(() => {
  void initMap().catch(() => {
    statusText.value = "地图初始化失败，请刷新页面重试。";
    ElMessage.error("地图初始化失败，请刷新后重试");
  });
});

onBeforeUnmount(() => {
  debouncedSearch.cancel();
  throttledReGeocode.cancel();

  if (mapInstance.value) {
    mapInstance.value.off("click", onMapClick);
    mapInstance.value.destroy();
  }

  mapInstance.value = null;
  markerInstance.value = null;
  geocoderInstance.value = null;
  autoCompleteInstance.value = null;
  placeSearchInstance.value = null;
});
</script>

<style scoped>
.location-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-wrap {
  position: relative;
}

.suggestion-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 8px);
  border-radius: 12px;
  border: 1px solid #dbe5f1;
  z-index: 20;
}

.suggestion-head {
  font-size: 12px;
  color: #6c7a89;
  margin-bottom: 6px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  max-height: 240px;
  overflow: auto;
}

.suggestion-item {
  border: none;
  background: #fff;
  text-align: left;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.suggestion-item:hover {
  background: #f3f7ff;
}

.suggestion-item .name {
  color: #1f2f40;
  font-weight: 600;
  line-height: 1.4;
}

.suggestion-item .address {
  color: #7f8fa2;
  font-size: 12px;
  line-height: 1.4;
}

.empty {
  padding: 4px 0;
  color: #8898aa;
  font-size: 13px;
}

.map {
  width: 100%;
  border-radius: 14px;
  border: 1px solid #dbe5f1;
  overflow: hidden;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #6f8195;
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

