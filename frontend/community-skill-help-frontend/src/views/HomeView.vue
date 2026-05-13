<template>
  <div class="page">
    <AppHeader @open-auth="authOpen = true" @logout="logout" @go-home="goHome" @search="onSearch" />

    <RightDock @go-home="goHome" @go-add="goAdd" @go-chat="goMessages" />

    <AuthDialog v-model="authOpen" @success="onAuthSuccess" />

    <div class="content">
      <div class="hero">
        <h1>社区技能广场</h1>
        <p>发现身边技能与服务，快速联系，安心选择</p>
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
            <p class="desc">{{ item.description || "专业服务，响应及时，沟通透明" }}</p>
            <p v-if="current === NEARBY_CATEGORY && hasDistance(item)" class="distance">距离 {{ formatDistance(item.distanceKm) }}</p>
            <div class="tag-row">
              <span class="nickname-badge">{{ item.sellerNickname || "用户" }}</span>
              <span class="credit-badge" :class="creditClass(item.sellerCreditScore)">★ {{ displayScore(item.sellerCreditScore) }}</span>
            </div>
            <span class="price">¥{{ item.price }} <small>起</small></span>
          </div>
        </div>
      </div>

      <div class="trust-strip">
        <div class="trust-item">
          <span class="trust-icon">✓</span>
          <div>
            <strong>真实可靠</strong>
            <p>实名认证，放心选择</p>
          </div>
        </div>
        <div class="trust-item">
          <span class="trust-icon">⌖</span>
          <div>
            <strong>就在身边</strong>
            <p>同城服务，快速响应</p>
          </div>
        </div>
        <div class="trust-item">
          <span class="trust-icon">☆</span>
          <div>
            <strong>评价透明</strong>
            <p>真实评分，安心参考</p>
          </div>
        </div>
        <div class="trust-item">
          <span class="trust-icon heart">♡</span>
          <div>
            <strong>互帮互助</strong>
            <p>温暖社区，共建美好</p>
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
  const base = await getSkillList({ page: 1, size: 300 });
  nearbyList.value = toNearbyResult(patchDistance(base, nearbyLocation.value));
}

async function loadList() {
  try {
    listLoading.value = true;
    list.value = await getSkillList({ page: 1, size: 300 });
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
  position: relative;
  min-height: 100vh;
  background:
    linear-gradient(90deg, rgba(255, 248, 239, 0.66) 0%, rgba(255, 255, 255, 0.48) 42%, rgba(255, 242, 226, 0.28) 100%),
    url("../assets/home-bg-community.png");
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  overflow-x: hidden;
}

.page::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 249, 240, 0.68) 100%),
    radial-gradient(circle at 15% 62%, rgba(255, 255, 255, 0.6), transparent 18%);
  z-index: 0;
}

.page::after {
  content: "";
  position: fixed;
  inset: auto 0 0;
  height: 34vh;
  pointer-events: none;
  z-index: 0;
  background: linear-gradient(0deg, rgba(255, 246, 234, 0.7), transparent);
}

.content {
  position: relative;
  z-index: 1;
  padding: 48px 24px 64px;
  max-width: 980px;
  margin: auto;
}

.hero {
  min-height: 190px;
  padding: 44px 40px;
  margin-bottom: 20px;
  border-radius: 22px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.94) 0%, rgba(255, 255, 255, 0.74) 48%, rgba(255, 255, 255, 0.28) 100%),
    url("../assets/workspace-bg.png");
  background-size: cover;
  background-position: center right;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 46px rgba(36, 55, 85, 0.13);
  backdrop-filter: blur(18px);
  animation: home-rise 0.48s ease both;
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.2), transparent 58%);
  pointer-events: none;
}

.hero h1 {
  margin: 0 0 14px;
  color: #14243d;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.12;
  position: relative;
  z-index: 1;
}

.hero p {
  margin: 0;
  color: #657189;
  font-size: 16px;
  font-weight: 500;
  position: relative;
  z-index: 1;
}

.category-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  font-weight: 600;
  flex-wrap: wrap;
  padding: 10px 12px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(18px);
  box-shadow: 0 14px 38px rgba(36, 55, 85, 0.1);
  animation: home-rise 0.48s ease 0.08s both;
}

.category-bar span {
  cursor: pointer;
  padding: 11px 24px;
  border-radius: 999px;
  color: #526075;
  transition: transform 0.2s ease, color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.category-bar span:hover {
  color: #2f6df6;
  background: rgba(47, 109, 246, 0.08);
  transform: translateY(-2px);
}

.category-bar .active {
  color: #fff;
  background: linear-gradient(135deg, #3b79ff, #2567ee);
  box-shadow: 0 10px 22px rgba(37, 103, 238, 0.25);
}

.nearby-panel,
.recommend-panel {
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 20px;
  padding: 16px;
  backdrop-filter: blur(16px);
  box-shadow: 0 18px 46px rgba(63, 135, 130, 0.11);
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.market-card {
  min-height: 170px;
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  box-shadow: 0 16px 38px rgba(36, 55, 85, 0.11);
  cursor: pointer;
  backdrop-filter: blur(16px);
  animation: card-in 0.42s ease both;
}

.market-card:hover {
  transform: translateY(-5px);
  border-color: rgba(47, 109, 246, 0.22);
  box-shadow: 0 24px 56px rgba(36, 55, 85, 0.17);
}

.market-card:active {
  transform: translateY(-2px) scale(0.99);
}

.market-card img {
  width: 100%;
  height: 104px;
  object-fit: contain;
  display: block;
  background: transparent;
  transition: transform 0.35s ease;
}

.market-card:hover img {
  transform: scale(1.045);
}

.info {
  min-width: 0;
  padding: 0;
}

.title {
  font-weight: 800;
  margin: 0 0 8px;
  color: #1d2c44;
  line-height: 1.32;
  font-size: 17px;
}

.meta {
  display: none;
}

.desc {
  margin: 0 0 10px;
  color: #6d788d;
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.distance {
  margin: 0 0 8px;
  color: #2567ee;
  font-size: 12px;
  font-weight: 700;
}

.tag-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 9px;
}

.nickname-badge {
  color: #606d80;
  background: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.3;
  max-width: 92px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credit-badge {
  border: none;
  border-radius: 0;
  padding: 0;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
}

.credit-green {
  color: #f2a30b;
  background: transparent;
}

.credit-blue {
  color: #f2a30b;
  background: transparent;
}

.credit-yellow {
  color: #f2a30b;
  background: transparent;
}

.credit-red {
  color: #f2a30b;
  background: transparent;
}

.price {
  color: #ff7a1a;
  font-weight: bold;
  font-size: 17px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border-radius: 0;
  background: transparent;
}

.price small {
  color: #8a93a5;
  font-size: 12px;
  font-weight: 500;
}

.trust-strip {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  padding: 22px 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 16px 40px rgba(36, 55, 85, 0.1);
  backdrop-filter: blur(16px);
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.trust-icon {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #3b79ff;
  border: 2px solid rgba(59, 121, 255, 0.78);
  font-weight: 900;
  font-size: 18px;
}

.trust-icon.heart {
  color: #ff8f77;
  border-color: rgba(255, 143, 119, 0.78);
}

.trust-item strong {
  display: block;
  color: #26364e;
  font-size: 14px;
  margin-bottom: 3px;
}

.trust-item p {
  margin: 0;
  color: #788397;
  font-size: 12px;
  line-height: 1.4;
}

.recommend-more {
  margin-top: 18px;
  text-align: center;
}

:deep(.el-empty) {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(14px);
}

@keyframes home-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 760px) {
  .page {
    background-attachment: scroll;
    background-position: center;
  }

  .content {
    padding: 24px 14px 44px;
  }

  .hero {
    min-height: 150px;
    padding: 28px 22px;
    border-radius: 18px;
    background-position: 64% center;
  }

  .hero h1 {
    font-size: 28px;
  }

  .hero p {
    font-size: 14px;
  }

  .category-bar {
    gap: 8px;
    padding: 8px;
    border-radius: 18px;
  }

  .category-bar span {
    padding: 8px 12px;
    font-size: 13px;
  }

  .market-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .market-card {
    grid-template-columns: 106px minmax(0, 1fr);
    min-height: 150px;
    padding: 16px;
  }

  .market-card img {
    height: 96px;
  }

  .trust-strip {
    grid-template-columns: 1fr 1fr;
    gap: 14px;
    padding: 18px;
  }

  .trust-item {
    align-items: flex-start;
  }
}
</style>
