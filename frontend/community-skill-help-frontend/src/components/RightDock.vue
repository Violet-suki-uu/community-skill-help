<template>
  <div class="dock">
    <div class="item" :class="{ active: active === 'add' }" @click="$emit('go-add')">
      <div class="circle">
        <span class="icon">+</span>
      </div>
      <div class="label">发布</div>
    </div>

    <div class="item" :class="{ active: active === 'chat' }" @click="$emit('go-chat')">
      <div class="circle">
        <span class="icon">•••</span>
      </div>
      <div class="label">消息</div>
    </div>

    <div class="item" :class="{ active: active === 'home' }" @click="$emit('go-home')">
      <div class="circle">
        <span class="icon">⌂</span>
      </div>
      <div class="label">首页</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ active?: string }>();
defineEmits(["go-add", "go-home", "go-chat"]);
</script>

<style scoped>
.dock {
  position: fixed;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  width: 86px;
  padding: 16px 10px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 22px 56px rgba(36, 55, 85, 0.14);
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  background: #fff;
  border: 2px solid rgba(59, 121, 255, 0.24);
  transition: 0.25s;
}

.item:hover .circle {
  background: rgba(59, 121, 255, 0.08);
  border-color: rgba(59, 121, 255, 0.45);
  transform: translateY(-2px);
}

.item.active .circle {
  background: linear-gradient(135deg, #3b79ff, #2567ee);
  border-color: rgba(59, 121, 255, 0.45);
  box-shadow: 0 10px 22px rgba(37, 103, 238, 0.25);
}

.item.active .icon {
  color: #fff;
}

.icon {
  font-weight: 900;
  font-size: 18px;
  color: #3b79ff;
  transition: 0.2s;
}

.label {
  font-size: 12px;
  color: #34445c;
  letter-spacing: 0;
}

.item.active .label {
  color: #2567ee;
  font-weight: 600;
}

@media (max-width: 760px) {
  .dock {
    right: 12px;
    width: 70px;
    padding: 12px 8px;
    gap: 14px;
    border-radius: 18px;
  }

  .item {
    width: 56px;
  }

  .circle {
    width: 42px;
    height: 42px;
  }
}
</style>
