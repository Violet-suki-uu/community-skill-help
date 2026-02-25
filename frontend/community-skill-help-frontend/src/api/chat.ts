/**
 * 模块说明：聊天 API 模块。作用：封装会话、消息和预约下单相关接口。
 */
import request from "./http";

type ApiWrap<T> = {
  code?: number;
  message?: string;
  data?: T;
};

export interface ConversationItem {
  conversationId: string;
  skillId: string;
  skillTitle: string;
  buyerId: string;
  sellerId: string;
  peerUserId: string;
  peerName: string;
  peerPhone?: string;
  lastMessage?: string;
  lastMessageAt?: string;
}

export interface ChatMessageItem {
  id: string;
  conversationId: string;
  senderId: string;
  content?: string;
  messageType: "TEXT" | "BOOKING";
  bookingAddress?: string;
  bookingPhone?: string;
  note?: string;
  createdAt?: string;
}

function unwrapApi<T>(payload: ApiWrap<T> | T): T {
  if (payload && typeof payload === "object" && ("code" in payload || "data" in payload || "message" in payload)) {
    const wrap = payload as ApiWrap<T>;
    if (typeof wrap.code === "number" && wrap.code !== 0) {
      throw new Error(wrap.message || "Request failed");
    }
    return wrap.data as T;
  }
  return payload as T;
}

export function openConversation(skillId: string) {
  return request.post<ApiWrap<string> | string>("/api/chat/conversations/open", { skillId }).then((res) => unwrapApi(res.data));
}

export function getConversations() {
  return request
    .get<ApiWrap<ConversationItem[]> | ConversationItem[]>("/api/chat/conversations")
    .then((res) => unwrapApi<ConversationItem[]>(res.data) || []);
}

export function getMessages(conversationId: string) {
  return request
    .get<ApiWrap<ChatMessageItem[]> | ChatMessageItem[]>(`/api/chat/conversations/${conversationId}/messages`)
    .then((res) => unwrapApi<ChatMessageItem[]>(res.data) || []);
}

export function sendMessage(conversationId: string, content: string) {
  return request
    .post<ApiWrap<boolean> | boolean>(`/api/chat/conversations/${conversationId}/messages`, { content })
    .then((res) => unwrapApi<boolean>(res.data));
}

export function createBooking(conversationId: string, payload: { address: string; phone: string; note?: string }) {
  return request
    .post<ApiWrap<number | string> | number | string>(`/api/chat/conversations/${conversationId}/booking`, payload)
    .then((res) => unwrapApi<number | string>(res.data));
}

