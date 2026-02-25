<template>
  <div class="page">
    <AppHeader @open-auth="authOpen = true" @logout="logout" @go-home="goHome" @search="onSearch" />

    <RightDock @go-home="goHome" @go-add="goAdd" @go-list="goManage" />

    <AuthDialog v-model="authOpen" @success="onAuthSuccess" />

    <div class="content">
      <div class="hero">
        <h1>社区技能广场</h1>
        <p>发现身边的技能与服务，简单发布，快速联系。</p>
      </div>

      <div class="category-bar">
        <span
          v-for="cat in categories"
          :key="cat"
          :class="{ active: current === cat }"
          @click="current = cat"
        >
          {{ cat }}
        </span>
      </div>

      <div class="market-grid">
        <div v-for="item in filteredList" :key="item.id" class="market-card">
          <img :src="item.image || fallbackImage" />
          <div class="info">
            <p class="title">{{ item.title }}</p>
            <p class="meta">{{ item.category || "未分类" }}</p>
            <span class="price">¥{{ item.price }}</span>
          </div>
        </div>
      </div>

      <el-empty v-if="filteredList.length === 0" description="没有匹配结果" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../store/auth";
import AppHeader from "../components/AppHeader.vue";
import RightDock from "../components/RightDock.vue";
import AuthDialog from "../components/AuthDialog.vue";
import { getSkillList, type SkillItem } from "../api/skill";

const router = useRouter();
const auth = useAuthStore();

const authOpen = ref(false);
const fallbackImage = "https://via.placeholder.com/400x300?text=Skill";

const list = ref<SkillItem[]>([]);
const categories = ["全部", "家教", "维修", "设计", "摄影", "编程"];
const current = ref("全部");
const keyword = ref("");

const filteredList = computed(() => {
  return list.value.filter((item) => {
    const categoryMatch = current.value === "全部" || item.category === current.value;
    const key = keyword.value.trim().toLowerCase();
    if (!key) return categoryMatch;
    const keywordMatch =
      String(item.title || "").toLowerCase().includes(key) ||
      String(item.description || "").toLowerCase().includes(key) ||
      String(item.category || "").toLowerCase().includes(key);
    return categoryMatch && keywordMatch;
  });
});

async function loadList() {
  list.value = await getSkillList();
}

function onSearch(kw: string) {
  keyword.value = kw || "";
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

function goManage() {
  goAdd();
}

function logout() {
  auth.logout();
  router.push("/home");
}

function onAuthSuccess() {
  loadList();
}

onMounted(loadList);
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
  gap: 26px;
  margin-bottom: 26px;
  font-weight: 600;
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

.price {
  color: #ff5500;
  font-weight: bold;
}
</style>
