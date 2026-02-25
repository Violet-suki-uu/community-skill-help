/**
 * 模块说明：认证 API 模块。作用：封装注册、登录请求。
 */
import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
  }),
  actions: {
    setToken(token: string) {
      this.token = token;
      localStorage.setItem("token", token);
    },
    logout() {
      this.token = "";
      localStorage.removeItem("token");
    },
  },
});

