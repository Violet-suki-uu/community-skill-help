<template>
  <div class="header">
    <div class="left">
      <div class="logo">邻光</div>
      <el-input class="search" v-model="kw" placeholder="搜索技能 / 服务" clearable>
        <template #append>
          <el-button @click="$emit('search', kw)">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="right">
      <el-button v-if="!token" type="primary" @click="$emit('open-auth')">登录 / 注册</el-button>
      <el-dropdown v-else>
        <span class="user">已登录 ▾</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$emit('go-home')">回到首页</el-dropdown-item>
            <el-dropdown-item divided @click="$emit('logout')">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useAuthStore } from "../store/auth";

defineEmits(["open-auth", "logout", "go-home", "search"]);

const kw = ref("");
const auth = useAuthStore();
const token = computed(() => auth.token);
</script>

<style scoped>
.header {
  height: 64px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.78);
  color: #18324d;
  border-bottom: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  box-shadow: 0 14px 34px rgba(43, 87, 126, 0.1);
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.logo {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 3px 2px 7px;
  font-family: "STKaiti", "KaiTi", "Microsoft YaHei", serif;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 0.1em;
  color: #17324e;
  text-shadow: 0 10px 28px rgba(22, 119, 255, 0.18);
  white-space: nowrap;
}

.logo::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0.1em;
  bottom: 0;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1677ff, #35b889, #ffb26b);
  box-shadow: 0 8px 20px rgba(53, 184, 137, 0.22);
  transform-origin: left;
  animation: logo-breathe 3.8s ease-in-out infinite;
}

.search {
  width: min(420px, 42vw);
}

.user {
  cursor: pointer;
  font-weight: 600;
  color: #18324d;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.el-input__wrapper) {
  border-radius: 999px 0 0 999px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 0 0 1px rgba(36, 114, 196, 0.1) inset;
}

:deep(.el-input-group__append) {
  border-radius: 0 999px 999px 0;
  overflow: hidden;
}

:deep(.el-button--primary) {
  border-radius: 999px;
  padding-inline: 18px;
  background: linear-gradient(135deg, #1677ff, #28a6df);
  border: none;
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.22);
}

@keyframes logo-breathe {
  0%,
  100% {
    opacity: 0.66;
    transform: scaleX(0.68);
  }
  50% {
    opacity: 1;
    transform: scaleX(1);
  }
}

@media (max-width: 760px) {
  .header {
    height: auto;
    min-height: 64px;
    padding: 12px 16px;
    gap: 12px;
    align-items: stretch;
    flex-direction: column;
  }

  .left {
    width: 100%;
    align-items: center;
    justify-content: space-between;
  }

  .search {
    width: min(62vw, 360px);
  }

  .right {
    justify-content: flex-end;
  }
}
</style>
