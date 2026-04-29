<template>
  <div class="header">
    <div class="left">
      <div class="logo" aria-label="小油条">
        <img src="/favicon.png" alt="" />
        <span>小油条</span>
      </div>
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
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.9);
  color: #14243d;
  border-bottom: 1px solid rgba(221, 228, 238, 0.62);
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 28px rgba(28, 49, 76, 0.07);
}

.left {
  display: flex;
  align-items: center;
  gap: 34px;
  min-width: 0;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  font-family: "Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", sans-serif;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.02em;
  color: #14243d;
  white-space: nowrap;
  transition: transform 0.2s ease;
}

.logo img {
  width: 46px;
  height: 46px;
  display: block;
  object-fit: contain;
}

.logo:hover {
  transform: translateY(-1px);
}

.search {
  width: min(380px, 36vw);
}

.user {
  cursor: pointer;
  font-weight: 600;
  color: #14243d;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 999px 0 0 999px;
  background: #f4f6f8;
  box-shadow: none;
}

:deep(.el-input-group__append) {
  border-radius: 0 999px 999px 0;
  overflow: hidden;
}

:deep(.el-button--primary) {
  border-radius: 999px;
  padding-inline: 26px;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b79ff, #2567ee);
  border: none;
  box-shadow: 0 12px 28px rgba(37, 103, 238, 0.28);
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

  .logo {
    font-size: 26px;
  }

  .logo img {
    width: 40px;
    height: 40px;
  }

  .search {
    width: min(62vw, 360px);
  }

  .right {
    justify-content: flex-end;
  }
}
</style>
