<template>
  <div class="admin-container">

    <!-- 顶部统计卡片 -->
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

    <!-- 中间双列布局：左帖子 + 右私信 -->
    <div class="admin-layout">
      <!-- 左侧：帖子管理 -->
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

      <!-- 右侧：私信管理（折叠面板二级目录） -->
      <div class="zh-card main-panel">
        <div class="panel-header">
          <h2 class="panel-title">用户私信管理</h2>
          <div class="panel-actions">
            <button class="zh-btn zh-btn-outline" @click="fetchMessages">刷新私信</button>
          </div>
        </div>

        <div class="message-collapse">
          <div v-if="groupedMessages.length === 0" class="empty-state">暂无私信数据</div>

          <!-- 一级目录：对话组 -->
          <div class="collapse-item" v-for="(group, idx) in groupedMessages" :key="idx">
            <div class="collapse-header" @click="toggleCollapse(idx)">
              <span class="tag tag-gray">对话</span>
              <span style="margin:0 8px;">{{ group.user1 }} ↔ {{ group.user2 }}</span>
              <span style="color:#8590a6; font-size:12px;">共 {{ group.messages.length }} 条</span>
              <span style="flex:1; text-align:right;">{{ expandedIdx === idx ? '▼' : '▶' }}</span>
            </div>

            <!-- 二级目录：对话详情 -->
            <div class="collapse-body" v-show="expandedIdx === idx">
              <div class="message-item" v-for="msg in group.messages" :key="msg.id">
                <div class="msg-meta">
                  <span class="tag" :class="msg.senderId == group.user1 ? 'tag-green' : 'tag-red'">
                    {{ msg.senderId }}
                  </span>
                  <span style="margin-left:8px; font-size:12px; color:#8590a6;">
                    {{ formatDate(msg.sendTime) }}
                  </span>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
                <div class="msg-actions">
                  <a class="action-link text-danger" @click="deleteMessage(msg.id)">删除此条</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：安全审计日志（通栏） -->
    <div class="zh-card log-panel">
      <div class="panel-header">
        <h2 class="panel-title">安全审计日志</h2>
      </div>
      <div class="log-list">
        <div v-if="logs.length === 0" class="empty-state">暂无操作记录</div>
        <div v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-time">{{ formatDate(log.createTime) }}</div>
          <div class="log-action">
            <span class="tag tag-gray">管理员 ID: {{ log.adminId }}</span>
            <span class="text-danger" style="font-size:13px;font-weight:bold;margin-left:5px;">{{ log.actionType }}</span>
          </div>
          <div class="log-detail">{{ log.detail }}</div>
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

const stats = ref({});
const posts = ref([]);
const logs = ref([]);
const selectedPostIds = ref([]);
const messages = ref([]);

// 私信折叠面板
const expandedIdx = ref(null);
const groupedMessages = ref([]);

const getHeaders = () => ({
  'Authorization': localStorage.getItem('token') || '',
  'Content-Type': 'application/json'
});

onMounted(() => {
  fetchDashboardData();
  fetchMessages();
});

const fetchDashboardData = () => {
  fetchStats();
  fetchPosts();
  fetchLogs();
};

const fetchStats = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/dashboard/stats`, { headers: getHeaders() });
    const data = await res.json();
    if (data.success) stats.value = data.data;
  } catch (e) {}
};

const fetchPosts = async () => {
  try {
    const res = await fetch(`${API_BASE}/posts/search?type=ALL`, { headers: getHeaders() });
    posts.value = await res.json();
    selectedPostIds.value = [];
  } catch (e) {}
};

const fetchLogs = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/logs`, { headers: getHeaders() });
    const data = await res.json();
    if (data.success) logs.value = data.data;
  } catch (e) {}
};

// ======================
// 私信分组（按对话双方）
// ======================
const groupMessages = (list) => {
  let map = {};
  list.forEach(msg => {
    let u1 = msg.senderId;
    let u2 = msg.receiverId;
    let key = u1 < u2 ? `${u1}-${u2}` : `${u2}-${u1}`;
    if (!map[key]) map[key] = { user1: u1, user2: u2, messages: [] };
    map[key].messages.push(msg);
  });
  return Object.values(map);
};

const fetchMessages = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/messages`, { headers: getHeaders() });
    const data = await res.json();
    if (data.success) {
      messages.value = data.data;
      groupedMessages.value = groupMessages(data.data);
    }
  } catch (e) {}
};

const toggleCollapse = (idx) => {
  expandedIdx.value = expandedIdx.value === idx ? null : idx;
};

// ======================
// 删除私信
// ======================
const deleteMessage = async (msgId) => {
  const isConfirmed = await showConfirm(`确认删除这条私信吗？将通知发送者和接收者。`, '私信删除确认');
  if (!isConfirmed) return;

  try {
    const res = await fetch(`${API_BASE}/admin/messages/${msgId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    const data = await res.json();
    if (data.success) {
      showMessage(data.message, 'success');
      fetchMessages();
      fetchLogs();
    } else {
      showMessage(data.message, 'error');
    }
  } catch (e) {
    showMessage('网络错误', 'error');
  }
};

// ======================
// 帖子逻辑
// ======================
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
.admin-container {
  padding: 0 0 20px 0;
}

/* 顶部统计卡片 */
.stat-grid {
  display: flex;
  gap: 10px;
  margin: 0 auto 10px auto;
  width: 1000px;
  padding: 0 16px;
}
.stat-card {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-title {
  font-size: 13px;
  color: #8590a6;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
}
.text-primary { color: #056de8; }
.text-success { color: #0084ff; }
.text-danger { color: #f1403c; }

/* 中间双列布局 */
.admin-layout {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin: 0 auto 10px auto;
  width: 1000px;
  padding: 0 16px;
}
.main-panel {
  flex: 1;
  min-width: 0;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 面板内容容器 */
.table-container {
  height: 420px;
  overflow-y: auto;
  overflow-x: auto;
  padding: 0;
}
.message-collapse {
  height: 420px;
  overflow-y: auto;
  padding: 0;
}

/* 底部日志面板：关键！与上面两个面板同宽同边距 */
.log-panel {
  width: 1000px;
  margin: 0 auto;
  padding: 0 16px; /* 和上面的 admin-layout 保持同样的左右内边距 */
}
.log-list {
  height: 260px;
  overflow-y: auto;
  padding: 0; /* 去掉原来的额外内边距 */
}

/* 面板头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #f0f2f7;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #121212;
}
.panel-actions { display: flex; gap: 8px; }

/* 表格样式 */
.zh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  text-align: left;
}
.zh-table th {
  background: #f8f9fa;
  color: #8590a6;
  font-weight: normal;
  padding: 8px 12px;
  border-top: 1px solid #ebebeb;
  border-bottom: 1px solid #ebebeb;
  position: sticky;
  top: 0;
  z-index: 10;
}
.zh-table td {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f7;
  color: #121212;
  vertical-align: middle;
}
.zh-table tr:hover td { background: #fcfcfc; }
.zh-table tr.selected-row td { background: rgba(5,109,232,.03); }
.td-title {
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}
.text-gray { color: #8590a6; font-size: 12px; }
.action-link { cursor: pointer; transition: .2s; }
.action-link:hover { text-decoration: underline; }

/* 私信折叠面板 */
.collapse-item {
  margin: 8px;
  border: 1px solid #f0f2f7;
  border-radius: 6px;
  overflow: hidden;
}
.collapse-header {
  padding: 8px 12px;
  background: #f8f9fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-weight: 500;
  font-size: 13px;
}
.collapse-body {
  padding: 8px 12px;
}
.message-item {
  padding: 8px 0;
  border-bottom: 1px dashed #f0f2f7;
}
.message-item:last-child { border-bottom: none; }
.msg-meta {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.msg-content {
  padding: 6px 8px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 4px;
  line-height: 1.4;
}
.msg-actions { font-size: 12px; text-align: right; }

/* 日志项 */
.log-item {
  padding: 10px 12px; /* 和表格单元格内边距对齐 */
  border-bottom: 1px solid #f0f2f7;
}
.log-time {
  font-size: 11px;
  color: #8590a6;
  margin-bottom: 4px;
  font-family: monospace;
}
.log-action { margin-bottom: 6px; }
.log-detail {
  font-size: 12px;
  color: #444;
  line-height: 1.4;
  background: #f8f9fa;
  padding: 6px 8px;
  border-radius: 4px;
}
.empty-state {
  text-align: center;
  color: #8590a6;
  padding: 30px;
  font-size: 13px;
}

/* 标签 */
.tag {
  padding: 2px 5px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: normal;
}
.tag-red { color: #f1403c; background: rgba(241,64,60,.1); }
.tag-green { color: #0084ff; background: rgba(0,132,255,.1); }
.tag-gray { color: #8590a6; background: #f6f6f6; border: 1px solid #ebebeb; }

/* 按钮 */
.zh-btn-danger { background: #f1403c; color: #fff; }
.zh-btn-danger:hover:not(:disabled) { background: #d93a36; }
.zh-btn-danger:disabled { background: #f9c4c3; cursor: not-allowed; }
</style>