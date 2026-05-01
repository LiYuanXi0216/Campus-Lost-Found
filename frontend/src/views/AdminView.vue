<template>
  <div class="admin-container">

    <div class="stat-grid">
      <div class="zh-card stat-card">
        <div class="stat-title">总计用户 (人)</div>
        <div class="stat-value text-primary">{{ stats.totalUsers || 0 }}</div>
      </div>
      <div class="zh-card stat-card">
        <div class="stat-title">全站帖子 (条)</div>
        <div class="stat-value">{{ stats.totalPosts || 0 }}</div>
      </div>
      <div class="zh-card stat-card">
        <div class="stat-title">已解决 (条)</div>
        <div class="stat-value text-success">{{ stats.resolvedPosts || 0 }}</div>
      </div>
      <div class="zh-card stat-card">
        <div class="stat-title">寻/招中 (条)</div>
        <div class="stat-value text-danger">{{ stats.pendingPosts || 0 }}</div>
      </div>
    </div>

    <div class="admin-layout">
      <div class="zh-card main-panel">
        <div class="panel-header">
          <h2 class="panel-title">内容管控面板</h2>
          <div class="panel-actions">
            <button class="zh-btn zh-btn-outline" @click="fetchPosts">刷新数据</button>
            <button
                class="zh-btn zh-btn-danger"
                :disabled="selectedPostIds.length === 0"
                @click="batchDelete"
            >
              批量删除 ({{ selectedPostIds.length }})
            </button>
          </div>
        </div>

        <div class="table-container">
          <table class="zh-table">
            <thead>
            <tr>
              <th width="40"><input type="checkbox" @change="toggleSelectAll" :checked="isAllSelected" /></th>
              <th width="60">ID</th>
              <th width="60">类型</th>
              <th>帖子标题</th>
              <th width="120">发布者 ID</th>
              <th width="150">发布时间</th>
              <th width="80">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="posts.length === 0"><td colspan="7" class="empty-state">暂无帖子数据</td></tr>
            <tr v-for="post in posts" :key="post.id" :class="{'selected-row': selectedPostIds.includes(post.id)}">
              <td><input type="checkbox" :value="post.id" v-model="selectedPostIds" /></td>
              <td class="text-gray">{{ post.id }}</td>
              <td>
                  <span :class="['tag', post.type === 'LOST' ? 'tag-red' : 'tag-green']">
                    {{ post.type === 'LOST' ? '寻物' : '招领' }}
                  </span>
              </td>
              <td class="td-title">{{ post.title }}</td>
              <td class="text-gray">{{ post.publisherId }}</td>
              <td class="text-gray">{{ formatDate(post.createTime) }}</td>
              <td>
                <a class="action-link text-danger" @click="deletePost(post.id)">删除</a>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="zh-card side-panel">
        <div class="panel-header" style="padding-bottom: 15px; border-bottom: 1px solid #f0f2f7;">
          <h2 class="panel-title">安全审计日志</h2>
        </div>
        <div class="log-list">
          <div v-if="logs.length === 0" class="empty-state" style="padding: 20px 0;">暂无操作记录</div>
          <div v-for="log in logs" :key="log.id" class="log-item">
            <div class="log-time">{{ formatDate(log.createTime) }}</div>
            <div class="log-action">
              <span class="tag tag-gray">管理员 ID: {{ log.adminId }}</span>
              <span class="text-danger" style="font-size: 13px; font-weight: bold; margin-left: 5px;">{{ log.actionType }}</span>
            </div>
            <div class="log-detail">{{ log.detail }}</div>
          </div>
        </div>
      </div>

      <!-- 在 <div class="admin-layout"> 里新增这个面板 -->
      <div class="zh-card main-panel" style="margin-top: 15px;">
        <div class="panel-header">
          <h2 class="panel-title">用户私信管理</h2>
          <div class="panel-actions">
            <button class="zh-btn zh-btn-outline" @click="fetchMessages">刷新私信</button>
          </div>
        </div>
        <div class="table-container">
          <table class="zh-table">
            <thead>
            <tr>
              <th width="60">私信ID</th>
              <th width="100">发送者ID</th>
              <th width="100">接收者ID</th>
              <th>私信内容</th>
              <th width="150">发送时间</th>
              <th width="80">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="messages.length === 0"><td colspan="6" class="empty-state">暂无私信数据</td></tr>
            <tr v-for="msg in messages" :key="msg.id">
              <td class="text-gray">{{ msg.id }}</td>
              <td class="text-gray">{{ msg.senderId }}</td>
              <td class="text-gray">{{ msg.receiverId }}</td>
              <td class="td-title">{{ msg.content }}</td>
              <td class="text-gray">{{ formatDate(msg.sendTime) }}</td>
              <td>
                <a class="action-link text-danger" @click="deleteMessage(msg.id)">删除</a>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed, inject } from 'vue';

const API_BASE = 'http://localhost:8080/api';
const showMessage = inject('showMessage');
const showConfirm = inject('showConfirm');

// ==============================
// 👇 原有代码（保持不动）
// ==============================
const stats = ref({});
const posts = ref([]);
const logs = ref([]);
const selectedPostIds = ref([]);

const getHeaders = () => ({
  'Authorization': localStorage.getItem('token') || '',
  'Content-Type': 'application/json'
});

// ==============================
// 👇 【新增代码 1/3】在这里加私信的 ref
// ==============================
const messages = ref([]);

// ==============================
// 👇 原有代码（保持不动）
// ==============================
onMounted(() => {
  fetchDashboardData();
  // ==============================
  // 👇 【新增代码 2/3】在这里加调用
  // ==============================
  fetchMessages();
});

// 聚合拉取大盘数据
const fetchDashboardData = () => {
  fetchStats();
  fetchPosts();
  fetchLogs();
};

// 1. 拉取统计数据
const fetchStats = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/dashboard/stats`, { headers: getHeaders() });
    const data = await res.json();
    if (data.success) stats.value = data.data;
  } catch (e) {}
};

// 2. 拉取全站帖子
const fetchPosts = async () => {
  try {
    const res = await fetch(`${API_BASE}/posts/search?type=ALL`, { headers: getHeaders() });
    posts.value = await res.json();
    selectedPostIds.value = [];
  } catch (e) {}
};

// 3. 拉取审计日志
const fetchLogs = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/logs`, { headers: getHeaders() });
    const data = await res.json();
    if (data.success) logs.value = data.data;
  } catch (e) {}
};

// ==============================
// 👇 【新增代码 3/3】在这里加私信的函数
// ==============================
// 拉取所有私信
const fetchMessages = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/messages`, { headers: getHeaders() });
    const data = await res.json(); // 现在返回的是带 success 的结构
    if (data.success) {
      messages.value = data.data; // 取 data 字段
    }
  } catch (e) {}
};

// 删除私信
const deleteMessage = async (msgId) => {
  const isConfirmed = await showConfirm(`确认删除这条私信吗？将通知发送者和接收者。`, '私信删除确认');
  if (!isConfirmed) return;

  try {
    const res = await fetch(`${API_BASE}/admin/messages/${msgId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    const data = await res.json(); // 现在返回的是带 success 的结构
    if (data.success) {
      showMessage(data.message, 'success'); // 直接用后端返回的 message
      fetchMessages();
      fetchLogs(); // 可选：顺便刷新审计日志
    } else {
      showMessage(data.message, 'error');
    }
  } catch (e) {
    showMessage('网络错误', 'error');
  }
};

// ==============================
// 👇 原有代码（保持不动）
// ==============================
// --- 表格交互逻辑 ---
const isAllSelected = computed(() => {
  return posts.value.length > 0 && selectedPostIds.value.length === posts.value.length;
});

const toggleSelectAll = (e) => {
  if (e.target.checked) {
    selectedPostIds.value = posts.value.map(p => p.id);
  } else {
    selectedPostIds.value = [];
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return dateStr.replace('T', ' ').substring(0, 16);
};

// --- 核心特权操作逻辑 ---
const deletePost = async (id) => {
  const isConfirmed = await showConfirm(`高危操作：确认强制删除帖子 ID [${id}] 吗？该操作将被记入审计日志！`, '管理员操作确认');
  if (!isConfirmed) return;
  try {
    const res = await fetch(`${API_BASE}/admin/posts/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    const data = await res.json();
    if (data.success) {
      showMessage('删除成功！', 'success');
      fetchDashboardData();
    } else {
      showMessage(data.message, 'error');
    }
  } catch (e) {
    showMessage('网络错误', 'error');
  }
};

const batchDelete = async () => {
  if (selectedPostIds.value.length === 0) return;
  const isConfirmed = await showConfirm(`高危操作：确认批量删除选中的 ${selectedPostIds.value.length} 条帖子吗？`, '批量删除确认');
  if (!isConfirmed) return;
  try {
    const res = await fetch(`${API_BASE}/admin/posts/batch`, {
      method: 'DELETE',
      headers: getHeaders(),
      body: JSON.stringify({ ids: selectedPostIds.value })
    });
    const data = await res.json();
    if (data.success) {
      showMessage('批量删除成功！', 'success');
      fetchDashboardData();
    } else {
      showMessage(data.message, 'error');
    }
  } catch (e) {
    showMessage('网络错误', 'error');
  }
};
</script>

<style scoped>
.admin-container { padding-bottom: 40px; }

/* 顶部统计卡片 */
.stat-grid { display: flex; gap: 15px; margin-bottom: 15px; }
.stat-card { flex: 1; padding: 24px; display: flex; flex-direction: column; justify-content: center; }
.stat-title { font-size: 14px; color: #8590a6; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; font-family: Helvetica, Arial, sans-serif; }
.text-primary { color: #056de8; }
.text-success { color: #0084ff; }
.text-danger { color: #f1403c; }

/* 主体布局 */
.admin-layout { display: flex; gap: 15px; align-items: flex-start; }
.main-panel { flex: 1; min-width: 0; padding: 0; overflow: hidden; }
.side-panel { width: 320px; flex-shrink: 0; }

.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; }
.panel-title { font-size: 16px; font-weight: 600; color: #121212; }
.panel-actions { display: flex; gap: 10px; }

/* 知乎风数据表格 */
.table-container { width: 100%; overflow-x: auto; }
.zh-table { width: 100%; border-collapse: collapse; font-size: 14px; text-align: left; }
.zh-table th { background: #f8f9fa; color: #8590a6; font-weight: normal; padding: 12px 15px; border-top: 1px solid #ebebeb; border-bottom: 1px solid #ebebeb; }
.zh-table td { padding: 12px 15px; border-bottom: 1px solid #f0f2f7; color: #121212; vertical-align: middle; }
.zh-table tr:hover td { background: #fcfcfc; }
.zh-table tr.selected-row td { background: rgba(5,109,232,.03); }

.td-title { max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.text-gray { color: #8590a6; font-size: 13px; }
.action-link { cursor: pointer; transition: .2s; }
.action-link:hover { text-decoration: underline; }

/* 标签样式复用 */
.tag { padding: 2px 6px; border-radius: 2px; font-size: 12px; font-weight: normal; }
.tag-red { color: #f1403c; background: rgba(241,64,60,.1); }
.tag-green { color: #0084ff; background: rgba(0,132,255,.1); }
.tag-gray { color: #8590a6; background: #f6f6f6; border: 1px solid #ebebeb; }

/* 按钮红/蓝风格重写 */
.zh-btn-danger { background: #f1403c; color: #fff; }
.zh-btn-danger:hover:not(:disabled) { background: #d93a36; }
.zh-btn-danger:disabled { background: #f9c4c3; cursor: not-allowed; }

/* 日志流样式 */
.log-list { max-height: 600px; overflow-y: auto; padding: 0 20px 20px; }
.log-item { padding: 15px 0; border-bottom: 1px solid #f0f2f7; }
.log-time { font-size: 12px; color: #8590a6; margin-bottom: 6px; font-family: monospace; }
.log-action { margin-bottom: 8px; }
.log-detail { font-size: 13px; color: #444; line-height: 1.5; background: #f8f9fa; padding: 8px; border-radius: 4px; }
.empty-state { text-align: center; color: #8590a6; padding: 40px; }
</style>