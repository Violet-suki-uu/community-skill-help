/**
 * 模块说明：用户 API 模块。作用：封装当前用户信息接口。
 */
import http from "./http";

export function getMeApi() {
  return http.get("/api/users/me");
}

