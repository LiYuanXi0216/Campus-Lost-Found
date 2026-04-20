<template>
  <div class="profile-container">
    <div class="zh-card profile-header">
      <div class="avatar-wrapper" @click="triggerAvatarUpload">
        <img :src="user.avatar" class="avatar" />
        <div class="avatar-hover">更换头像</div>
      </div>
      <input type="file" ref="avatarInput" class="hidden" @change="uploadAvatar" accept="image/*" style="display: none;" />

      <div class="user-info">
        <h2>{{ user.nickname }}</h2>
        <p class="email">📩 {{ user.email || '未绑定邮箱' }}</p>
      </div>
    </div>

    <div class="profile-layout">
      <div class="zh-card profile-menu">
        <a :class="{ active: currentTab === 'posts' }" @click="currentTab = 'posts'">我的发布</a>
        <a :class="{ active: currentTab === 'subs' }" @click="currentTab = 'subs'">我的订阅</a>
        <a :class="{ active: currentTab === 'messages' }" @click="currentTab = 'messages'">消息中心 <span v-if="unreadCount > 0" class="red-dot"></span></a>
      </div>

      <div class="zh-card profile-content">
        <div v-if="currentTab === 'posts'">
          <h3 class="tab-title">我发布的帖子</h3>
          <div v-if="myPosts.length === 0" class="empty-state">暂无发布记录</div>
          <div v-for="post in myPosts" :key="post.id" class="list-item">
            <div class="item-main" style="cursor: pointer;" @click="showDetail(post)">
              <div class="item-title">
                <span :class="['tag', post.type === 'LOST' ? 'tag-red' : 'tag-green']">{{ post.type === 'LOST'?'寻物':'招领' }}</span>
                {{ post.title }}
              </div>
              <div class="item-meta">{{ post.createTime }} | 状态: {{ post.itemStatus === 'RESOLVED' ? '已解决' : '寻/招中' }}</div>
            </div>
            <div style="display:flex; gap:10px;">
              <button class="zh-btn zh-btn-outline" @click="openEditModal(post)">编辑</button>
              <button class="zh-btn zh-btn-outline" style="color:#f1403c; border-color:#f1403c;" @click="deletePost(post.id)">删除</button>
            </div>
          </div>
        </div>

        <div v-if="currentTab === 'subs'">
          <h3 class="tab-title">我的订阅规则</h3>
          <div v-if="mySubs.length === 0" class="empty-state">暂无订阅规则</div>
          <div v-for="sub in mySubs" :key="sub.id" class="list-item">
            <div class="item-main">
              <div class="item-title">关注关键词: {{ sub.keyword || '无' }}</div>
              <div class="item-meta">关注区域ID: {{ sub.buildingId || '全校范围' }}</div>
            </div>
            <button class="zh-btn zh-btn-outline" @click="deleteSub(sub.id)">取消订阅</button>
          </div>
        </div>

        <div v-if="currentTab === 'messages'">
          <h3 class="tab-title">消息通知</h3>
          <div v-if="myMessages.length === 0" class="empty-state">暂无消息</div>
          <div v-for="msg in myMessages" :key="msg.id" :class="['list-item', 'msg-item', { unread: !msg.isRead }]" @click="readMessage(msg)">
            <div class="item-main">
              <div class="item-title"><span v-if="!msg.isRead" class="red-dot"></span> {{ msg.title }}</div>
              <div class="item-desc">{{ msg.content }}</div>
              <div class="item-meta">{{ msg.createTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="zh-modal-mask">
      <div class="zh-modal" style="width: 500px; max-height: 90vh; overflow-y: auto;">
        <button class="zh-modal-close" @click="showDetailModal = false">&times;</button>
        <div class="zh-modal-header"><h2>{{ currentPost.title }}</h2></div>
        <div class="zh-modal-body">
          <div class="post-meta" style="margin-bottom: 15px;">
            <span :class="['tag', currentPost.type === 'LOST' ? 'tag-red' : 'tag-green']">{{ currentPost.type === 'LOST' ? '寻物' : '招领' }}</span>
            <span>📍 {{ currentPost.locationDesc || '未提供' }}</span>
            <span>🕒 {{ currentPost.incidentStartDate }}</span>
          </div>
          <img v-if="currentPost.imageUrl" :src="currentPost.imageUrl" style="width:100%; max-height:250px; object-fit:contain; background:#f6f6f6; border-radius:4px;" />
          <p style="margin: 15px 0; line-height: 1.6; color:#121212;">{{ currentPost.description }}</p>
          <div style="background: rgba(5,109,232,.05); padding: 10px; border-radius: 3px; color: #056de8;">
            <strong>联系方式：</strong>{{ currentPost.contact }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="zh-modal-mask">
      <div class="zh-modal" style="width: 500px; max-height: 90vh; overflow-y: auto;">
        <button class="zh-modal-close" @click="showEditModal = false">&times;</button>
        <div class="zh-modal-header"><h2>编辑帖子信息</h2></div>
        <div class="zh-modal-body">
          <select class="zh-input" v-model="postForm.itemStatus" style="font-weight:bold; color:#056de8;">
            <option value="PENDING">当前状态: 未解决 (寻/招中)</option>
            <option value="RESOLVED">当前状态: 已解决 (已找到/已归还)</option>
          </select>
          <select class="zh-input" v-model="postForm.type"><option value="LOST">寻物</option><option value="FOUND">招领</option></select>
          <input type="text" class="zh-input" v-model="postForm.title" placeholder="标题" />

          <div style="display:flex; gap:10px;">
            <input type="date" class="zh-input" v-model="postForm.incidentStartDate" />
            <input type="date" class="zh-input" v-model="postForm.incidentEndDate" />
          </div>
          <input type="text" class="zh-input" v-model="postForm.incidentTimeDesc" placeholder="模糊时间" />

          <select class="zh-input" v-model="postForm.buildingId">
            <option value="">-- 选择相关建筑 (可选) --</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">
              {{ b.name }} {{ b.aliases ? `(${b.aliases})` : '' }}
            </option>
          </select>
          <input type="text" class="zh-input" v-model="postForm.locationDesc" placeholder="具体位置" />

          <textarea class="zh-input" v-model="postForm.description" rows="3" placeholder="详细描述"></textarea>
          <input type="text" class="zh-input" v-model="postForm.contact" placeholder="联系方式" />

          <div style="margin-bottom:15px; font-size:13px; color:#8590a6;">
            更换图片 (不选则保留原图):
            <input type="file" @change="handleFileChange" accept="image/*" style="margin-top:5px; width:100%;" />
          </div>

          <button class="zh-btn zh-btn-primary zh-btn-block" @click="submitEdit" :disabled="isSubmitting">{{ isSubmitting ? '保存中...' : '保存修改' }}</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';

const emit = defineEmits(['update-unread']);
const API_BASE = 'http://localhost:8080/api';

const currentTab = ref('posts');
const user = ref(JSON.parse(localStorage.getItem('user')) || {});

const myPosts = ref([]);
const mySubs = ref([]);
const myMessages = ref([]);
const buildings = ref([]); // 需要加载建筑字典供编辑时使用

// 弹窗状态
const showDetailModal = ref(false);
const showEditModal = ref(false);
const isSubmitting = ref(false);
const currentPost = ref({});
const postForm = ref({});
const selectedFile = ref(null);
const avatarInput = ref(null);

const unreadCount = computed(() => myMessages.value.filter(m => !m.isRead).length);

const getHeaders = (isUpload = false) => {
  const h = { 'Authorization': localStorage.getItem('token') || '' };
  if (!isUpload) h['Content-Type'] = 'application/json';
  return h;
};

onMounted(() => {
  fetchBuildings();
  fetchPosts();
  fetchSubs();
  fetchMessages();
});

const fetchBuildings = async () => {
  try {
    const res = await fetch(`${API_BASE}/buildings`);
    buildings.value = await res.json();
  } catch (e) {}
};

// --- 头像上传逻辑 ---
const triggerAvatarUpload = () => { avatarInput.value.click(); };

const uploadAvatar = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const fd = new FormData(); fd.append('file', file);
  try {
    const uRes = await fetch(`${API_BASE}/files/upload`, { method: 'POST', headers: getHeaders(true), body: fd });
    const uData = await uRes.json();
    if (uData.success) {
      const res = await fetch(`${API_BASE}/users/avatar`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify({ avatar: uData.imageUrl }) });
      if (res.ok) {
        user.value.avatar = uData.imageUrl;
        localStorage.setItem('user', JSON.stringify(user.value)); // 同步本地缓存
        alert('头像更新成功！');
      }
    }
  } catch (err) { alert('头像上传失败'); }
};

// --- 帖子逻辑 ---
const fetchPosts = async () => {
  const res = await fetch(`${API_BASE}/posts/my`, { headers: getHeaders() });
  myPosts.value = await res.json();
};

const showDetail = (post) => {
  currentPost.value = post;
  showDetailModal.value = true;
};

const openEditModal = (post) => {
  selectedFile.value = null; // 清空暂存文件
  postForm.value = { ...post };
  showEditModal.value = true;
};

const handleFileChange = (e) => { if (e.target.files.length > 0) selectedFile.value = e.target.files[0]; };

const submitEdit = async () => {
  isSubmitting.value = true;
  try {
    // 如果选了新图片，先上传
    if (selectedFile.value) {
      const fd = new FormData(); fd.append('file', selectedFile.value);
      const uRes = await fetch(`${API_BASE}/files/upload`, { method: 'POST', headers: getHeaders(true), body: fd });
      const uData = await uRes.json();
      if (uData.success) { postForm.value.imageUrl = uData.imageUrl; if(uData.aiTags) postForm.value.description += '\n' + uData.aiTags; }
    }

    const payload = { ...postForm.value };
    if(!payload.buildingId) payload.buildingId = null;

    const res = await fetch(`${API_BASE}/posts/${postForm.value.id}`, {
      method: 'PUT', headers: getHeaders(), body: JSON.stringify(payload)
    });
    if (res.ok || (await res.json()).success) {
      alert('修改成功！');
      showEditModal.value = false;
      fetchPosts();
    }
  } catch (e) { alert('修改失败'); } finally { isSubmitting.value = false; }
};

const deletePost = async (id) => {
  if(!confirm('确定删除此贴？删除后不可恢复！')) return;
  await fetch(`${API_BASE}/posts/${id}`, { method: 'DELETE', headers: getHeaders() });
  fetchPosts();
};

// --- 订阅 & 消息逻辑 ---
const fetchSubs = async () => {
  const res = await fetch(`${API_BASE}/subscriptions/my`, { headers: getHeaders() });
  mySubs.value = await res.json();
};
const deleteSub = async (id) => {
  if(!confirm('确定取消该订阅？')) return;
  await fetch(`${API_BASE}/subscriptions/${id}`, { method: 'DELETE', headers: getHeaders() });
  fetchSubs();
};
const fetchMessages = async () => {
  const res = await fetch(`${API_BASE}/messages/my`, { headers: getHeaders() });
  const data = await res.json();
  if (data.success) { myMessages.value = data.data; emit('update-unread'); }
};
const readMessage = async (msg) => {
  if (msg.isRead) return;
  await fetch(`${API_BASE}/messages/${msg.id}/read`, { method: 'PUT', headers: getHeaders() });
  msg.isRead = true;
  emit('update-unread');
};
</script>

<style scoped>
.profile-header { display: flex; align-items: center; padding: 30px; }
.avatar-wrapper { position: relative; width: 100px; height: 100px; border-radius: 4px; overflow: hidden; margin-right: 24px; border: 1px solid #ebebeb; cursor: pointer; }
.avatar { width: 100%; height: 100%; object-fit: cover; }
.avatar-hover { position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.6); color: white; text-align: center; font-size: 12px; padding: 4px 0; opacity: 0; transition: 0.2s; }
.avatar-wrapper:hover .avatar-hover { opacity: 1; }

.user-info h2 { font-size: 26px; font-weight: 600; margin-bottom: 10px; }
.email { color: #8590a6; font-size: 14px; }

.profile-layout { display: flex; gap: 10px; margin-top: 10px; align-items: flex-start; }
.profile-menu { width: 250px; padding: 8px 0; }
.profile-menu a { display: block; padding: 0 20px; line-height: 48px; font-size: 15px; color: #444; cursor: pointer;}
.profile-menu a:hover { background: #f6f6f6; }
.profile-menu a.active { background: rgba(5,109,232,.05); color: #056de8; font-weight: 600; }

.profile-content { flex: 1; min-height: 400px; padding: 24px; }
.tab-title { font-size: 20px; font-weight: 600; border-bottom: 1px solid #f0f2f7; padding-bottom: 15px; margin-bottom: 15px; }

.empty-state { text-align: center; color: #8590a6; padding: 40px; }
.list-item { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-bottom: 1px solid #f0f2f7; }
.item-title { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.item-desc { font-size: 14px; color: #444; margin-bottom: 6px; }
.item-meta { font-size: 13px; color: #8590a6; }

.msg-item { cursor: pointer; transition: .2s; padding: 16px 10px; border-radius: 4px; }
.msg-item:hover { background: #fcfcfc; }
.msg-item.unread .item-title { color: #121212; }
.msg-item:not(.unread) .item-title, .msg-item:not(.unread) .item-desc { color: #8590a6; }

.tag { padding: 2px 6px; border-radius: 2px; font-size: 12px; font-weight: normal; margin-right: 6px; vertical-align: text-bottom; }
.tag-red { color: #f1403c; background: rgba(241,64,60,.1); }
.tag-green { color: #0084ff; background: rgba(0,132,255,.1); }
</style>