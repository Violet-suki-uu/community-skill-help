<template>
  <div class="publish-page">
    <div class="page-header">
      <h2>发布技能</h2>
      <el-button class="back-btn" round @click="goHome">返回首页</el-button>
    </div>

    <div class="form-card">
      <el-input v-model.trim="form.title" placeholder="技能标题" maxlength="50" show-word-limit />
      <el-input v-model.number="form.price" placeholder="价格（元）" type="number" />
      <el-input v-model.trim="form.category" placeholder="分类（如：家教/维修/设计）" maxlength="20" />
      <el-input v-model.trim="form.description" placeholder="技能描述" type="textarea" :rows="3" maxlength="300" show-word-limit />

      <div class="location-field">
        <div class="location-title">服务地址</div>
        <LocationPicker map-height="280px" @change="onFormLocationChange" />
        <div v-if="form.address && typeof form.lng === 'number' && typeof form.lat === 'number'" class="location-preview">
          {{ form.address }}（{{ form.lng.toFixed(6) }}, {{ form.lat.toFixed(6) }}）
        </div>
      </div>

      <div class="upload-row">
        <el-upload
          class="uploader"
          :show-file-list="false"
          :http-request="doUpload"
          :before-upload="beforeUpload"
          accept="image/*"
        >
          <img v-if="form.image" :src="form.image" class="preview-image" />
          <el-button v-else type="primary" plain>上传图片</el-button>
        </el-upload>
        <div class="upload-tip">
          <div class="tip-title">建议上传清晰封面</div>
          <div class="tip-desc">支持 JPG/PNG，最大 5MB</div>
        </div>
      </div>

      <el-button class="submit-btn" type="primary" :loading="submitting" @click="submit">发布技能</el-button>
    </div>

    <h2 class="manage-title">我的技能</h2>
    <div class="filter-bar">
      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="on">上架中</el-radio-button>
        <el-radio-button label="off">已下架</el-radio-button>
      </el-radio-group>
    </div>

    <div class="manage-grid">
      <div v-for="item in filteredList" :key="item.id" class="manage-card">
        <img :src="item.image || fallbackImage" />
        <div class="info">
          <p class="title">{{ item.title }}</p>
          <span class="price">¥{{ item.price }}</span>
          <p class="meta">{{ item.category || "未分类" }} | {{ item.status === 1 ? "上架中" : "已下架" }}</p>
        </div>
        <div class="actions">
          <el-button size="small" @click="openEdit(item)">编辑</el-button>
          <el-button size="small" type="warning" @click="toggleStatus(item)">
            {{ item.status === 1 ? "下架" : "上架" }}
          </el-button>
          <el-button size="small" type="danger" @click="removeItem(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="filteredList.length === 0" description="暂无技能" />

    <h2 class="manage-title">预约管理</h2>
    <div class="filter-bar">
      <el-radio-group v-model="reservationTab" size="small">
        <el-radio-button label="seller">预约我的</el-radio-button>
        <el-radio-button label="buyer">我预约的</el-radio-button>
      </el-radio-group>
      <el-button class="refresh-btn" size="small" round @click="loadReservations">刷新</el-button>
    </div>

    <h3 class="sub-title">进行中</h3>
    <div class="reservation-grid">
      <div v-for="item in activeReservationList" :key="item.id" class="reservation-card">
        <div class="reservation-head">
          <div class="skill-name">{{ item.skillTitle }}</div>
          <span class="status-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
        </div>
        <div class="reservation-line">
          <span>买家：{{ item.buyerName }}</span>
          <span>卖家：{{ item.sellerName }}</span>
        </div>
        <div class="reservation-line">地址：{{ item.address || "-" }}</div>
        <div class="reservation-line">电话：{{ item.phone || "-" }}</div>
        <div class="reservation-line">备注：{{ item.note || "-" }}</div>
        <div class="reservation-foot">
          <span>{{ formatTime(item.createdAt) }}</span>
          <div class="reservation-actions">
            <el-button v-if="canFinish(item)" size="small" type="success" round @click="finishItem(item.id)">完成</el-button>
            <el-button v-if="canConfirm(item)" size="small" type="primary" round @click="openConfirm(item)">确认</el-button>
            <el-button v-if="canCancel(item)" size="small" type="danger" plain round @click="cancelItem(item.id)">取消</el-button>
            <el-button v-if="item.conversationId" size="small" round @click="goConversation(item.conversationId)">去聊天</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="activeReservationList.length === 0" description="暂无进行中预约" />

    <h3 class="sub-title">历史预约</h3>
    <div class="reservation-grid">
      <div v-for="item in historyReservationList" :key="item.id" class="reservation-card history">
        <div class="reservation-head">
          <div class="skill-name">{{ item.skillTitle }}</div>
          <span class="status-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
        </div>
        <div class="reservation-line">
          <span>买家：{{ item.buyerName }}</span>
          <span>卖家：{{ item.sellerName }}</span>
        </div>
        <div class="reservation-line">地址：{{ item.address || "-" }}</div>
        <div class="reservation-line">电话：{{ item.phone || "-" }}</div>
        <div class="reservation-line">备注：{{ item.note || "-" }}</div>
        <div class="reservation-line" v-if="item.rating">评分：{{ item.rating }} 星</div>
        <div class="reservation-line" v-if="item.ratingComment">评价：{{ item.ratingComment }}</div>
        <div class="reservation-foot">
          <span>{{ formatTime(item.updatedAt || item.createdAt) }}</span>
          <el-button v-if="item.conversationId" size="small" round @click="goConversation(item.conversationId)">查看聊天</el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="historyReservationList.length === 0" description="暂无历史预约" />

    <el-dialog v-model="confirmOpen" title="确认完成并评价" width="420px">
      <div class="confirm-form">
        <div class="confirm-label">请为本次服务评分（1-5星）</div>
        <el-rate v-model="confirmForm.rating" :max="5" />
        <el-input v-model.trim="confirmForm.comment" type="textarea" :rows="3" maxlength="120" show-word-limit placeholder="可选评价" />
      </div>
      <template #footer>
        <el-button @click="confirmOpen = false">取消</el-button>
        <el-button type="primary" :loading="confirmSubmitting" @click="submitConfirm">提交确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editOpen" title="编辑技能" width="520px">
      <div class="edit-form">
        <el-input v-model.trim="editForm.title" placeholder="技能标题" maxlength="50" show-word-limit />
        <el-input v-model.number="editForm.price" placeholder="价格（元）" type="number" />
        <el-input v-model.trim="editForm.category" placeholder="分类（如：家教/维修/设计）" maxlength="20" />
        <el-input
          v-model.trim="editForm.description"
          placeholder="技能描述"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
        />
        <div class="location-field">
          <div class="location-title">服务地址</div>
          <LocationPicker map-height="240px" :initial-value="editLocationInitial" @change="onEditLocationChange" />
          <div v-if="editForm.address && typeof editForm.lng === 'number' && typeof editForm.lat === 'number'" class="location-preview">
            {{ editForm.address }}（{{ editForm.lng.toFixed(6) }}, {{ editForm.lat.toFixed(6) }}）
          </div>
        </div>
        <div class="upload-row">
          <el-upload
            class="uploader"
            :show-file-list="false"
            :http-request="doUploadEdit"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <img v-if="editForm.image" :src="editForm.image" class="preview-image" />
            <el-button v-else type="primary" plain>上传图片</el-button>
          </el-upload>
          <div class="upload-tip">
            <div class="tip-title">建议上传清晰封面</div>
            <div class="tip-desc">支持 JPG/PNG，最大 5MB</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import type { Action, UploadProps, UploadRequestOptions } from "element-plus";
import { ElMessage, ElMessageBox } from "element-plus";
import { addSkill, deleteSkill, getMySkills, updateSkill, updateSkillStatus, type SkillItem } from "../api/skill";
import LocationPicker from "../components/LocationPicker.vue";
import {
  cancelReservation,
  confirmReservation,
  finishReservation,
  getMyReservations,
  type ReservationItem,
} from "../api/reservation";
import { apiBaseUrl } from "../api/http";
import { uploadImageApi } from "../api/upload";

const router = useRouter();
const fallbackImage = "https://via.placeholder.com/400x300?text=Skill";

const form = reactive({
  title: "",
  price: 0,
  image: "",
  category: "",
  description: "",
  lng: undefined as number | undefined,
  lat: undefined as number | undefined,
  address: "",
  adcode: "",
  cityName: "",
});

const editForm = reactive({
  id: "",
  title: "",
  price: 0,
  image: "",
  category: "",
  description: "",
  lng: undefined as number | undefined,
  lat: undefined as number | undefined,
  address: "",
  adcode: "",
  cityName: "",
});

const submitting = ref(false);
const editSubmitting = ref(false);
const editOpen = ref(false);
const editLocationInitial = ref<{
  lng?: number;
  lat?: number;
  address?: string;
  adcode?: string;
  cityName?: string;
} | null>(null);
const confirmOpen = ref(false);
const confirmSubmitting = ref(false);
const confirmTargetId = ref("");
const myList = ref<SkillItem[]>([]);
const activeTab = ref<"all" | "on" | "off">("all");
const reservationTab = ref<"seller" | "buyer">("seller");
const buyerReservations = ref<ReservationItem[]>([]);
const sellerReservations = ref<ReservationItem[]>([]);

const confirmForm = reactive({
  rating: 5,
  comment: "",
});

const filteredList = computed(() => {
  if (activeTab.value === "all") return myList.value;
  if (activeTab.value === "on") return myList.value.filter((x) => x.status === 1);
  return myList.value.filter((x) => x.status !== 1);
});

const reservationList = computed(() => {
  return reservationTab.value === "seller" ? sellerReservations.value : buyerReservations.value;
});

const activeReservationList = computed(() => {
  return reservationList.value.filter((x) => !isHistoryStatus(x.status));
});

const historyReservationList = computed(() => {
  return reservationList.value.filter((x) => isHistoryStatus(x.status));
});

const beforeUpload: UploadProps["beforeUpload"] = (file) => {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("仅支持图片文件");
    return false;
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error("图片大小不能超过 5MB");
    return false;
  }
  return true;
};

function normalizeImageUrl(url: string) {
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  if (url.startsWith("/")) return `${apiBaseUrl}${url}`;
  return `${apiBaseUrl}/${url}`;
}

async function handleUpload(options: UploadRequestOptions, target: { image: string }) {
  const file = options.file as File;
  const res = await uploadImageApi(file);
  const url = res.data?.data || res.data?.url || res.data;
  if (typeof url === "string") {
    target.image = normalizeImageUrl(url);
  } else {
    target.image = "";
  }
  if (!target.image) throw new Error("上传返回图片地址为空");
  options.onSuccess(res.data);
}

async function doUpload(options: UploadRequestOptions) {
  try {
    await handleUpload(options, form);
    ElMessage.success("图片上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.response?.data?.message || error?.message || "图片上传失败");
  }
}

async function doUploadEdit(options: UploadRequestOptions) {
  try {
    await handleUpload(options, editForm);
    ElMessage.success("图片上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.response?.data?.message || error?.message || "图片上传失败");
  }
}

async function submit() {
  if (submitting.value) return;
  if (!form.title || !form.price || !form.image || !form.category) {
    ElMessage.warning("请填写标题、价格、分类并上传图片");
    return;
  }
  if (typeof form.lng !== "number" || typeof form.lat !== "number" || !form.address) {
    ElMessage.warning("请先选择服务地址");
    return;
  }
  try {
    submitting.value = true;
    await addSkill(form);
    ElMessage.success("发布成功");
    resetForm();
    await loadMine();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "发布失败");
  } finally {
    submitting.value = false;
  }
}

function resetForm() {
  form.title = "";
  form.price = 0;
  form.image = "";
  form.category = "";
  form.description = "";
  form.lng = undefined;
  form.lat = undefined;
  form.address = "";
  form.adcode = "";
  form.cityName = "";
}

function onFormLocationChange(payload: { lng: number; lat: number; address: string; adcode: string; cityName: string }) {
  form.lng = payload.lng;
  form.lat = payload.lat;
  form.address = payload.address;
  form.adcode = payload.adcode;
  form.cityName = payload.cityName;
}

function onEditLocationChange(payload: { lng: number; lat: number; address: string; adcode: string; cityName: string }) {
  editForm.lng = payload.lng;
  editForm.lat = payload.lat;
  editForm.address = payload.address;
  editForm.adcode = payload.adcode;
  editForm.cityName = payload.cityName;
}

function goHome() {
  router.push("/home");
}

function openEdit(item: SkillItem) {
  editForm.id = String(item.id);
  editForm.title = item.title;
  editForm.price = Number(item.price) || 0;
  editForm.image = item.image;
  editForm.category = item.category || "";
  editForm.description = item.description || "";
  editForm.lng = typeof item.lng === "number" ? item.lng : undefined;
  editForm.lat = typeof item.lat === "number" ? item.lat : undefined;
  editForm.address = item.address || "";
  editForm.adcode = item.adcode || "";
  editForm.cityName = item.cityName || "";
  editLocationInitial.value = {
    lng: editForm.lng,
    lat: editForm.lat,
    address: editForm.address,
    adcode: editForm.adcode,
    cityName: editForm.cityName,
  };
  editOpen.value = true;
}

async function saveEdit() {
  if (editSubmitting.value) return;
  if (!editForm.title || !editForm.price || !editForm.image || !editForm.category) {
    ElMessage.warning("请填写标题、价格、分类并上传图片");
    return;
  }
  try {
    editSubmitting.value = true;
    await updateSkill(editForm.id, editForm);
    const idx = myList.value.findIndex((x) => x.id === editForm.id);
    if (idx >= 0) {
      const current = myList.value[idx]!;
      myList.value[idx] = {
        ...current,
        id: current.id,
        title: editForm.title,
        price: editForm.price,
        image: editForm.image,
        category: editForm.category,
        description: editForm.description,
        lng: editForm.lng,
        lat: editForm.lat,
        address: editForm.address,
        adcode: editForm.adcode,
        cityName: editForm.cityName,
      };
    }
    ElMessage.success("保存成功");
    editOpen.value = false;
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "保存失败");
  } finally {
    editSubmitting.value = false;
  }
}

async function loadMine() {
  try {
    myList.value = await getMySkills();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "加载我的技能失败");
  }
}

async function loadReservations() {
  try {
    const [seller, buyer] = await Promise.all([getMyReservations("seller"), getMyReservations("buyer")]);
    sellerReservations.value = seller;
    buyerReservations.value = buyer;
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "加载预约记录失败");
  }
}

async function toggleStatus(item: SkillItem) {
  const nextStatus = item.status === 1 ? 0 : 1;
  try {
    await updateSkillStatus(item.id, nextStatus);
    item.status = nextStatus;
    ElMessage.success(nextStatus === 1 ? "已上架" : "已下架");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "更新状态失败");
  }
}

async function removeItem(id: SkillItem["id"]) {
  try {
    await ElMessageBox.confirm("确认删除该技能？", "提示", { type: "warning" });
    await deleteSkill(id);
    myList.value = myList.value.filter((x) => x.id !== id);
    ElMessage.success("删除成功");
  } catch (error: any) {
    if ((error as Action) === "cancel" || (error as Action) === "close") return;
    ElMessage.error(error?.response?.data?.message || error?.message || "删除失败");
  }
}

function statusText(status?: string) {
  const value = String(status || "").toUpperCase();
  if (value === "AWAIT_BUYER_CONFIRM") return "待买家确认";
  if (value === "COMPLETED") return "已完成";
  if (value === "CANCELED") return "已取消";
  return "待服务";
}

function statusClass(status?: string) {
  const value = String(status || "").toUpperCase();
  if (value === "AWAIT_BUYER_CONFIRM") return "pending";
  if (value === "COMPLETED") return "ok";
  if (value === "CANCELED") return "cancel";
  return "pending";
}

function isHistoryStatus(status?: string) {
  const value = String(status || "").toUpperCase();
  return value === "COMPLETED" || value === "CANCELED";
}

function canFinish(item: ReservationItem) {
  return reservationTab.value === "seller" && String(item.status || "").toUpperCase() === "PENDING";
}

function canConfirm(item: ReservationItem) {
  return reservationTab.value === "buyer" && String(item.status || "").toUpperCase() === "AWAIT_BUYER_CONFIRM";
}

function canCancel(item: ReservationItem) {
  const value = String(item.status || "").toUpperCase();
  return value !== "COMPLETED" && value !== "CANCELED";
}

async function cancelItem(id: string) {
  try {
    await ElMessageBox.confirm("确认取消该预约？", "提示", { type: "warning" });
    await cancelReservation(id);
    ElMessage.success("已取消预约");
    await loadReservations();
  } catch (error: any) {
    if ((error as Action) === "cancel" || (error as Action) === "close") return;
    ElMessage.error(error?.response?.data?.message || error?.message || "取消预约失败");
  }
}

async function finishItem(id: string) {
  try {
    await finishReservation(id);
    ElMessage.success("已标记完成，已通知买家确认");
    await loadReservations();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "操作失败");
  }
}

function openConfirm(item: ReservationItem) {
  confirmTargetId.value = item.id;
  confirmForm.rating = 5;
  confirmForm.comment = "";
  confirmOpen.value = true;
}

async function submitConfirm() {
  if (!confirmTargetId.value) return;
  if (!confirmForm.rating || confirmForm.rating < 1 || confirmForm.rating > 5) {
    ElMessage.warning("请先选择1-5星评分");
    return;
  }
  try {
    confirmSubmitting.value = true;
    await confirmReservation(confirmTargetId.value, {
      rating: confirmForm.rating,
      comment: confirmForm.comment || undefined,
    });
    confirmOpen.value = false;
    ElMessage.success("预约已确认完成");
    await loadReservations();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "确认失败");
  } finally {
    confirmSubmitting.value = false;
  }
}

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(
    date.getHours()
  ).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function goConversation(conversationId?: string) {
  if (!conversationId) return;
  router.push({ path: "/messages", query: { cid: conversationId } });
}

onMounted(async () => {
  await loadMine();
  await loadReservations();
});
</script>

<style scoped>
.publish-page {
  --surface: rgba(216, 242, 250, 0.5);
  --surface-strong: rgba(255, 236, 212, 0.5);
  --surface-soft: rgba(218, 246, 236, 0.46);
  --line: rgba(116, 169, 196, 0.42);
  --line-warm: rgba(255, 181, 116, 0.34);
  --text-main: #17324e;
  --text-muted: #607486;
  position: relative;
  min-height: 100vh;
  padding: 34px 48px 56px;
  max-width: none;
  margin: 0;
  overflow-x: hidden;
  background:
    linear-gradient(135deg, rgba(228, 246, 253, 0.28), rgba(255, 246, 235, 0.38) 48%, rgba(231, 248, 239, 0.42)),
    url("../assets/workspace-bg.png") center / cover fixed no-repeat;
}

.publish-page::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.3), rgba(216, 241, 249, 0.1) 42%, rgba(255, 249, 238, 0.26)),
    radial-gradient(circle at 78% 8%, rgba(255, 174, 104, 0.26), transparent 28%),
    radial-gradient(circle at 8% 78%, rgba(39, 180, 132, 0.16), transparent 24%);
  animation: publish-glow 11s ease-in-out infinite alternate;
}

.publish-page > * {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin-left: auto;
  margin-right: auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 18px 22px;
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(246, 252, 255, 0.76), rgba(255, 248, 238, 0.68)),
    linear-gradient(180deg, var(--surface), var(--surface-soft));
  border: 1px solid var(--line);
  backdrop-filter: blur(22px) saturate(1.16);
  box-shadow:
    0 22px 62px rgba(43, 87, 126, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.page-header h2,
.manage-title {
  color: var(--text-main);
  letter-spacing: 0;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
}

.page-header h2::after {
  content: "";
  display: block;
  width: 72px;
  height: 4px;
  margin-top: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1677ff, #35b889, #ffb26b);
}

.form-card {
  background:
    radial-gradient(circle at 12% 0%, rgba(111, 203, 226, 0.2), transparent 34%),
    radial-gradient(circle at 92% 16%, rgba(255, 176, 103, 0.18), transparent 30%),
    linear-gradient(135deg, rgba(219, 243, 250, 0.58), rgba(255, 239, 218, 0.44) 54%, rgba(218, 246, 236, 0.52)),
    var(--surface);
  padding: 24px;
  border-radius: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow:
    0 28px 80px rgba(43, 87, 126, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(112, 169, 198, 0.46);
  backdrop-filter: blur(26px) saturate(1.24);
  animation: rise-in 0.45s ease both;
  position: relative;
  overflow: hidden;
}

.form-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.2), transparent 34%, rgba(255, 182, 118, 0.16) 70%, transparent),
    radial-gradient(circle at 92% 0%, rgba(34, 167, 216, 0.22), transparent 22%),
    repeating-linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 12px);
  opacity: 0.9;
}

.form-card > * {
  position: relative;
  z-index: 1;
}

.back-btn {
  border-radius: 999px;
  padding: 0 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.back-btn:hover {
  transform: translateX(-3px);
  box-shadow: 0 10px 22px rgba(43, 87, 126, 0.14);
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.location-field {
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 14px;
  background:
    radial-gradient(circle at 8% 10%, rgba(57, 173, 215, 0.14), transparent 34%),
    linear-gradient(135deg, rgba(224, 246, 252, 0.58), rgba(255, 240, 219, 0.4)),
    rgba(220, 246, 236, 0.3);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  backdrop-filter: blur(16px);
}

.location-field:hover {
  transform: translateY(-2px);
  border-color: rgba(22, 119, 255, 0.28);
  box-shadow: 0 16px 34px rgba(43, 87, 126, 0.12);
}

.location-title {
  font-size: 13px;
  font-weight: 700;
  color: #2a3b47;
  margin-bottom: 8px;
}

.location-preview {
  margin-top: 8px;
  color: #5d7085;
  font-size: 12px;
}

.upload-tip {
  color: #6c7a89;
  font-size: 12px;
}
.tip-title {
  font-weight: 600;
  color: #2a3b47;
  margin-bottom: 4px;
}
.tip-desc {
  color: #8b98a5;
}

.uploader {
  width: 180px;
}

.preview-image {
  width: 180px;
  height: 130px;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid rgba(210, 228, 240, 0.95);
  box-shadow: 0 12px 28px rgba(43, 87, 126, 0.14);
}

.submit-btn {
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1677ff, #22a7d8);
  border: none;
  box-shadow: 0 14px 28px rgba(22, 119, 255, 0.22);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(22, 119, 255, 0.3);
}

.submit-btn:active {
  transform: translateY(0) scale(0.99);
}

.manage-title {
  margin-top: 32px;
  padding-left: 12px;
  border-left: 5px solid #22a7d8;
}

.sub-title {
  margin: 14px 0 0;
  font-size: 17px;
  color: #27394d;
}

.filter-bar {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(246, 252, 255, 0.72), rgba(255, 249, 239, 0.6));
  border: 1px solid var(--line);
  backdrop-filter: blur(18px) saturate(1.12);
  width: fit-content;
}

.refresh-btn {
  margin-left: 4px;
}

.manage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  margin-top: 16px;
}

.manage-card {
  background:
    linear-gradient(180deg, rgba(248, 253, 255, 0.76), rgba(255, 249, 240, 0.62)),
    var(--surface);
  border: 1px solid var(--line);
  border-radius: 20px;
  overflow: hidden;
  box-shadow:
    0 18px 46px rgba(43, 87, 126, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.58);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  backdrop-filter: blur(16px);
}

.manage-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 64px rgba(43, 87, 126, 0.2);
}

.manage-card:active,
.reservation-card:active {
  transform: translateY(-1px) scale(0.995);
}

.manage-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  background: #eef5fb;
}

.manage-card .info {
  padding: 10px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(235, 249, 244, 0.24));
}

.title {
  margin: 0 0 6px;
}

.price {
  color: #ff5500;
  font-weight: 600;
}

.meta {
  margin: 8px 0 0;
  color: #888;
  font-size: 12px;
}

.actions {
  display: flex;
  justify-content: space-around;
  padding: 10px;
  gap: 6px;
  border-top: 1px solid rgba(226, 236, 245, 0.9);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reservation-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.reservation-card {
  background:
    linear-gradient(135deg, rgba(247, 253, 255, 0.76), rgba(239, 250, 245, 0.62) 52%, rgba(255, 248, 239, 0.66)),
    var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 14px;
  box-shadow:
    0 16px 42px rgba(43, 87, 126, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(18px) saturate(1.16);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.reservation-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 56px rgba(43, 87, 126, 0.18);
}

.reservation-card.history {
  opacity: 0.92;
}

.reservation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.skill-name {
  font-size: 15px;
  font-weight: 700;
  color: #22384e;
}

.status-tag {
  border: 1px solid;
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 12px;
  white-space: nowrap;
}

.status-tag.pending {
  color: #1661d6;
  border-color: #8eb9ff;
  background: #eef4ff;
}

.status-tag.ok {
  color: #1f8f46;
  border-color: #66c18a;
  background: #edf9f0;
}

.status-tag.cancel {
  color: #c73636;
  border-color: #f0a2a2;
  background: #fff1f1;
}

.reservation-line {
  margin-top: 6px;
  color: #4d6278;
  font-size: 13px;
  line-height: 1.4;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.reservation-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7f8fa2;
  font-size: 12px;
}

.reservation-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.confirm-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.confirm-label {
  color: #42596f;
  font-size: 14px;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 14px !important;
  background:
    linear-gradient(135deg, rgba(226, 246, 252, 0.72), rgba(255, 242, 224, 0.54)),
    rgba(224, 246, 236, 0.34) !important;
  box-shadow:
    0 0 0 1px rgba(36, 114, 196, 0.14) inset,
    inset 0 1px 0 rgba(255, 255, 255, 0.5) !important;
  backdrop-filter: blur(14px);
}

:deep(.el-input-group__append) {
  background: rgba(232, 246, 247, 0.58) !important;
  border-color: rgba(126, 177, 201, 0.38) !important;
}

:deep(.el-input__count) {
  background: transparent !important;
  color: #73889a !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.38) inset, 0 0 0 3px rgba(22, 119, 255, 0.1) !important;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes publish-glow {
  from {
    opacity: 0.62;
    transform: translate3d(-10px, 0, 0);
  }
  to {
    opacity: 0.96;
    transform: translate3d(10px, -8px, 0);
  }
}

@media (max-width: 760px) {
  .publish-page {
    padding: 20px 14px 36px;
  }

  .page-header {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }

  .form-card {
    padding: 18px;
  }

  .upload-row,
  .filter-bar,
  .reservation-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .manage-grid,
  .reservation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
