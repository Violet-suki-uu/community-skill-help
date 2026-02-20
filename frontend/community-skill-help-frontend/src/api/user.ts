import http from "./http";

export function getMeApi() {
  return http.get("/api/users/me");
}
