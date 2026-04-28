<template>
  <Transition name="fade">
    <div v-if="visible" class="zh-modal-mask">
      <div class="zh-modal confirm-modal">
        <div class="confirm-header">
          <svg class="warning-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <h3 class="confirm-title">{{ title }}</h3>
        </div>
        <div class="confirm-body">
          <p>{{ message }}</p>
        </div>
        <div class="confirm-footer">
          <button class="zh-btn zh-btn-outline" @click="handleCancel">取消</button>
          <button class="zh-btn zh-btn-primary" @click="handleConfirm">确定</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref } from 'vue';

const visible = ref(false);
const title = ref('提示');
const message = ref('');
let resolvePromise = null; // 用来存放 Promise 的 resolve 函数

// 核心：调用这个方法会返回一个 Promise，只有点击确定或取消时，Promise 才会出结果
const show = (msg, t = '提示信息') => {
  message.value = msg;
  title.value = t;
  visible.value = true;
  return new Promise((resolve) => {
    resolvePromise = resolve;
  });
};

const handleConfirm = () => {
  visible.value = false;
  resolvePromise(true); // 返回 true 给 await
};

const handleCancel = () => {
  visible.value = false;
  resolvePromise(false); // 返回 false 给 await
};

defineExpose({ show });
</script>

<style scoped>
.confirm-modal { width: 400px; padding: 24px; border-radius: 2px; }
.confirm-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.warning-icon { width: 22px; height: 22px; color: #f1403c; }
.confirm-title { font-size: 16px; font-weight: 600; color: #121212; margin: 0; }
.confirm-body p { font-size: 14px; color: #444; line-height: 1.5; margin: 0; }
.confirm-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
.confirm-footer .zh-btn { padding: 6px 20px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>