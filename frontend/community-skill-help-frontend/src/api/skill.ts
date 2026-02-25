/**
 * 模块说明：技能 API 模块。作用：封装技能列表、详情、发布、编辑等接口调用。
 */
import request, { apiBaseUrl } from "./http";

type ApiWrap<T> = {
  code?: number;
  message?: string;
  data?: T;
};

export type SkillId = string | number;

type Paged<T> = {
  records?: T[];
  list?: T[];
  items?: T[];
};

type BackendSkillItem = {
  id: SkillId;
  title: string;
  price: number | string;
  image?: string;
  imageUrl?: string;
  status?: number;
  category?: string;
  description?: string;
  userId?: SkillId;
  sellerNickname?: string;
  sellerCreditScore?: number | string;
  lng?: number | string;
  lat?: number | string;
  address?: string;
  adcode?: string;
  cityName?: string;
  city?: string;
  distanceKm?: number | string;
  distance?: number | string;
  distanceMeter?: number | string;
  distanceMeters?: number | string;
  viewCount?: number | string;
};

export interface SkillItem {
  id: SkillId;
  title: string;
  price: number | string;
  image: string;
  status?: number;
  category?: string;
  description?: string;
  userId?: SkillId;
  sellerNickname?: string;
  sellerCreditScore?: number;
  lng?: number;
  lat?: number;
  address?: string;
  adcode?: string;
  cityName?: string;
  distanceKm?: number;
  viewCount?: number;
}

export interface SkillDetail extends SkillItem {
  sellerNickname: string;
  sellerCreditScore: number;
}

export interface RecommendPage {
  items: SkillItem[];
  nextCursor?: string;
}

function isApiWrap<T>(payload: unknown): payload is ApiWrap<T> {
  if (payload === null || typeof payload !== "object") return false;
  return "code" in payload || "message" in payload || "data" in payload;
}

function ensureApiSuccess<T>(payload: ApiWrap<T>): T | undefined {
  if (typeof payload.code === "number" && payload.code !== 0) {
    throw new Error(payload.message || "Request failed");
  }
  return payload.data;
}

function toAbsoluteUrl(url?: string): string {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  if (url.startsWith("/")) return `${apiBaseUrl}${url}`;
  return `${apiBaseUrl}/${url}`;
}

function toFiniteNumber(value: unknown): number | undefined {
  const n = Number(value);
  return Number.isFinite(n) ? n : undefined;
}

function normalizeDistanceKm(item: BackendSkillItem): number | undefined {
  const directKm = toFiniteNumber(item.distanceKm);
  if (directKm !== undefined) return directKm;

  const maybeKm = toFiniteNumber(item.distance);
  if (maybeKm !== undefined && maybeKm <= 100) return maybeKm;

  const meter = toFiniteNumber(item.distanceMeters ?? item.distanceMeter);
  if (meter !== undefined) return meter / 1000;

  if (maybeKm !== undefined) return maybeKm / 1000;
  return undefined;
}

function normalizeSkill(item: BackendSkillItem): SkillItem {
  const image = item.image || item.imageUrl || "";
  const lng = toFiniteNumber(item.lng);
  const lat = toFiniteNumber(item.lat);
  const distanceKm = normalizeDistanceKm(item);
  return {
    ...item,
    id: String(item.id),
    userId: item.userId == null ? undefined : String(item.userId),
    image: toAbsoluteUrl(image),
    sellerNickname: item.sellerNickname || "User",
    sellerCreditScore: Number(item.sellerCreditScore ?? 0),
    lng,
    lat,
    address: item.address || "",
    adcode: String(item.adcode || ""),
    cityName: item.cityName || item.city || "",
    distanceKm,
    viewCount: Number(item.viewCount ?? 0),
  };
}

function unwrapPaged<T>(payload: Paged<T> | undefined | null): T[] {
  if (!payload) return [];
  if (Array.isArray(payload.records)) return payload.records;
  if (Array.isArray(payload.list)) return payload.list;
  if (Array.isArray(payload.items)) return payload.items;
  return [];
}

function unwrapList<T>(payload: ApiWrap<T[] | Paged<T>> | T[] | Paged<T> | undefined | null): T[] {
  if (Array.isArray(payload)) return payload;
  if (!payload) return [];

  if (isApiWrap<T[] | Paged<T>>(payload)) {
    const data = ensureApiSuccess<T[] | Paged<T>>(payload);
    if (Array.isArray(data)) return data;
    return unwrapPaged<T>(data as Paged<T>);
  }

  return unwrapPaged<T>(payload as Paged<T>);
}

function unwrapOne<T>(payload: ApiWrap<T> | T): T {
  if (isApiWrap<T>(payload)) {
    const data = ensureApiSuccess<T>(payload);
    if (data === undefined) {
      throw new Error(payload.message || "Empty response");
    }
    return data as T;
  }
  return payload as T;
}

export function getSkillList(params?: { keyword?: string; category?: string }) {
  return request
    .get<ApiWrap<BackendSkillItem[] | Paged<BackendSkillItem>> | BackendSkillItem[]>("/api/skills", { params })
    .then((res) => unwrapList<BackendSkillItem>(res.data).map(normalizeSkill));
}

export function getMySkills() {
  return request
    .get<ApiWrap<BackendSkillItem[] | Paged<BackendSkillItem>> | BackendSkillItem[]>("/api/skills/mine")
    .then((res) => unwrapList<BackendSkillItem>(res.data).map(normalizeSkill));
}

export function addSkill(data: Partial<SkillItem>) {
  const payload = {
    title: data.title,
    description: data.description,
    category: data.category,
    price: data.price,
    imageUrl: data.image,
    lng: data.lng,
    lat: data.lat,
    address: data.address,
    adcode: data.adcode,
    cityName: data.cityName,
  };

  return request.post<ApiWrap<SkillItem> | SkillItem>("/api/skills", payload).then((res) => unwrapOne(res.data));
}

export async function updateSkillStatus(id: SkillId, status: number) {
  return request
    .patch<ApiWrap<boolean> | boolean>(`/api/skills/${id}/status`, { status })
    .then((res) => unwrapOne<boolean>(res.data));
}

export function deleteSkill(id: SkillId) {
  return request.delete<ApiWrap<boolean> | boolean>(`/api/skills/${id}`).then((res) => unwrapOne<boolean>(res.data));
}

export function updateSkill(id: SkillId, data: Partial<SkillItem>) {
  const payload = {
    title: data.title,
    description: data.description,
    category: data.category,
    price: data.price,
    imageUrl: data.image,
    lng: data.lng,
    lat: data.lat,
    address: data.address,
    adcode: data.adcode,
    cityName: data.cityName,
  };

  return request.put<ApiWrap<SkillItem> | SkillItem>(`/api/skills/${id}`, payload).then((res) => unwrapOne(res.data));
}

export function getNearbySkills(params: {
  lng: number;
  lat: number;
  keyword?: string;
  category?: string;
  radiusKm?: number;
  page?: number;
  size?: number;
}) {
  return request
    .get<ApiWrap<BackendSkillItem[] | Paged<BackendSkillItem>> | BackendSkillItem[]>("/api/skills/nearby", {
      params: {
        ...params,
        radiusKm: params.radiusKm ?? 5,
      },
    })
    .then((res) => unwrapList<BackendSkillItem>(res.data).map(normalizeSkill));
}

export function getSkillDetail(id: SkillId) {
  return request
    .get<ApiWrap<BackendSkillItem & { sellerNickname?: string; sellerCreditScore?: number }> | (BackendSkillItem & { sellerNickname?: string; sellerCreditScore?: number })>(
      `/api/skills/${id}`
    )
    .then((res) => {
      const raw = unwrapOne<BackendSkillItem & { sellerNickname?: string; sellerCreditScore?: number }>(res.data);
      const normalized = normalizeSkill(raw);
      return {
        ...normalized,
        sellerNickname: raw.sellerNickname || "User",
        sellerCreditScore: Number(raw.sellerCreditScore ?? 0),
      } as SkillDetail;
    });
}

export function getRecommendList(params?: { cursor?: string; size?: number }) {
  return request
    .get<ApiWrap<{ items?: BackendSkillItem[]; nextCursor?: string }> | { items?: BackendSkillItem[]; nextCursor?: string }>("/api/recommend", {
      params,
    })
    .then((res) => {
      const payload = unwrapOne<{ items?: BackendSkillItem[]; nextCursor?: string }>(res.data);
      return {
        items: Array.isArray(payload.items) ? payload.items.map(normalizeSkill) : [],
        nextCursor: payload.nextCursor || undefined,
      } as RecommendPage;
    });
}

export function logSearchEvent(keyword: string) {
  const value = String(keyword || "").trim();
  if (!value) return Promise.resolve();
  return request.post<ApiWrap<boolean> | boolean>("/api/user-events/search", { keyword: value }).then(() => undefined);
}

