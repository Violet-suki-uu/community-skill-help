<template>
  <el-dialog
    v-model="visible"
    class="auth-dialog"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    width="820px"
  >
    <!-- Header -->
    <template #header>
      <div class="dlg-header">
        <!-- 吉祥物 -->
        <div class="mascot-wrap">
          <img src="../assets/mascot.png" alt="吉祥物" />
        </div>

        <!-- Tabs -->
        <div class="tabs">
          <div
            class="tab"
            :class="{ active: mode === 'login' }"
            @click="switchMode('login')"
          >
            登录
          </div>
          <div
            class="tab"
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >
            注册
          </div>
        </div>

        <!-- Close -->
        <div class="close" @click="close">✕</div>
      </div>
    </template>

    <!-- Body -->
    <div class="dlg-body">
      <!-- 左侧 -->
      <div class="left">
        <div class="form-header">
          <h2 class="title">{{ mode === 'login' ? '账号登录' : '创建账号' }}</h2>
          <div class="subtitle">
            {{ mode === 'login' ? '欢迎回来，请登录您的账号' : '填写信息，快速创建账号' }}
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model.trim="form.phone"
              size="large"
              placeholder="请输入手机号"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model.trim="form.password"
              type="password"
              show-password
              size="large"
              placeholder="请输入密码"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="昵称" prop="nickname">
            <el-input
              v-model.trim="form.nickname"
              size="large"
              placeholder="可选，可后续修改"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <div class="form-footer">
            <el-button
              type="primary"
              class="auth-submit"
              :loading="loading"
              @click="onSubmit"
            >
              {{ mode === 'login' ? '立即登录' : '立即注册' }}
            </el-button>

            <div class="quick-switch" @click="toggleMode">
              {{ mode === 'login' ? '没有账号？点击注册' : '已有账号？点击登录' }}
            </div>
          </div>
        </el-form>
      </div>

      <!-- 右侧 -->
      <div class="right">
        <div class="hint">
          <div class="icon">🔧</div>
          <div class="big">技能互助社区</div>
          <div class="small">发布你的技能</div>
          <div class="small">寻找所需帮助</div>
          <div class="small">建立互助连接</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
// 组件说明：登录注册弹窗组件。作用：统一处理账号认证交互。
import { ref, reactive, watch, computed } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { loginApi, registerApi } from "../api/auth";
import { useAuthStore } from "../store/auth";

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
  (e: "success"): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const mode = ref<"login" | "register">("login");
const loading = ref(false);
const formRef = ref<FormInstance>();

const store = useAuthStore();

const form = reactive({
  phone: "",
  password: "",
  nickname: "",
});

const rules: FormRules = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1\d{10}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码至少 6 位", trigger: "blur" },
  ],
  nickname: [
    {
      validator: (_rule, value, callback) => {
        if (mode.value !== "register") return callback();
        if (!value) return callback();
        if (value.length < 2) return callback(new Error("昵称至少 2 个字符"));
        callback();
      },
      trigger: "blur",
    },
  ],
};

function resetForm() {
  form.phone = "";
  form.password = "";
  form.nickname = "";
  formRef.value?.clearValidate();
}

function switchMode(next: "login" | "register") {
  mode.value = next;
  formRef.value?.clearValidate(["nickname"]);
}

function toggleMode() {
  switchMode(mode.value === "login" ? "register" : "login");
}

function close() {
  visible.value = false;
}

async function onSubmit() {
  if (loading.value || !formRef.value) return;

  try {
    await formRef.value.validate();
    loading.value = true;

    if (mode.value === "login") {
      const res = await loginApi({ phone: form.phone, password: form.password });
      if (res.data.code === 0) {
        store.setToken(res.data.data);
        ElMessage.success("登录成功");
        visible.value = false;
        emit("success");
      } else {
        ElMessage.error(res.data.message || "登录失败");
      }
    } else {
      const res = await registerApi({
        phone: form.phone,
        password: form.password,
        nickname: form.nickname || undefined,
      });
      if (res.data.code === 0) {
        ElMessage.success("注册成功，请登录");
        switchMode("login");
      } else {
        ElMessage.error(res.data.message || "注册失败");
      }
    }
  } catch {
    ElMessage.warning("请先完善表单信息");
  } finally {
    loading.value = false;
  }
}

// 关闭弹窗时重置表单
watch(
  () => props.modelValue,
  (isOpen) => {
    if (!isOpen) resetForm();
  }
);
</script>

<style scoped>
/* ================= 外层对话框 ================= */
:deep(.auth-dialog .el-dialog) {
  width: 820px !important;
  border-radius: 24px !important;
  padding: 10px 12px !important;
  overflow: hidden !important;
  box-shadow:
    0 20px 50px rgba(22, 119, 255, 0.12),
    0 10px 30px rgba(22, 119, 255, 0.08) !important;
  border: 1.5px solid rgba(232, 241, 255, 0.8) !important;
  background: linear-gradient(135deg, #ffffff 0%, #f9fbfe 100%) !important;
}
:deep(.auth-dialog .el-dialog__body) {
  padding: 16px 20px !important;
}

/* ================= Header ================= */
.dlg-header {
  position: relative;
  height: 56px;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding-top: 10px;
}

/* 吉祥物 - 带浮动动效 */
.mascot-wrap {
  position: absolute;
  left: 350px;
  top: -420px;
  width: 640px;
  height: 720px;
  z-index: 10;
  pointer-events: none;
}
.mascot-wrap img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.2));
  animation: float 3.5s ease-in-out infinite;
  border-radius: 20px;
}
@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(-2deg);
  }
  50% {
    transform: translateY(10px) rotate(0deg);
  }
}

/* Tabs */
.tabs {
  display: flex;
  gap: 4px;
  background: #f0f7ff;
  padding: 4px;
  border-radius: 18px;
  border: 1px solid #e6f0ff;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.6);
}
.tab {
  padding: 10px 24px;
  border-radius: 14px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  color: #4a7db9;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.tab.active {
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: #fff;
  box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

/* Close */
.close {
  position: absolute;
  right: 16px;
  top: 16px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e8f1ff;
  transition: all 0.3s ease;
  font-size: 16px;
  color: #8ca7d9;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.1);
}
.close:hover {
  background: #fff;
  border-color: #1677ff;
  color: #1677ff;
  transform: rotate(90deg) scale(1.05);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
}

/* ================= Body ================= */
.dlg-body {
  display: flex;
  gap: 24px;
  min-height: 300px;
  margin-top: 4px;
}
.left {
  flex: 1;
  padding-right: 20px;
  border-right: 1px solid #f0f7ff;
}
.right {
  width: 240px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e6f0ff;
  box-shadow: inset 0 1px 3px rgba(22, 119, 255, 0.05);
  padding: 24px 18px;
}

/* ================= Form ================= */
.form-header {
  margin-bottom: 20px;
}
.title {
  font-size: 22px;
  margin-bottom: 6px;
  font-weight: 700;
  color: #1a1a1a;
}
.subtitle {
  color: #7a9bdb;
  font-size: 13px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a7db9;
  font-size: 14px;
}
:deep(.el-input__wrapper) {
  border-radius: 16px !important;
  padding: 12px 16px !important;
  background: #f8fbff !important;
  border: 1.5px solid #e6f0ff !important;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
  box-shadow: inset 0 1px 2px rgba(22, 119, 255, 0.03) !important;
}
:deep(.el-input__wrapper.is-focus) {
  background: #fff !important;
  border-color: #1677ff !important;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15) !important;
  transform: translateY(-1px);
}
:deep(.el-input__inner) {
  color: #1a73e8;
  font-size: 14px;
  font-weight: 500;
}
:deep(.el-input__inner::placeholder) {
  color: #9ab8e8;
  font-size: 13px;
}

/* ================= Footer ================= */
.form-footer {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #f0f7ff;
}
.auth-submit {
  height: 44px;
  width: 220px;
  border-radius: 16px !important;
  font-weight: 600;
  font-size: 15px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}
.auth-submit::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.3);
}
.auth-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
}
.auth-submit:active {
  transform: translateY(0);
  box-shadow: 0 3px 8px rgba(22, 119, 255, 0.2);
}
.quick-switch {
  text-align: center;
  color: #6b9bdb;
  cursor: pointer;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all 0.3s ease;
}
.quick-switch:hover {
  color: #1677ff;
  background: rgba(240, 247, 255, 0.6);
  transform: translateY(-1px);
}

/* ================= Hint ================= */
.hint {
  text-align: center;
  color: #4a7db9;
}
.icon {
  font-size: 32px;
  margin-bottom: 14px;
  filter: drop-shadow(0 3px 6px rgba(22, 119, 255, 0.15));
}
.big {
  font-size: 18px;
  font-weight: 700;
  color: #1677ff;
  margin-bottom: 14px;
}
.small {
  font-size: 12px;
  line-height: 1.6;
  margin-bottom: 6px;
  color: #6b9bdb;
}
.small:last-child {
  margin-bottom: 0;
}

/* ================= 响应式 ================= */
@media (max-width: 900px) {
  :deep(.auth-dialog .el-dialog) {
    width: 90vw !important;
    border-radius: 20px !important;
  }
  .mascot-wrap {
    width: 240px;
    height: 280px;
    left: -20px;
    top: -150px;
  }
  .dlg-header {
    height: 50px;
    padding-top: 8px;
  }
  .dlg-body {
    flex-direction: column;
    gap: 20px;
  }
  .left {
    padding-right: 0;
    border-right: none;
    border-bottom: 1px solid #f0f7ff;
    padding-bottom: 20px;
  }
  .right {
    width: 100%;
    border-radius: 16px;
  }
  .tabs {
    transform: scale(0.9);
  }
  .close {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }
  .title {
    font-size: 20px;
  }
  .auth-submit {
    width: 200px;
    height: 42px;
    font-size: 14px;
  }
}
</style>
