<template>
  <div id="app" :class="{ 'simple-mode-active': isSimpleMode }">
    <header class="zh-header">
      <div class="zh-header-inner">
        <h1 class="zh-logo" @click="activeTab = 'home'" style="cursor: pointer;">Campus Lost&Found</h1>

        <nav class="zh-nav">
          <a :class="{ active: activeTab === 'home' }" @click="activeTab = 'home'">首页大厅</a>

          <a v-if="isLoggedIn" :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">
            消息聊天
          </a>

          <a v-if="isLoggedIn" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">
            个人中心 <span v-if="unreadCount > 0" class="red-dot"></span>
          </a>

          <a v-if="isAdmin" :class="{ active: activeTab === 'admin' }" @click="activeTab = 'admin'">管理后台</a>
        </nav>

        <div class="zh-auth">
          <button class="zh-btn zh-btn-outline mode-toggle" @click="toggleMode">
            {{ isSimpleMode ? '切换到：标准模式' : '切换到：简洁模式' }}
          </button>

          <template v-if="isLoggedIn">
            <span class="greeting">你好，{{ currentUser.nickname }}</span>
            <button class="zh-btn zh-btn-outline" @click="doLogout">退出</button>
          </template>
          <template v-else>
            <button class="zh-btn zh-btn-primary" @click="showAuthModal = true">登录 / 注册</button>
          </template>
        </div>
      </div>
    </header>

    <main class="zh-main">
      <template v-if="activeTab === 'home'">
        <SimpleHomeView v-if="isSimpleMode" />
        <HomeView v-else />
      </template>

      <ChatView v-if="activeTab === 'chat'" />

      <ProfileView v-if="activeTab === 'profile'" @update-unread="fetchUnreadCount" />

      <AdminView v-if="activeTab === 'admin'" />
    </main>

    <div v-if="showAuthModal" class="zh-modal-mask">
      <div class="zh-modal">
        <button class="zh-modal-close" @click="showAuthModal = false">&times;</button>
        <div class="zh-modal-header">
          <h2>{{ isRegisterMode ? '注册账号' : '密码登录' }}</h2>
        </div>
        <div class="zh-modal-body">
          <input type="text" class="zh-input" v-model="authForm.username" placeholder="账号 或 绑定的邮箱" />
          <input type="password" class="zh-input" v-model="authForm.password" placeholder="密码" />

          <template v-if="isRegisterMode">
            <input type="email" class="zh-input" v-model="authForm.email" placeholder="邮箱" />
            <div style="display: flex; gap: 10px; margin-bottom: 15px;">
              <input type="text" class="zh-input" v-model="authForm.code" placeholder="验证码" style="margin-bottom:0;" />
              <button
                  class="zh-btn zh-btn-outline"
                  @click="sendCode"
                  :disabled="codeCountdown > 0"
                  :style="{ opacity: codeCountdown > 0 ? 0.6 : 1 }"
              >
                {{ codeCountdown > 0 ? `${codeCountdown} s` : '获取验证码' }}
              </button>
            </div>
            <input type="text" class="zh-input" v-model="authForm.nickname" placeholder="昵称" />
          </template>

          <button class="zh-btn zh-btn-primary zh-btn-block" @click="handleAuthAction">
            {{ isRegisterMode ? '注册' : '登录' }}
          </button>
          <div class="zh-auth-footer">
            <a @click="isRegisterMode = !isRegisterMode">
              {{ isRegisterMode ? '已有账号？直接登录' : '没有账号？免费注册' }}
            </a>
          </div>
        </div>
      </div>
    </div>

    <Toast ref="toastRef" />
    <Confirm ref="confirmRef" />
  </div>
</template>

<script setup>
// 修改原本的第一行，加上 provide
import { ref, onMounted, provide } from 'vue';
// 引入刚刚建好的组件
import Toast from './components/Toast.vue';
import Confirm from './components/Confirm.vue'; // 👉 新增导入
import HomeView from './views/HomeView.vue';
import ProfileView from './views/ProfileView.vue';
import AdminView from './views/AdminView.vue';
import SimpleHomeView from './views/SimpleHomeView.vue'; // 引入新视图

// 👇 导入聊天页面
import ChatView from './views/ChatView.vue';

const API_BASE = 'http://localhost:8080/api';
const activeTab = ref('home');
const isLoggedIn = ref(false);
const currentUser = ref({});
const unreadCount = ref(0);

const showAuthModal = ref(false);
const isRegisterMode = ref(false);
const authForm = ref({ username: '', password: '', email: '', code: '', nickname: '' });
const isAdmin = ref(false);
const isSimpleMode = ref(localStorage.getItem('mode') === 'simple');
const codeCountdown = ref(0);//验证码发送倒计时变量

// ====== 新增：全局 Toast 控制逻辑 ======
const toastRef = ref(null);
const confirmRef = ref(null);

const onShowMsg = (msg, type = 'info') => {
  if (toastRef.value) {
    toastRef.value.show(msg, type);
  }
};
// 像自来水一样提供给所有子组件
provide('showMessage', onShowMsg);

onMounted(() => {
  checkLoginState();
});

// 👉 新增：封装一个全局的异步确认方法
const onShowConfirm = (msg, title) => {
  if (confirmRef.value) {
    return confirmRef.value.show(msg, title);
  }
  return Promise.resolve(false);
};

provide('showConfirm', onShowConfirm); // 👉 向下游所有组件提供 showConfirm

window.addEventListener('switch-to-chat', (event) => {
  // 1. 切换标签到聊天页
  activeTab.value = 'chat';

  // 2. 如果事件里带了 userId，也可以在这里直接调用全局方法（双重保险）
  if (event.detail?.userId && window.startChatWithUserId) {
    window.startChatWithUserId(event.detail.userId);
  }
});


const checkLoginState = () => {
  const token = localStorage.getItem('token');
  const userStr = localStorage.getItem('user');
  if (token && userStr) {
    isLoggedIn.value = true;
    currentUser.value = JSON.parse(userStr);
    isAdmin.value = currentUser.value.role === 'ADMIN';
    fetchUnreadCount();
  } else {
    isLoggedIn.value = false;
    currentUser.value = {};
    activeTab.value = 'home';
  }

  window.dispatchEvent(new Event('auth-state-changed'));
};

const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return;
  try {
    const res = await fetch(`${API_BASE}/messages/my`, { headers: { 'Authorization': localStorage.getItem('token') } });
    const data = await res.json();
    if (data.success) {
      unreadCount.value = data.data.filter(m => !m.isRead).length;
    }
  } catch (e) {}
};

const doLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  checkLoginState();
};

const sendCode = async () => {
  if (!authForm.value.email) return onShowMsg('请先填写邮箱', 'error');
  if (codeCountdown.value > 0) return;

  // ==============================
  // 👇 【关键修改】点击后立即开始倒计时！
  // ==============================
  codeCountdown.value = 60;
  const timer = setInterval(() => {
    codeCountdown.value--;
    if (codeCountdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);

  // ==============================
  // 然后再发送网络请求
  // ==============================
  try {
    const res = await fetch(`${API_BASE}/users/send-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: authForm.value.email })
    });
    const data = await res.json();

    if (data.success) {
      onShowMsg('验证码发送成功！', 'success');
    } else {
      // ==============================
      // 👇 如果发送失败，把倒计时重置回来
      // ==============================
      codeCountdown.value = 0;
      clearInterval(timer);
      onShowMsg(data.message, 'error');
    }
  } catch (e) {
    // ==============================
    // 👇 网络错误也重置倒计时
    // ==============================
    codeCountdown.value = 0;
    onShowMsg('验证码发送失败', 'error');
  }
};

const handleAuthAction = async () => {
  const url = isRegisterMode.value ? `${API_BASE}/users/register` : `${API_BASE}/users/login`;
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(authForm.value)
    });
    const data = await res.json();

    if (data.success) {
      if (!isRegisterMode.value) {
        // ==========================================
        // 🚨 核心逻辑：这四行代码千万不能丢！
        // ==========================================
        localStorage.setItem('token', data.token);              // 1. 保存令牌
        localStorage.setItem('user', JSON.stringify(data.data));// 2. 保存用户信息
        checkLoginState();                                      // 3. 触发系统状态更新（头像、菜单显示等）
        showAuthModal.value = false;                            // 4. 关闭登录弹窗

        onShowMsg('登录成功！', 'success'); // 视觉提示放在最后
      } else {
        // 注册成功逻辑
        onShowMsg('注册成功，请登录！', 'success');
        isRegisterMode.value = false; // 切回登录模式
      }
    } else {
      // 密码错误、账号不存在等逻辑
      onShowMsg(data.message, 'error');
    }
  } catch (e) {
    onShowMsg('网络请求异常，请检查后端是否启动', 'error');
  }
};

// 👇 新增：暴露一个全局方法，让 HomeView 可以直接调用切换标签
window.switchToChatTab = () => {
  activeTab.value = 'chat';
};

const toggleMode = () => {
  isSimpleMode.value = !isSimpleMode.value;
  localStorage.setItem('mode', isSimpleMode.value ? 'simple' : 'standard');
  // 切换模式后，给用户一个丝滑的提示
  onShowMsg(`已切换到${isSimpleMode.value ? '简洁' : '标准'}模式`, 'success');
};

provide('isSimpleMode', isSimpleMode); // 提供给子组件使用
</script>

<style>
/* ================== 全局知乎风 CSS Reset ================== */
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: #f6f6f6; color: #121212; font-size: 15px; }
a { color: #8590a6; cursor: pointer; text-decoration: none; transition: .2s; }
a:hover { color: #056de8; }
h1, h2, h3, p { margin: 0; }

/* 顶部导航 */
.zh-header { background: #fff; box-shadow: 0 1px 3px rgba(18,18,.1); position: sticky; top: 0; z-index: 100; height: 52px; display: flex; justify-content: center; }
.zh-header-inner { width: 1000px; display: flex; align-items: center; padding: 0 16px; }
.zh-logo { color: #056de8; font-size: 22px; font-weight: bold; margin-right: 30px; }
.zh-nav { display: flex; gap: 30px; flex-grow: 1; }
.zh-nav a { font-size: 15px; color: #8590a6; line-height: 52px; position: relative; }
.zh-nav a.active { color: #121212; font-weight: 600; }
.zh-nav a.active::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 3px; background: #056de8; }
.zh-auth { display: flex; align-items: center; gap: 15px; }
.greeting { color: #8590a6; font-size: 14px; }
.red-dot { display: inline-block; width: 8px; height: 8px; background: #f1403c; border-radius: 50%; vertical-align: super; }

/* 主容器 */
.zh-main { width: 1000px; margin: 10px auto; padding: 0 16px; box-sizing: border-box; }

/* 卡片风格 */
.zh-card { background: #fff; border-radius: 2px; box-shadow: 0 1px 3px rgba(18,18,.1); padding: 20px; margin-bottom: 10px; }

/* 表单与输入框 */
.zh-input { width: 100%; box-sizing: border-box; padding: 8px 12px; font-size: 14px; border: 1px solid #ebebeb; border-radius: 3px; background: #f6f6f6; transition: .2s; outline: none; margin-bottom: 15px; }
.zh-input:focus { background: #fff; border-color: #056de8; }
select.zh-input { appearance: none; cursor: pointer; }

/* 按钮风格 */
.zh-btn { padding: 8px 16px; font-size: 14px; border-radius: 3px; cursor: pointer; text-align: center; border: 1px solid transparent; transition: .2s; outline: none; }
.zh-btn-primary { background: #056de8; color: #fff; }
.zh-btn-primary:hover { background: #045bb2; }
.zh-btn-outline { background: transparent; color: #056de8; border-color: #056de8; }
.zh-btn-outline:hover { background: rgba(5,109,232,.05); }
.zh-btn-block { width: 100%; padding: 10px; font-size: 15px; }

/* 弹窗风格 */
.zh-modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(18,18,.65); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.zh-modal { background: #fff; width: 400px; border-radius: 2px; box-shadow: 0 5px 20px rgba(0,0,0,.1); position: relative; }
.zh-modal-close { position: absolute; right: 15px; top: 15px; background: none; border: none; font-size: 24px; color: #8590a6; cursor: pointer; }
.zh-modal-header { padding: 24px 24px 0; text-align: center; }
.zh-modal-body { padding: 24px; }
.zh-auth-footer { text-align: center; margin-top: 15px; font-size: 14px; }

/* ================== 修复原生 Date 控件的突兀感 ================== */
input[type="date"].zh-input {
  font-family: inherit;
  height: 35px; /* 强制统一高度 */
  color: #121212;
  line-height: normal;
  background-color: #f6f6f6;
}
input[type="date"].zh-input:focus {
  background-color: #fff;
}
/* 针对 Chrome/Edge 浏览器日历图标的微调 */
input[type="date"].zh-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.5;
  transition: .2s;
}
input[type="date"].zh-input:focus::-webkit-calendar-picker-indicator {
  opacity: 1;
}

/* 全局控制：如果处于简洁模式，整体字号变大，颜色对比度更高 */
.simple-mode-active { font-size: 18px !important; }
.mode-toggle { margin-right: 15px; border-color: #056de8; color: #056de8; font-weight: bold; }
</style>