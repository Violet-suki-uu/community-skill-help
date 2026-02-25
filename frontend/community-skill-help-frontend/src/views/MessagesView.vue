<template>
  <div class="chat-page">
    <div class="chat-shell">
      <aside class="sidebar">
        <div class="sidebar-top">
          <el-input v-model.trim="keyword" placeholder="搜索联系人/技能" clearable />
        </div>
        <div class="contact-list">
          <div
            v-for="item in filteredContacts"
            :key="item.conversationId"
            class="contact-item"
            :class="{ active: item.conversationId === activeConversationId }"
            @click="selectConversation(item.conversationId)"
          >
            <div class="avatar">{{ displayContactName(item).slice(0, 1) || "?" }}</div>
            <div class="contact-main">
              <div class="line1">
                <span class="name">{{ displayContactName(item) }}</span>
                <span class="time">{{ formatTime(item.lastMessageAt) }}</span>
              </div>
              <div class="line2">
                <span class="preview">{{ item.lastMessage || item.skillTitle }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="filteredContacts.length === 0" description="暂无会话" />
        </div>
      </aside>

      <main class="chat-main">
        <template v-if="activeContact">
          <header class="chat-header">
            <div class="header-left">
              <el-button class="booking-btn" size="small" round @click="onBookingAction">预约</el-button>
              <div class="title-wrap">
                <div class="title">{{ displayContactName(activeContact) }}</div>
                <div class="subtitle">{{ activeContact.skillTitle }}</div>
              </div>
            </div>
            <el-button class="home-btn" size="small" round @click="goHome">返回首页</el-button>
          </header>

          <section ref="messageBodyRef" class="message-body">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="bubble-row"
              :class="{ own: String(msg.senderId) === currentUserId }"
            >
              <div class="bubble" :class="{ booking: msg.messageType === 'BOOKING' }">
                <template v-if="msg.messageType === 'BOOKING'">
                  <div class="booking-title">预约服务</div>
                  <div class="booking-line">地址：{{ msg.bookingAddress }}</div>
                  <div class="booking-line">电话：{{ msg.bookingPhone }}</div>
                  <div v-if="msg.note" class="booking-line">备注：{{ msg.note }}</div>
                </template>
                <template v-else>
                  {{ msg.content }}
                </template>
              </div>
            </div>
          </section>

          <footer class="chat-input">
            <el-input v-model.trim="draft" placeholder="输入消息..." @keyup.enter="sendCurrentMessage" />
            <el-button type="primary" round :disabled="!draft" @click="sendCurrentMessage">发送</el-button>
          </footer>
        </template>

        <div v-else class="empty-main">
          <el-empty description="选择左侧联系人开始聊天" />
        </div>
      </main>
    </div>

    <el-dialog v-model="bookingOpen" title="预约服务" width="420px">
      <div class="booking-form">
        <el-input v-model.trim="bookingForm.address" placeholder="服务地址" />
        <el-input v-model.trim="bookingForm.phone" placeholder="联系电话" />
        <el-input v-model.trim="bookingForm.note" placeholder="备注（可选）" />
      </div>
      <template #footer>
        <el-button @click="bookingOpen = false">取消</el-button>
        <el-button type="primary" :loading="bookingSubmitting" @click="submitBooking">确认预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bookingInfoOpen" title="对方预约信息" width="420px">
      <div v-if="latestBookingInfo" class="booking-info">
        <div class="info-row"><span>地址：</span>{{ latestBookingInfo.bookingAddress }}</div>
        <div class="info-row"><span>电话：</span>{{ latestBookingInfo.bookingPhone }}</div>
        <div class="info-row"><span>备注：</span>{{ latestBookingInfo.note || "无" }}</div>
        <div class="info-row"><span>时间：</span>{{ formatTime(latestBookingInfo.createdAt) }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="bookingInfoOpen = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 组件说明：消息页模块。作用：展示会话列表并完成实时聊天操作。
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createBooking, getConversations, getMessages, sendMessage, type ChatMessageItem, type ConversationItem } from "../api/chat";
import { getMeApi } from "../api/user";

const route = useRoute();
const router = useRouter();

const contacts = ref<ConversationItem[]>([]);
const messages = ref<ChatMessageItem[]>([]);
const currentUserId = ref("");
const activeConversationId = ref("");
const keyword = ref("");
const draft = ref("");
const bookingOpen = ref(false);
const bookingSubmitting = ref(false);
const bookingInfoOpen = ref(false);
const latestBookingInfo = ref<ChatMessageItem | null>(null);
const messageBodyRef = ref<HTMLElement | null>(null);

const bookingForm = reactive({
  address: "",
  phone: "",
  note: "",
});

let timer: number | undefined;

const filteredContacts = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return contacts.value;
  return contacts.value.filter((x) => {
    return displayContactName(x).toLowerCase().includes(kw) || x.skillTitle.toLowerCase().includes(kw);
  });
});

const activeContact = computed(() => contacts.value.find((x) => x.conversationId === activeConversationId.value));
const isSellerSide = computed(() => {
  if (!activeContact.value) return false;
  return String(activeContact.value.sellerId) === currentUserId.value;
});

function displayContactName(item: ConversationItem) {
  const name = (item.peerName || "").trim();
  if (name) return name;
  if (item.peerPhone && item.peerPhone.trim()) return item.peerPhone.trim();
  return `用户${item.peerUserId}`;
}

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function goHome() {
  router.push("/home");
}

async function loadCurrentUser() {
  const res = await getMeApi();
  currentUserId.value = String(res?.data?.data?.id || "");
}

async function loadConversations(preserveSelection = true) {
  const data = await getConversations();
  contacts.value = data;
  const queryConversationId = route.query.cid ? String(route.query.cid) : "";

  if (queryConversationId && data.some((x) => x.conversationId === queryConversationId)) {
    activeConversationId.value = queryConversationId;
    return;
  }
  if (preserveSelection && activeConversationId.value && data.some((x) => x.conversationId === activeConversationId.value)) {
    return;
  }
  activeConversationId.value = data[0]?.conversationId || "";
}

async function loadMessages() {
  if (!activeConversationId.value) {
    messages.value = [];
    return;
  }
  messages.value = await getMessages(activeConversationId.value);
  await nextTick();
  if (messageBodyRef.value) {
    messageBodyRef.value.scrollTop = messageBodyRef.value.scrollHeight;
  }
}

async function refreshAll() {
  await loadConversations(true);
  await loadMessages();
}

async function selectConversation(conversationId: string) {
  activeConversationId.value = conversationId;
  await loadMessages();
  router.replace({ path: "/messages", query: { cid: conversationId } });
}

async function sendCurrentMessage() {
  if (!activeConversationId.value || !draft.value) return;
  try {
    await sendMessage(activeConversationId.value, draft.value);
    draft.value = "";
    await refreshAll();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "发送失败");
  }
}

function findLatestBookingFromPeer() {
  return (
    [...messages.value]
      .reverse()
      .find((x) => x.messageType === "BOOKING" && String(x.senderId) !== currentUserId.value) || null
  );
}

function onBookingAction() {
  if (!activeConversationId.value) return;
  if (isSellerSide.value) {
    const latest = findLatestBookingFromPeer();
    if (!latest) {
      ElMessage.info("对方暂时没有预约");
      return;
    }
    latestBookingInfo.value = latest;
    bookingInfoOpen.value = true;
    return;
  }
  bookingOpen.value = true;
}

async function submitBooking() {
  if (!activeConversationId.value) return;
  if (!bookingForm.address || !bookingForm.phone) {
    ElMessage.warning("请填写服务地址和联系电话");
    return;
  }
  try {
    bookingSubmitting.value = true;
    await createBooking(activeConversationId.value, {
      address: bookingForm.address,
      phone: bookingForm.phone,
      note: bookingForm.note || undefined,
    });
    bookingForm.address = "";
    bookingForm.phone = "";
    bookingForm.note = "";
    bookingOpen.value = false;
    await refreshAll();
    ElMessage.success("预约信息已发送");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "预约失败");
  } finally {
    bookingSubmitting.value = false;
  }
}

watch(
  () => route.query.cid,
  async (cid) => {
    const id = cid ? String(cid) : "";
    if (!id) return;
    activeConversationId.value = id;
    await loadMessages();
  }
);

onMounted(async () => {
  try {
    await loadCurrentUser();
    await refreshAll();
  } catch {
    router.push("/home");
    return;
  }
  timer = window.setInterval(refreshAll, 5000);
});

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer);
});
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
  padding: 10px;
  background: radial-gradient(circle at 0 0, #f3f8ff 0, #f7f9fc 55%, #eef2f8 100%);
}

.chat-shell {
  width: min(1260px, 98vw);
  height: calc(100vh - 20px);
  margin: 0 auto;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 12px 30px rgba(20, 40, 70, 0.12);
  display: grid;
  grid-template-columns: 290px 1fr;
}

.sidebar {
  border-right: 1px solid #eef1f5;
  background: linear-gradient(180deg, #fcfdff 0%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
}

.sidebar-top {
  padding: 10px;
}

.sidebar-top :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 12px;
}

.contact-list {
  overflow: auto;
  padding: 4px 6px 10px;
}

.contact-item {
  border-radius: 12px;
  padding: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
  transition: background 0.2s ease;
}

.contact-item:hover {
  background: #edf4ff;
}

.contact-item.active {
  background: #dfeeff;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #2f79ff;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 700;
}

.contact-main {
  min-width: 0;
  flex: 1;
}

.line1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #25313f;
}

.time {
  font-size: 12px;
  color: #92a0b3;
}

.line2 {
  margin-top: 4px;
}

.preview {
  display: block;
  font-size: 13px;
  color: #7f8da1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-main {
  display: grid;
  grid-template-rows: 62px 1fr 72px;
}

.chat-header {
  border-bottom: 1px solid #eef1f5;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-btn {
  min-width: 112px;
  height: 38px;
  font-size: 14px;
}

.booking-btn {
  min-width: 84px;
  height: 38px;
  font-size: 14px;
}

.title-wrap .title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2f40;
}

.title-wrap .subtitle {
  font-size: 13px;
  color: #8c9ab0;
  margin-top: 2px;
}

.message-body {
  overflow-y: auto;
  padding: 16px 18px;
  background: #f6f9ff;
}

.bubble-row {
  display: flex;
  margin-bottom: 12px;
}

.bubble-row.own {
  justify-content: flex-end;
}

.bubble {
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 15px;
  line-height: 1.55;
  background: #ffffff;
  color: #1e2c3a;
  border: 1px solid #e5edf8;
}

.bubble-row.own .bubble {
  background: #d8ecff;
  border-color: #c7e3ff;
}

.bubble.booking {
  background: #fff8eb;
  border-color: #ffe0ad;
}

.booking-title {
  font-weight: 700;
  margin-bottom: 4px;
  color: #8c5500;
}

.booking-line {
  color: #4d3b1f;
}

.chat-input {
  border-top: 1px solid #eef1f5;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-input :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px;
}

.chat-input .el-button {
  height: 40px;
  padding: 0 16px;
}

.empty-main {
  display: grid;
  place-items: center;
}

.booking-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.booking-info {
  display: grid;
  gap: 10px;
}

.info-row {
  font-size: 14px;
  color: #2b3b4e;
  line-height: 1.5;
}

.info-row span {
  color: #7d8b9b;
}

@media (max-width: 900px) {
  .chat-shell {
    grid-template-columns: 1fr;
    grid-template-rows: 220px 1fr;
    height: calc(100vh - 12px);
  }
}
</style>

