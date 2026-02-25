<template>
  <div class="page">
    <AppHeader @open-auth="authOpen = true" @logout="logout" @go-home="goHome" @search="onSearch" />

    <RightDock @go-home="goHome" @go-add="goAdd" @go-chat="goMessages" />

    <AuthDialog v-model="authOpen" @success="onAuthSuccess" />

    <div class="content">
      <div class="hero">
        <h1>社区技能广场</h1>
        <p>发现身边技能与服务，快速联系，安心选择。</p>
      </div>

      <div class="category-bar">
        <span v-for="cat in categories" :key="cat" :class="{ active: current === cat }" @click="current = cat">
          {{ cat }}
        </span>
      </div>

      <div v-if="current === NEARBY_CATEGORY" class="nearby-panel">
        <div class="nearby-title">附近技能定位</div>
        <LocationPicker map-height="260px" @change="onNearbyLocationChange" />
        <div class="nearby-hint">
          <span v-if="nearbyLocation">当前选点：{{ nearbyLocation.address }}</span>
          <span v-else>定位失败时可手动点击地图选点。</span>
        </div>
      </div>

      <div v-if="current === RECOMMEND_CATEGORY" class="recommend-panel">
        <div class="recommend-title">猜你喜欢</div>
      </div>

      <div class="market-grid" v-loading="listLoading">
        <div v-for="item in filteredList" :key="item.id" class="market-card" @click="goDetail(item.id)">
          <img :src="item.image || fallbackImage" />
          <div class="info">
            <p class="title">{{ item.title }}</p>
            <p class="meta">{{ displayCategory(item.category) }}</p>
            <p v-if="current === NEARBY_CATEGORY && hasDistance(item)" class="distance">距离 {{ formatDistance(item.distanceKm) }}</p>
            <div class="tag-row">
              <span class="nickname-badge">{{ item.sellerNickname || "用户" }}</span>
              <span class="credit-badge" :class="creditClass(item.sellerCreditScore)">信用：{{ displayScore(item.sellerCreditScore) }}</span>
            </div>
            <span class="price">售价 ¥{{ item.price }}</span>
          </div>
        </div>
      </div>

      <div v-if="current === RECOMMEND_CATEGORY && recommendHasMore && filteredList.length" class="recommend-more">
        <el-button :loading="recommendLoadingMore" round @click="loadMoreRecommend">加载更多推荐</el-button>
      </div>

      <el-empty v-if="!listLoading && filteredList.length === 0" :description="emptyText" />
    </div>
  </div>
</template>

<script setup lang="ts">
// 组件说明：首页模块。作用：展示技能列表、推荐与附近筛选。
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getNearbySkills, getRecommendList, getSkillList, logSearchEvent, type SkillId, type SkillItem } from "../api/skill";
import AppHeader from "../components/AppHeader.vue";
import AuthDialog from "../components/AuthDialog.vue";
import LocationPicker from "../components/LocationPicker.vue";
import RightDock from "../components/RightDock.vue";
import { useAuthStore } from "../store/auth";

interface LocationPayload {
  lng: number;
  lat: number;
  address: string;
  adcode: string;
  cityName: string;
}

const ALL_CATEGORY = "全部";
const RECOMMEND_CATEGORY = "推荐";
const NEARBY_CATEGORY = "附近";
const NEARBY_RADIUS_KM = 5;
const ENABLE_NEARBY_API = String(import.meta.env.VITE_ENABLE_NEARBY_API || "false").toLowerCase() === "true";

const categories = [ALL_CATEGORY, RECOMMEND_CATEGORY, "家教", "维修", "设计", "摄影", "编程", "跑腿", "其他", NEARBY_CATEGORY] as const;

const router = useRouter();
const auth = useAuthStore();

const authOpen = ref(false);
const fallbackImage = "https://via.placeholder.com/400x300?text=Skill";

const list = ref<SkillItem[]>([]);
const nearbyList = ref<SkillItem[]>([]);
const recommendList = ref<SkillItem[]>([]);
const current = ref<(typeof categories)[number]>(ALL_CATEGORY);
const keyword = ref("");
const listLoading = ref(false);

const nearbyLocation = ref<LocationPayload | null>(null);
const nearbyApiUnavailable = ref(false);
const nearbyFallbackNotified = ref(false);

const recommendCursor = ref<string | undefined>(undefined);
const recommendHasMore = ref(true);
const recommendLoadingMore = ref(false);

const filteredList = computed(() => {
  let source = list.value;
  if (current.value === NEARBY_CATEGORY) source = nearbyList.value;
  if (current.value === RECOMMEND_CATEGORY) source = recommendList.value;

  const key = keyword.value.trim().toLowerCase();
  return source.filter((item) => {
    const categoryMatch =
      current.value === ALL_CATEGORY ||
      current.value === NEARBY_CATEGORY ||
      current.value === RECOMMEND_CATEGORY ||
      displayCategory(item.category) === current.value;
    if (!categoryMatch) return false;
    if (!key) return true;
    return (
      String(item.title || "").toLowerCase().includes(key) ||
      String(item.description || "").toLowerCase().includes(key) ||
      String(item.category || "").toLowerCase().includes(key) ||
      String(item.address || "").toLowerCase().includes(key) ||
      String(item.sellerNickname || "").toLowerCase().includes(key)
    );
  });
});

const emptyText = computed(() => {
  if (current.value === NEARBY_CATEGORY && !nearbyLocation.value) {
    return "请先在上方地图选择定位点";
  }
  if (current.value === RECOMMEND_CATEGORY) {
    return "暂无推荐，先浏览或搜索一些技能试试";
  }
  return "没有匹配结果";
});

function haversineDistanceKm(lat1: number, lng1: number, lat2: number, lng2: number) {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const earth = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return 2 * earth * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function patchDistance(items: SkillItem[], location: LocationPayload | null) {
  if (!location) return items;
  return items.map((item) => {
    if (typeof item.distanceKm === "number" && Number.isFinite(item.distanceKm)) return item;
    if (typeof item.lng !== "number" || typeof item.lat !== "number") return item;
    if (!Number.isFinite(item.lng) || !Number.isFinite(item.lat)) return item;
    const computedDistance = haversineDistanceKm(location.lat, location.lng, item.lat, item.lng);
    return {
      ...item,
      distanceKm: computedDistance,
    };
  });
}

function toNearbyResult(items: SkillItem[]) {
  return items
    .filter((item) => typeof item.distanceKm === "number" && Number.isFinite(item.distanceKm) && item.distanceKm <= NEARBY_RADIUS_KM)
    .sort((a, b) => Number(a.distanceKm || 0) - Number(b.distanceKm || 0));
}

function isNearbyApiUnsupported(error: any) {
  const statusCode = Number(error?.response?.status || 0);
  if ([400, 404, 405, 501].includes(statusCode)) return true;
  const msg = String(error?.response?.data?.message || error?.message || "").toLowerCase();
  return msg.includes("failed to convert") || msg.includes("nearby");
}

async function loadNearbyByClientDistance() {
  const base = await getSkillList();
  nearbyList.value = toNearbyResult(patchDistance(base, nearbyLocation.value));
}

async function loadList() {
  try {
    listLoading.value = true;
    list.value = await getSkillList();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "加载技能列表失败");
  } finally {
    listLoading.value = false;
  }
}

async function loadNearbyList() {
  if (!nearbyLocation.value) return;
  try {
    listLoading.value = true;
    if (!ENABLE_NEARBY_API || nearbyApiUnavailable.value) {
      await loadNearbyByClientDistance();
      if (ENABLE_NEARBY_API && nearbyApiUnavailable.value && !nearbyFallbackNotified.value) {
        nearbyFallbackNotified.value = true;
        ElMessage.warning("nearby 接口不可用，已切换本地距离计算");
      }
      return;
    }

    const data = await getNearbySkills({
      lng: nearbyLocation.value.lng,
      lat: nearbyLocation.value.lat,
      radiusKm: NEARBY_RADIUS_KM,
    });
    nearbyList.value = toNearbyResult(patchDistance(data, nearbyLocation.value));
  } catch (error: any) {
    if (isNearbyApiUnsupported(error)) {
      nearbyApiUnavailable.value = true;
      await loadNearbyByClientDistance();
      if (!nearbyFallbackNotified.value) {
        nearbyFallbackNotified.value = true;
        ElMessage.warning("nearby 接口暂未就绪，已使用本地距离计算");
      }
      return;
    }
    ElMessage.error(error?.response?.data?.message || error?.message || "加载附近技能失败");
  } finally {
    listLoading.value = false;
  }
}

async function loadRecommend(reset = true) {
  if (recommendLoadingMore.value) return;
  try {
    if (reset) {
      listLoading.value = true;
      recommendCursor.value = undefined;
      recommendHasMore.value = true;
    } else {
      recommendLoadingMore.value = true;
      if (!recommendHasMore.value) return;
    }

    const res = await getRecommendList({
      cursor: reset ? undefined : recommendCursor.value,
      size: 20,
    });

    const data = Array.isArray(res.items) ? res.items : [];
    if (reset) {
      recommendList.value = data;
    } else {
      const merged = [...recommendList.value];
      const exists = new Set(merged.map((x) => String(x.id)));
      for (const item of data) {
        const id = String(item.id);
        if (!exists.has(id)) {
          exists.add(id);
          merged.push(item);
        }
      }
      recommendList.value = merged;
    }

    recommendCursor.value = res.nextCursor;
    recommendHasMore.value = Boolean(res.nextCursor);
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "加载推荐失败");
  } finally {
    listLoading.value = false;
    recommendLoadingMore.value = false;
  }
}

function loadMoreRecommend() {
  if (!recommendHasMore.value) return;
  void loadRecommend(false);
}

function onWindowScroll() {
  if (current.value !== RECOMMEND_CATEGORY) return;
  if (!recommendHasMore.value || recommendLoadingMore.value || listLoading.value) return;
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const viewport = window.innerHeight || document.documentElement.clientHeight;
  const fullHeight = document.documentElement.scrollHeight;
  if (scrollTop + viewport >= fullHeight - 120) {
    void loadRecommend(false);
  }
}

function onSearch(kw: string) {
  keyword.value = kw || "";
  const value = String(kw || "").trim();
  if (!value || !auth.token) return;
  void logSearchEvent(value).catch(() => undefined);
}

function goHome() {
  router.push("/home");
}

function goAdd() {
  if (!auth.token) {
    authOpen.value = true;
    return;
  }
  router.push("/skill/add");
}

function goMessages() {
  if (!auth.token) {
    authOpen.value = true;
    return;
  }
  router.push("/messages");
}

function goDetail(id: SkillId) {
  router.push(`/skills/${id}`);
}

function displayCategory(category?: string) {
  const value = String(category || "").trim();
  if (!value) return "其他";
  if (["家教", "维修", "设计", "摄影", "编程", "跑腿"].includes(value)) return value;
  if (value.includes("补习") || value.includes("辅导")) return "家教";
  if (value.includes("修") || value.includes("安装")) return "维修";
  if (value.includes("代码") || value.includes("开发")) return "编程";
  if (value.includes("拍") || value.includes("摄影")) return "摄影";
  if (value.includes("设计")) return "设计";
  if (value.includes("跑腿") || value.includes("代取") || value.includes("代办")) return "跑腿";
  return "其他";
}

function toScore(score?: number) {
  const n = Number(score ?? 0);
  return Number.isFinite(n) ? Math.max(0, Math.round(n)) : 0;
}

function displayScore(score?: number) {
  return toScore(score);
}

function creditClass(score?: number) {
  const n = toScore(score);
  if (n >= 80) return "credit-green";
  if (n >= 60) return "credit-blue";
  if (n >= 40) return "credit-yellow";
  return "credit-red";
}

function hasDistance(item: SkillItem) {
  return typeof item.distanceKm === "number" && Number.isFinite(item.distanceKm);
}

function formatDistance(distanceKm?: number) {
  if (typeof distanceKm !== "number" || !Number.isFinite(distanceKm)) return "";
  return `${Math.max(distanceKm, 0.1).toFixed(1)}km`;
}

function onNearbyLocationChange(payload: LocationPayload) {
  nearbyLocation.value = payload;
  if (current.value === NEARBY_CATEGORY) {
    void loadNearbyList();
  }
}

function logout() {
  auth.logout();
  router.push("/home");
}

async function onAuthSuccess() {
  if (current.value === NEARBY_CATEGORY && nearbyLocation.value) {
    await loadNearbyList();
    return;
  }
  if (current.value === RECOMMEND_CATEGORY) {
    await loadRecommend(true);
    return;
  }
  await loadList();
}

watch(
  () => current.value,
  (value) => {
    if (value === NEARBY_CATEGORY) {
      if (nearbyLocation.value) {
        void loadNearbyList();
      }
      return;
    }
    if (value === RECOMMEND_CATEGORY) {
      void loadRecommend(true);
      return;
    }
    if (!list.value.length) {
      void loadList();
    }
  }
);

onMounted(() => {
  void loadList();
  window.addEventListener("scroll", onWindowScroll, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", onWindowScroll);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-image: url("../assets/home-bg.jpg");
  background-size: cover;
  background-position: center;
}

.content {
  padding: 40px 60px;
  max-width: 1300px;
  margin: auto;
}

.hero {
  max-width: 720px;
  padding: 20px 24px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(6px);
  margin-bottom: 30px;
}

.hero h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
}

.hero p {
  margin: 0;
  color: #555;
}

.category-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  font-weight: 600;
  flex-wrap: wrap;
}

.category-bar span {
  cursor: pointer;
  transition: 0.2s;
}

.category-bar span:hover {
  color: #1677ff;
}

.category-bar .active {
  color: #1677ff;
}

.nearby-panel,
.recommend-panel {
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e2ebf5;
  border-radius: 16px;
  padding: 14px;
}

.nearby-title,
.recommend-title {
  margin-bottom: 8px;
  color: #1f2f40;
  font-size: 14px;
  font-weight: 700;
}

.nearby-hint,
.recommend-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #6f8195;
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.market-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  transition: 0.25s;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}

.market-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.market-card img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.info {
  padding: 14px;
}

.title {
  font-weight: 600;
  margin: 0 0 6px;
}

.meta {
  margin: 0 0 6px;
  color: #888;
  font-size: 12px;
}

.distance {
  margin: 0 0 8px;
  color: #1661d6;
  font-size: 12px;
  font-weight: 700;
}

.tag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.nickname-badge {
  color: #526071;
  background: #f5f7fb;
  border: 1px solid #dce3ef;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credit-badge {
  border: 1px solid;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
}

.credit-green {
  color: #1f8f46;
  border-color: #66c18a;
  background: #edf9f0;
}

.credit-blue {
  color: #1661d6;
  border-color: #77a6f2;
  background: #eef4ff;
}

.credit-yellow {
  color: #9a6b00;
  border-color: #efc255;
  background: #fff9e8;
}

.credit-red {
  color: #c73636;
  border-color: #f08b8b;
  background: #fff1f1;
}

.price {
  color: #ff5500;
  font-weight: bold;
}

.recommend-more {
  margin-top: 18px;
  text-align: center;
}
</style>

