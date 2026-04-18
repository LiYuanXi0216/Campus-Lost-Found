<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-brand">校园失物招领 V3 (Vue版)</div>
      <div class="nav-tabs">
        <button :class="{ active: activeTab === 'home' }" @click="activeTab = 'home'">首页大厅</button>
        <button v-if="isLoggedIn" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">个人中心</button>
        <button v-if="isAdmin" :class="{ active: activeTab === 'admin' }" @click="activeTab = 'admin'">管理员看板</button>
      </div>
      <div class="nav-auth">
        <template v-if="isLoggedIn">
          <span class="greeting">你好, {{ currentUser.nickname }}</span>
          <button class="btn-logout" @click="doLogout">退出登录</button>
        </template>
        <template v-else>
          <button class="btn-login" @click="showAuthModal = true">登录 / 注册</button>
        </template>
      </div>
    </nav>

    <HomeView :activeTab="activeTab" />

    <div v-if="showAuthModal" class="modal-overlay">
      <div class="modal-content">
        <span class="close-btn" @click="showAuthModal = false">&times;</span>
        <h2>{{ isRegisterMode ? '账号注册' : '用户登录' }}</h2>

        <input type="text" v-model="authForm.username" placeholder="账号 或 绑定的邮箱 (必填)" />
        <input type="password" v-model="authForm.password" placeholder="密码 (必填)" />

        <template v-if="isRegisterMode">
          <input type="email" v-model="authForm.email" placeholder="邮箱" />
          <div style="display: flex; gap: 10px;">
            <input type="text" v-model="authForm.code" placeholder="验证码" />
            <button class="btn btn-code" @click="sendCode">获取验证码</button>
          </div>
          <input type="text" v-model="authForm.nickname" placeholder="昵称" />
        </template>

        <button class="btn btn-primary submit-btn" @click="handleAuthAction">
          {{ isRegisterMode ? '立即注册' : '立即登录' }}
        </button>
        <p class="toggle-text" @click="isRegisterMode = !isRegisterMode">
          {{ isRegisterMode ? '已有账号？去登录' : '没有账号？去注册' }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import HomeView from './views/HomeView.vue';

const API_BASE = 'http://localhost:8080/api';

// 全局状态
const activeTab = ref('home');
const isLoggedIn = ref(false);
const isAdmin = ref(false);
const currentUser = ref({});

// 弹窗状态
const showAuthModal = ref(false);
const isRegisterMode = ref(false);
const authForm = ref({ username: '', password: '', email: '', code: '', nickname: '' });

// 页面加载时检查本地存储
onMounted(() => {
  checkLoginState();
});

const checkLoginState = () => {
  const token = localStorage.getItem('token');
  const userStr = localStorage.getItem('user');
  if (token && userStr) {
    isLoggedIn.value = true;
    currentUser.value = JSON.parse(userStr);
    isAdmin.value = currentUser.value.role === 'ADMIN';
  } else {
    isLoggedIn.value = false;
    isAdmin.value = false;
    currentUser.value = {};
    activeTab.value = 'home'; // 未登录强制回首页
  }
};

const doLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  checkLoginState();
};

const sendCode = async () => {
  if (!authForm.value.email) return alert('请先填写邮箱');
  try {
    const res = await fetch(`${API_BASE}/users/send-code`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: authForm.value.email })
    });
    const data = await res.json();
    alert(data.message);
  } catch (e) { alert('验证码发送失败'); }
};

const handleAuthAction = async () => {
  const url = isRegisterMode.value ? `${API_BASE}/users/register` : `${API_BASE}/users/login`;
  try {
    const res = await fetch(url, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(authForm.value)
    });
    const data = await res.json();
    if (data.success) {
      if (!isRegisterMode.value) {
        // 登录成功
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.data));
        checkLoginState();
        showAuthModal.value = false;
      } else {
        // 注册成功
        alert('注册成功，请登录！');
        isRegisterMode.value = false; // 切回登录
      }
    } else {
      alert(data.message);
    }
  } catch (e) { alert('网络请求异常'); }
};
</script>

<style>
/* 全局样式 resets */
body { margin: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; color: #333; }
#app { min-height: 100vh; }

/* 导航栏样式 */
.navbar { background: white; padding: 15px 30px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 100; }
.nav-brand { font-size: 20px; font-weight: bold; color: #007bff; }
.nav-tabs button { background: none; border: none; font-size: 16px; padding: 10px 15px; cursor: pointer; color: #555; }
.nav-tabs button.active { color: #007bff; border-bottom: 2px solid #007bff; font-weight: bold; }
.greeting { margin-right: 15px; font-weight: bold; }
.btn-login { background: #007bff; color: white; padding: 8px 15px; border: none; border-radius: 5px; cursor: pointer; }
.btn-logout { background: #eee; color: #333; padding: 8px 15px; border: none; border-radius: 5px; cursor: pointer; }
.btn-code { background: #28a745; color: white; padding: 0 15px; border: none; border-radius: 5px; white-space: nowrap; }

/* 登录弹窗专用样式 */
.toggle-text { text-align: center; margin-top: 15px; cursor: pointer; color: #007bff; font-size: 14px; }
.toggle-text:hover { text-decoration: underline; }

.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex; justify-content: center; align-items: center;
  z-index: 9999; /* 保证在最顶层 */
}
.modal-content {
  background: white; padding: 30px; border-radius: 10px;
  width: 90%; max-width: 400px; position: relative;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}
.close-btn {
  position: absolute; top: 15px; right: 20px;
  font-size: 24px; cursor: pointer; color: #888;
}
.modal-content input {
  width: 100%; padding: 12px; margin-bottom: 15px;
  border: 1px solid #ddd; border-radius: 5px; box-sizing: border-box;
}
.submit-btn {
  width: 100%; padding: 12px; font-size: 16px; margin-top: 10px;
}
</style>

