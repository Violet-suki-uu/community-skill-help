import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../store/auth";

import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import SkillAddView from "../views/SkillAddView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ✅ 默认进主页（主页允许未登录访问）
    { path: "/", redirect: "/home" },

    // ✅ 主页允许未登录访问（用于展示壳子 + 弹窗登录）
    { path: "/home", component: HomeView },

    // login 仍然保留（调试/单独访问也行）
    { path: "/login", component: LoginView },

 
    // 发布技能：必须登录（由守卫拦）
    { path: "/skill/add", component: SkillAddView },

  ],
});

// ✅ 关键：允许未登录访问 /home（不再强制跳 /login）
router.beforeEach((to) => {
  const auth = useAuthStore();

  // 公开页：未登录也能访问
  const publicPaths = ["/home", "/login", "/skills"];

  // 未登录访问非公开页 → 回主页（不去 login）
  if (!publicPaths.includes(to.path) && !auth.token) {
    return "/home";
  }

  // 已登录还访问 /login → 回主页
  if (to.path === "/login" && auth.token) {
    return "/home";
  }
});

export default router;
