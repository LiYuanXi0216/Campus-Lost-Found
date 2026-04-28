<template>
  <Transition name="toast">
    <div v-if="visible" :class="['zh-toast', type]">
      <svg v-if="type === 'success'" class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
      <svg v-else-if="type === 'error'" class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
      <svg v-else class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>

      <span class="message">{{ message }}</span>
    </div>
  </Transition>
</template>

<script setup>
import { ref } from 'vue';

const visible = ref(false);
const message = ref('');
const type = ref('info');

const show = (msg, t = 'info') => {
  message.value = msg;
  type.value = t;
  visible.value = true;
  setTimeout(() => visible.value = false, 3000);
};

defineExpose({ show });
</script>

<style scoped>
/* 知乎风极简视觉 */
.zh-toast {
  position: fixed;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffffff;
  padding: 10px 20px;
  border-radius: 2px; /* 直角风格 */
  box-shadow: 0 3px 12px rgba(18,18,18,0.08), 0 1px 4px rgba(18,18,18,0.04); /* 克制的阴影 */
  border: 1px solid #ebebeb;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 100000;
  min-width: 150px;
}

.toast-icon { width: 18px; height: 18px; }
.zh-toast.success .toast-icon { color: #056de8; } /* 知乎蓝 */
.zh-toast.error .toast-icon { color: #f1403c; }   /* 知乎红 */
.zh-toast.info .toast-icon { color: #8590a6; }    /* 知乎灰 */

.message { font-size: 14px; color: #121212; font-weight: 500; }

.toast-enter-active, .toast-leave-active { transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -15px); }
</style>