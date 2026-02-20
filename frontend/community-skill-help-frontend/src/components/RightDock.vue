<template>
  <div class="dock">
    <div class="item" :class="{ active: active === 'add' }" @click="$emit('go-add')">
      <div class="circle">
        <span class="icon">+</span>
      </div>
      <div class="label">发布</div>
    </div>

    <div class="item" :class="{ active: active === 'manage' }" @click="$emit('go-list')">
      <div class="circle">
        <span class="icon">#</span>
      </div>
      <div class="label">我的</div>
    </div>

    <div class="item" :class="{ active: active === 'home' }" @click="$emit('go-home')">
      <div class="circle">
        <span class="icon">H</span>
      </div>
      <div class="label">首页</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ active?: string }>();
defineEmits(["go-add", "go-home", "go-list"]);
</script>

<style scoped>
.dock {
  position: fixed;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  width: 92px;
  padding: 14px 10px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

.item {
  width: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: 0.25s;
  user-select: none;
}

.circle {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(22, 119, 255, 0.08);
  border: 1px solid rgba(22, 119, 255, 0.18);
  transition: 0.25s;
}

.item:hover .circle {
  background: rgba(22, 119, 255, 0.16);
  border-color: rgba(22, 119, 255, 0.35);
  transform: translateY(-2px);
}

.item.active .circle {
  background: #1677ff;
  border-color: #1677ff;
}

.item.active .icon {
  color: #fff;
}

.icon {
  font-weight: 900;
  font-size: 18px;
  color: #1677ff;
  transition: 0.2s;
}

.label {
  font-size: 12px;
  color: #444;
  letter-spacing: 1px;
}

.item.active .label {
  color: #1677ff;
  font-weight: 600;
}
</style>
