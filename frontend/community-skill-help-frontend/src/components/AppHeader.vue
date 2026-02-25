<template>
  <div class="header">
    <div class="left">
      <div class="logo">SkillHelp</div>
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
// 组件说明：页面头部组件。作用：提供搜索、登录入口和导航操作。
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
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1677ffaf; /* 顶部蓝色 */
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,.2);
}
.left { display: flex; align-items: center; gap: 14px; }
.logo { font-weight: 900; font-size: 22px; letter-spacing: .5px; }
.search { width: 420px; }
.right { display: flex; align-items: center; gap: 12px; }
.user { cursor: pointer; font-weight: 600; }
</style>

