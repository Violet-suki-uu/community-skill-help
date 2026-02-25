/**
 * 模块说明：路由配置模块。作用：定义页面路由与登录访问守卫。
 */
import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../store/auth";

import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import MessagesView from "../views/MessagesView.vue";
import SkillAddView from "../views/SkillAddView.vue";
import SkillDetailView from "../views/SkillDetailView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/home" },
    { path: "/home", component: HomeView },
    { path: "/login", component: LoginView },
    { path: "/skill/add", component: SkillAddView },
    { path: "/skills/:id", component: SkillDetailView },
    { path: "/messages", component: MessagesView },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const publicPaths = ["/home", "/login"];
  if (to.path.startsWith("/skills/")) {
    return;
  }

  if (!publicPaths.includes(to.path) && !auth.token) {
    return "/home";
  }

  if (to.path === "/login" && auth.token) {
    return "/home";
  }
});

export default router;

