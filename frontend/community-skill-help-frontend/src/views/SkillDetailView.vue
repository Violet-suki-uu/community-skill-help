<template>
  <div class="detail-page">
    <div class="detail-card" v-if="detail">
      <div class="left">
        <img :src="detail.image || fallbackImage" class="cover" />
      </div>
      <div class="right">
        <div class="top-row">
          <h1 class="title">{{ detail.title }}</h1>
          <div class="credit-wrap" :style="creditStyle">信誉：{{ detail.sellerCreditScore }}</div>
        </div>
        <div class="seller-line">发布者：{{ detail.sellerNickname || "用户" }}</div>
        <div class="meta-line">
          <span>{{ detail.category || "未分类" }}</span>
          <span class="dot">·</span>
          <span>售价 {{ detail.price }}</span>
        </div>
        <div class="desc">{{ detail.description || "暂无描述" }}</div>

        <div class="actions">
          <el-button round @click="goHome">返回首页</el-button>
          <el-button type="primary" round @click="contactSeller">联系卖家</el-button>
        </div>
      </div>
    </div>

    <div class="reservation-card" v-if="auth.token && detail">
      <div class="reservation-head">
        <h3>该技能的预约记录</h3>
        <el-button text @click="loadReservations">刷新</el-button>
      </div>

      <div v-loading="reservationLoading">
        <div v-if="reservations.length" class="reservation-list">
          <div v-for="item in reservations" :key="item.id" class="reservation-item">
            <div class="line">
              <span class="name">{{ item.buyerName }} → {{ item.sellerName }}</span>
              <span class="status" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
            </div>
            <div class="sub">地址：{{ item.address || "-" }}</div>
            <div class="sub">电话：{{ item.phone || "-" }}</div>
            <div class="sub">备注：{{ item.note || "-" }}</div>
            <div class="time">{{ formatTime(item.createdAt) }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无预约记录" />
      </div>
    </div>

    <el-empty v-if="!detail" description="技能不存在或加载失败" />
  </div>
</template>

<script setup lang="ts">
// 组件说明：技能详情页模块。作用：展示技能细节并发起联系与预约。
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { openConversation } from "../api/chat";
import { getSkillDetail, type SkillDetail } from "../api/skill";
import { getSkillReservations, type ReservationItem } from "../api/reservation";
import { getMeApi } from "../api/user";
import { useAuthStore } from "../store/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const detail = ref<SkillDetail | null>(null);
const reservations = ref<ReservationItem[]>([]);
const reservationLoading = ref(false);
const currentUserId = ref("");
const fallbackImage = "https://via.placeholder.com/720x480?text=Skill";

const skillId = computed(() => String(route.params.id || ""));

const creditStyle = computed(() => {
  const score = detail.value?.sellerCreditScore ?? 0;
  if (score >= 80) {
    return { color: "#1f8f46", borderColor: "#66c18a", background: "#edf9f0" };
  }
  if (score >= 60) {
    return { color: "#1661d6", borderColor: "#77a6f2", background: "#eef4ff" };
  }
  if (score >= 40) {
    return { color: "#9a6b00", borderColor: "#efc255", background: "#fff9e8" };
  }
  return { color: "#c73636", borderColor: "#f08b8b", background: "#fff1f1" };
});

async function loadMe() {
  if (!auth.token) {
    currentUserId.value = "";
    return;
  }
  try {
    const res = await getMeApi();
    currentUserId.value = String(res?.data?.data?.id || "");
  } catch {
    currentUserId.value = "";
  }
}

async function loadDetail() {
  if (!skillId.value) return;
  try {
    detail.value = await getSkillDetail(skillId.value);
  } catch (error: any) {
    detail.value = null;
    ElMessage.error(error?.response?.data?.message || error?.message || "加载技能详情失败");
  }
}

async function loadReservations() {
  if (!auth.token || !skillId.value) {
    reservations.value = [];
    return;
  }
  try {
    reservationLoading.value = true;
    reservations.value = await getSkillReservations(skillId.value);
  } catch (error: any) {
    reservations.value = [];
    ElMessage.error(error?.response?.data?.message || error?.message || "加载预约记录失败");
  } finally {
    reservationLoading.value = false;
  }
}

function goHome() {
  router.push("/home");
}

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(
    date.getHours()
  ).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function statusText(status?: string) {
  const value = String(status || "").toUpperCase();
  if (value === "AWAIT_BUYER_CONFIRM") return "待买家确认";
  if (value === "COMPLETED") return "已完成";
  if (value === "CANCELED") return "已取消";
  if (value === "PENDING") return "待服务";
  return "待处理";
}

function statusClass(status?: string) {
  const value = String(status || "").toUpperCase();
  if (value === "AWAIT_BUYER_CONFIRM") return "wait";
  if (value === "COMPLETED") return "ok";
  if (value === "CANCELED") return "cancel";
  return "pending";
}

async function contactSeller() {
  if (!detail.value) return;
  if (!auth.token) {
    ElMessage.warning("请先登录");
    return;
  }
  if (currentUserId.value && String(detail.value.userId) === currentUserId.value) {
    ElMessage.warning("这是你发布的技能，不能联系自己");
    return;
  }
  try {
    const cid = await openConversation(String(detail.value.id));
    router.push({ path: "/messages", query: { cid: String(cid) } });
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "发起会话失败");
  }
}

onMounted(async () => {
  await loadMe();
  await loadDetail();
  await loadReservations();
});
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 32px;
  background: radial-gradient(circle at 0 0, #f3f8ff 0, #f7f9fc 60%, #eef2f8 100%);
}

.detail-card {
  width: min(1100px, 96vw);
  margin: 0 auto;
  background: #fff;
  border-radius: 22px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 14px 38px rgba(20, 40, 70, 0.12);
  padding: 24px;
  display: grid;
  grid-template-columns: 48% 1fr;
  gap: 24px;
}

.cover {
  width: 100%;
  height: 100%;
  min-height: 340px;
  border-radius: 16px;
  object-fit: cover;
}

.right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  margin: 0;
  font-size: 28px;
  color: #1f2f40;
}

.credit-wrap {
  border: 2px solid;
  border-radius: 999px;
  padding: 6px 14px;
  font-weight: 700;
  font-size: 14px;
  white-space: nowrap;
}

.seller-line {
  color: #33475b;
  font-size: 15px;
  font-weight: 600;
}

.meta-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6d7f92;
  font-size: 14px;
}

.dot {
  opacity: 0.5;
}

.desc {
  margin-top: 6px;
  color: #2f4256;
  line-height: 1.7;
  font-size: 15px;
  background: #f7fbff;
  border: 1px solid #e6eef7;
  border-radius: 12px;
  padding: 12px 14px;
  min-height: 120px;
}

.actions {
  margin-top: auto;
  display: flex;
  gap: 10px;
}

.reservation-card {
  width: min(1100px, 96vw);
  margin: 16px auto 0;
  background: #fff;
  border-radius: 18px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 10px 26px rgba(20, 40, 70, 0.1);
  padding: 16px;
}

.reservation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reservation-head h3 {
  margin: 0;
  font-size: 18px;
}

.reservation-list {
  margin-top: 10px;
  display: grid;
  gap: 10px;
}

.reservation-item {
  border: 1px solid #e6eef7;
  border-radius: 12px;
  padding: 10px 12px;
  background: #f9fcff;
}

.line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.name {
  font-weight: 700;
  color: #29415f;
}

.status {
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  border: 1px solid;
}

.status.pending {
  color: #1661d6;
  border-color: #8eb9ff;
  background: #eef4ff;
}

.status.wait {
  color: #8c5500;
  border-color: #f2c979;
  background: #fff7e6;
}

.status.ok {
  color: #1f8f46;
  border-color: #66c18a;
  background: #edf9f0;
}

.status.cancel {
  color: #c73636;
  border-color: #f09a9a;
  background: #fff1f1;
}

.sub {
  margin-top: 4px;
  color: #51657a;
  font-size: 13px;
}

.time {
  margin-top: 6px;
  font-size: 12px;
  color: #8898ab;
}

@media (max-width: 900px) {
  .detail-page {
    padding: 14px;
  }
  .detail-card {
    grid-template-columns: 1fr;
    padding: 14px;
    gap: 14px;
  }
  .title {
    font-size: 22px;
  }
}
</style>

