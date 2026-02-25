/**
 * 模块说明：预约 API 模块。作用：封装预约查询、取消、完成和确认接口。
 */
import request from "./http";

type ApiWrap<T> = {
  code?: number;
  message?: string;
  data?: T;
};

export interface ReservationItem {
  id: string;
  skillId: string;
  skillTitle: string;
  buyerId: string;
  buyerName: string;
  sellerId: string;
  sellerName: string;
  status: string;
  address?: string;
  phone?: string;
  note?: string;
  rating?: number;
  ratingComment?: string;
  ratedAt?: string;
  conversationId?: string;
  createdAt?: string;
  updatedAt?: string;
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

function normalizeReservation(item: any): ReservationItem {
  return {
    id: String(item?.id ?? ""),
    skillId: String(item?.skillId ?? ""),
    skillTitle: String(item?.skillTitle ?? ""),
    buyerId: String(item?.buyerId ?? ""),
    buyerName: String(item?.buyerName ?? "用户"),
    sellerId: String(item?.sellerId ?? ""),
    sellerName: String(item?.sellerName ?? "用户"),
    status: String(item?.status ?? "PENDING"),
    address: item?.address || "",
    phone: item?.phone || "",
    note: item?.note || "",
    rating: item?.rating == null ? undefined : Number(item.rating),
    ratingComment: item?.ratingComment || "",
    ratedAt: item?.ratedAt || "",
    conversationId: item?.conversationId == null ? "" : String(item.conversationId),
    createdAt: item?.createdAt || "",
    updatedAt: item?.updatedAt || "",
  };
}

export function getSkillReservations(skillId: string | number) {
  return request
    .get<ApiWrap<any[]> | any[]>(`/api/reservations/skill/${skillId}`)
    .then((res) => (unwrapApi<any[]>(res.data) || []).map(normalizeReservation));
}

export function getMyReservations(role: "buyer" | "seller" | "all" = "all") {
  return request
    .get<ApiWrap<any[]> | any[]>("/api/reservations/mine", { params: { role } })
    .then((res) => (unwrapApi<any[]>(res.data) || []).map(normalizeReservation));
}

export function cancelReservation(reservationId: string | number) {
  return request
    .patch<ApiWrap<boolean> | boolean>(`/api/reservations/${reservationId}/cancel`)
    .then((res) => unwrapApi<boolean>(res.data));
}

export function finishReservation(reservationId: string | number) {
  return request
    .patch<ApiWrap<boolean> | boolean>(`/api/reservations/${reservationId}/finish`)
    .then((res) => unwrapApi<boolean>(res.data));
}

export function confirmReservation(reservationId: string | number, payload: { rating: number; comment?: string }) {
  return request
    .patch<ApiWrap<boolean> | boolean>(`/api/reservations/${reservationId}/confirm`, payload)
    .then((res) => unwrapApi<boolean>(res.data));
}

