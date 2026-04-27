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
  --chat-surface: rgba(246, 252, 255, 0.66);
  --chat-warm: rgba(255, 248, 239, 0.58);
  --chat-mint: rgba(237, 250, 244, 0.56);
  --chat-line: rgba(150, 190, 211, 0.34);
  --chat-shadow: rgba(43, 87, 126, 0.16);
  min-height: 100vh;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(228, 246, 253, 0.28), rgba(255, 246, 235, 0.34) 48%, rgba(231, 248, 239, 0.42)),
    url("../assets/workspace-bg.png") center / cover no-repeat fixed;
  position: relative;
}

.chat-page::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 18%, rgba(34, 167, 216, 0.18), transparent 24%),
    radial-gradient(circle at 84% 86%, rgba(255, 178, 107, 0.18), transparent 26%);
  animation: chat-ambient 10s ease-in-out infinite alternate;
}

.chat-shell {
  position: relative;
  z-index: 1;
  width: min(1260px, 98vw);
  height: calc(100vh - 36px);
  margin: 0 auto;
  border-radius: 26px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(247, 253, 255, 0.72), rgba(255, 249, 240, 0.54) 56%, rgba(237, 250, 244, 0.58)),
    var(--chat-surface);
  border: 1px solid var(--chat-line);
  box-shadow:
    0 30px 90px rgba(43, 87, 126, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(24px) saturate(1.16);
  display: grid;
  grid-template-columns: 290px 1fr;
  animation: chat-rise 0.45s ease both;
}

.sidebar {
  border-right: 1px solid var(--chat-line);
  background:
    linear-gradient(180deg, rgba(238, 249, 255, 0.66) 0%, rgba(255, 248, 239, 0.38) 100%),
    rgba(255, 255, 255, 0.26);
  display: flex;
  flex-direction: column;
}

.sidebar-top {
  padding: 14px;
  border-bottom: 1px solid var(--chat-line);
}

.sidebar-top :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 12px;
}

.contact-list {
  overflow: auto;
  padding: 10px 8px 14px;
}

.contact-item {
  border-radius: 16px;
  padding: 11px;
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.contact-item:hover {
  background: rgba(237, 246, 255, 0.9);
  transform: translateX(3px);
}

.contact-item.active {
  background: linear-gradient(135deg, rgba(218, 239, 255, 0.72), rgba(233, 249, 239, 0.62), rgba(255, 246, 232, 0.42));
  box-shadow: 0 10px 24px rgba(43, 87, 126, 0.12);
  position: relative;
}

.contact-item.active::before {
  content: "";
  width: 4px;
  border-radius: 999px;
  align-self: stretch;
  background: linear-gradient(180deg, #1677ff, #35b889);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1677ff, #22a7d8);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.22);
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
  grid-template-rows: 68px 1fr 78px;
  min-width: 0;
}

.chat-header {
  border-bottom: 1px solid var(--chat-line);
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background:
    linear-gradient(90deg, rgba(247, 253, 255, 0.62), rgba(255, 249, 239, 0.46)),
    rgba(255, 255, 255, 0.28);
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
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.booking-btn:hover,
.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(43, 87, 126, 0.14);
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
  padding: 22px;
  background:
    linear-gradient(180deg, rgba(239, 249, 255, 0.42), rgba(255, 249, 240, 0.24)),
    radial-gradient(circle at 90% 10%, rgba(255, 200, 138, 0.2), transparent 24%),
    radial-gradient(circle at 8% 88%, rgba(53, 184, 137, 0.12), transparent 26%);
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
  padding: 12px 15px;
  border-radius: 18px 18px 18px 6px;
  font-size: 15px;
  line-height: 1.55;
  background:
    linear-gradient(135deg, rgba(250, 253, 255, 0.82), rgba(255, 249, 240, 0.54)),
    rgba(255, 255, 255, 0.46);
  color: #1e2c3a;
  border: 1px solid var(--chat-line);
  box-shadow: 0 12px 28px rgba(43, 87, 126, 0.1);
  backdrop-filter: blur(12px);
  animation: bubble-in 0.22s ease both;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(43, 87, 126, 0.14);
}

.bubble-row.own .bubble {
  border-radius: 18px 18px 6px 18px;
  background: linear-gradient(135deg, rgba(216, 236, 255, 0.82), rgba(225, 247, 239, 0.76));
  border-color: rgba(148, 204, 232, 0.42);
}

.bubble.booking {
  background: linear-gradient(135deg, rgba(255, 248, 235, 0.86), rgba(255, 253, 247, 0.72));
  border-color: rgba(255, 190, 115, 0.44);
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
  border-top: 1px solid var(--chat-line);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  background:
    linear-gradient(90deg, rgba(247, 253, 255, 0.62), rgba(255, 249, 239, 0.46)),
    rgba(255, 255, 255, 0.28);
}

.chat-input :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.chat-input :deep(.el-input__wrapper.is-focus) {
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.36) inset, 0 0 0 3px rgba(22, 119, 255, 0.1) !important;
}

.chat-input .el-button {
  height: 42px;
  padding: 0 18px;
  background: linear-gradient(135deg, #1677ff, #22a7d8);
  border: none;
  box-shadow: 0 12px 24px rgba(22, 119, 255, 0.22);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.chat-input .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(22, 119, 255, 0.3);
}

.chat-input .el-button:active {
  transform: translateY(0) scale(0.98);
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
  .chat-page {
    padding: 8px;
    background-attachment: scroll;
  }

  .chat-shell {
    grid-template-columns: 1fr;
    grid-template-rows: 220px 1fr;
    height: calc(100vh - 16px);
    border-radius: 20px;
  }

  .chat-main {
    grid-template-rows: 72px 1fr 76px;
  }

  .bubble {
    max-width: 84%;
  }
}

@keyframes chat-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes chat-ambient {
  from {
    opacity: 0.58;
    transform: translate3d(-8px, 0, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(8px, -8px, 0);
  }
}
</style>
