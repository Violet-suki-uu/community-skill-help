import request from "./http";

type ApiWrap<T> = {
  code?: number;
  message?: string;
  data?: T;
};

type Paged<T> = {
  records?: T[];
  list?: T[];
  items?: T[];
};

export interface SkillItem {
  id: number;
  title: string;
  price: number | string;
  image: string;
  status?: number;
  category?: string;
  description?: string;
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

  if (typeof payload === "object" && "data" in payload) {
    const data = payload.data;
    if (Array.isArray(data)) return data;
    return unwrapPaged<T>(data as Paged<T>);
  }

  return unwrapPaged<T>(payload as Paged<T>);
}

function unwrapOne<T>(payload: ApiWrap<T> | T): T {
  if (typeof payload === "object" && payload !== null && "data" in payload && payload.data !== undefined) {
    return payload.data as T;
  }
  return payload as T;
}

export function getSkillList(params?: { keyword?: string; category?: string }) {
  return request
    .get<ApiWrap<SkillItem[] | Paged<SkillItem>> | SkillItem[]>("/api/skills", { params })
    .then((res) => unwrapList<SkillItem>(res.data));
}

export function getMySkills() {
  return request
    .get<ApiWrap<SkillItem[] | Paged<SkillItem>> | SkillItem[]>("/api/skills/mine")
    .then((res) => unwrapList<SkillItem>(res.data));
}

export function addSkill(data: Partial<SkillItem>) {
  return request
    .post<ApiWrap<SkillItem> | SkillItem>("/api/skills", data)
    .then((res) => unwrapOne(res.data));
}

export async function updateSkillStatus(id: number, status: number) {
  try {
    return await request.patch(`/api/skills/${id}/status`, { status });
  } catch {
    return request.put(`/api/skills/${id}`, { status });
  }
}

export function deleteSkill(id: number) {
  return request.delete(`/api/skills/${id}`);
}
