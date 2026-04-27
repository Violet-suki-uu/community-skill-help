import { defineStore } from "pinia";
import { logoutApi } from "../api/auth";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
  }),
  actions: {
    setToken(token: string) {
      this.token = token;
      localStorage.setItem("token", token);
    },
    async logout() {
      try {
        if (this.token) await logoutApi();
      } catch (_) {
        // 忽略登出失败，本地状态仍需清理
      }
      this.token = "";
      localStorage.removeItem("token");
    },
  },
});
