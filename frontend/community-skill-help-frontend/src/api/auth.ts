import http from "./http";

export interface LoginReq {
  phone: string;
  password: string;
}

export interface RegisterReq {
  phone: string;
  password: string;
  nickname?: string;
}

export function loginApi(data: LoginReq) {
  return http.post("/api/auth/login", data);
}

export function registerApi(data: RegisterReq) {
  return http.post("/api/auth/register", data);
}
