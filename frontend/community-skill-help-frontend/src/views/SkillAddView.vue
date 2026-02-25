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
// 组件说明：发布与管理页模块。作用：发布技能并管理技能与预约状态。
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
  padding: 28px 48px 40px;
  max-width: 1100px;
  margin: auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.form-card {
  background: #fff;
  padding: 22px 24px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.back-btn {
  border-radius: 999px;
  padding: 0 16px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.location-field {
  border: 1px solid #e3ebf5;
  border-radius: 14px;
  padding: 12px;
  background: #f9fbff;
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
  border-radius: 12px;
  border: 1px solid #eee;
}

.submit-btn {
  height: 42px;
  border-radius: 12px;
}

.manage-title {
  margin-top: 28px;
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
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.manage-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.manage-card .info {
  padding: 10px;
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
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 6px 18px rgba(25, 50, 90, 0.08);
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
</style>

