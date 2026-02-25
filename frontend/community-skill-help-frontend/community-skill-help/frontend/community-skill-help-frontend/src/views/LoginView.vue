<template>
  <div class="page">
    <el-card class="card">
      <h2 class="title">请登录</h2>

      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          style="width: 100%"
          :loading="loading"
          @click="onLogin"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { loginApi } from "../api/auth";
import { useAuthStore } from "../store/auth";

const router = useRouter();
const store = useAuthStore();
const loading = ref(false);

const form = reactive({
  phone: "",
  password: "",
});

async function onLogin() {
  if (!form.phone || !form.password) {
    ElMessage.warning("请输入手机号和密码");
    return;
  }

  loading.value = true;
  try {
    const res = await loginApi(form);
    // 后端返回：{ code, message, data }
    if (res.data.code === 0) {
      store.setToken(res.data.data);
      ElMessage.success("登录成功");

      // ✅ 登录成功，跳转首页
      router.push("/home");
    } else {
      ElMessage.error(res.data.message || "登录失败");
    }
  } catch (e) {
    ElMessage.error("请求失败，请检查后端是否启动");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f5f5;
}
.card {
  width: 360px;
}
.title {
  text-align: center;
  margin: 0 0 18px 0;
}
</style>
