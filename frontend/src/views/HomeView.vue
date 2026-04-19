<template>
  <div class="container">
    <section v-if="activeTab === 'home'" class="section-view">
      <div class="header-row">
        <h2>📋 信息大厅</h2>
        <button class="btn btn-primary" @click="openPostModal()">+ 发布新信息</button>
      </div>
      <div class="search-panel" style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
        <div style="display: flex; gap: 10px; margin-bottom: 10px;">
          <input type="text" v-model="searchQuery.keyword" placeholder="输入多个关键词请用空格隔开 (如: 黑色 校园卡)" style="flex: 2; margin: 0;" @keyup.enter="loadPosts" />
          <select v-model="searchQuery.type" style="flex: 1; margin: 0;">
            <option value="ALL">全部类型</option>
            <option value="LOST">寻物启事</option>
            <option value="FOUND">招领启事</option>
          </select>
          <select v-model="searchQuery.buildingId" style="flex: 1; margin: 0;">
            <option value="">所有区域</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
        </div>
        <div style="display: flex; gap: 10px; align-items: center;">
          <span style="font-size: 14px; color: #555;">发生时间段：</span>
          <input type="date" v-model="searchQuery.startDate" style="width: auto; margin: 0;" />
          <span>至</span>
          <input type="date" v-model="searchQuery.endDate" style="width: auto; margin: 0;" />
          <button class="btn btn-primary" style="margin-left: auto; padding: 8px 30px;" @click="loadPosts">🔍 精准搜索</button>
        </div>
      </div>

      <div class="post-list">
        <p v-if="posts.length === 0" class="empty-text">暂无数据</p>
        <div v-for="post in posts" :key="post.id" class="post-card" @click="showDetail(post)">
          <img :src="post.imageUrl || defaultImg" class="post-img" />
          <div class="post-info">
            <h3>
              <span :class="['badge', post.type === 'LOST' ? 'bg-danger' : 'bg-success']">
                {{ post.type === 'LOST' ? '寻物' : '招领' }}
              </span>
              {{ post.title }}
            </h3>
            <p class="meta-text">
              📍 {{ post.locationDesc || '未填写地点' }} |
              ⏰ {{ post.incidentStartDate || '未知日期' }} {{ post.incidentTimeDesc || '' }}
            </p>
            <div v-if="isMyPostsView" class="action-btns">
              <button class="btn btn-sm btn-primary" @click.stop="openPostModal(post)">编辑</button>
              <button class="btn btn-sm btn-danger" @click.stop="deletePost(post.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'profile'" class="section-view">
      <div class="profile-header" v-if="profileData">
        <img :src="profileData.userInfo.avatar" class="avatar" />
        <div>
          <h2>{{ profileData.userInfo.nickname }}</h2>
          <p>{{ profileData.userInfo.email || '未绑定邮箱' }}</p>
        </div>
      </div>
      <div class="stat-cards" v-if="profileData">
        <div class="stat-card"><h3>发布总数</h3><h2 class="text-primary">{{ profileData.stats.postCount }}</h2></div>
        <div class="stat-card"><h3>我的评论</h3><h2 class="text-gray">开发中</h2></div>
        <div class="stat-card"><h3>我的订阅</h3><h2 class="text-gray">开发中</h2></div>
      </div>
      <h3>📝 我发布的帖子</h3>
      <div class="post-list">
        <p v-if="myPosts.length === 0" class="empty-text">您还没有发布过帖子</p>
        <div v-for="post in myPosts" :key="post.id" class="post-card" @click="showDetail(post)">
          <img :src="post.imageUrl || defaultImg" class="post-img" />
          <div class="post-info">
            <h3><span :class="['badge', post.type === 'LOST' ? 'bg-danger' : 'bg-success']">{{ post.type === 'LOST' ? '寻物' : '招领' }}</span> {{ post.title }}</h3>
            <div class="action-btns" style="margin-top:10px;">
              <button class="btn btn-sm btn-primary" @click.stop="openPostModal(post)">编辑</button>
              <button class="btn btn-sm btn-danger" @click.stop="deletePost(post.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <span class="close-btn" @click="showModal = false">&times;</span>
        <h2>{{ postForm.id ? '修改帖子信息' : '发布新信息' }}</h2>

        <select v-model="postForm.type"><option value="LOST">寻物启事</option><option value="FOUND">招领启事</option></select>
        <input type="text" v-model="postForm.title" placeholder="标题" />

        <div class="flex-row">
          <input type="date" v-model="postForm.incidentStartDate" title="起始日期" />
          <span>至</span>
          <input type="date" v-model="postForm.incidentEndDate" title="结束日期" />
        </div>
        <input type="text" v-model="postForm.incidentTimeDesc" placeholder="模糊时间补充 (例：下午二节课后)" />

        <div class="flex-row">
          <select v-model="postForm.buildingId">
            <option value="">-- 选择附近建筑 (可选) --</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">
              {{ b.name }} <span v-if="b.aliases">({{ b.aliases }})</span>
            </option>
          </select>
          <button v-if="postForm.type === 'FOUND'" class="btn btn-success" @click="autoGetLocation">📍 自动获取GPS</button>
        </div>
        <input type="text" v-model="postForm.locationDesc" placeholder="具体位置说明 (例：二楼靠窗)" />
        <textarea v-model="postForm.description" rows="3" placeholder="详细描述物品特征..."></textarea>
        <input type="text" v-model="postForm.contact" placeholder="联系方式 (微信/手机号)" />

        <input type="file" @change="handleFileChange" accept="image/*" />

        <button class="btn btn-primary submit-btn" @click="submitPost" :disabled="isSubmitting">
          {{ isSubmitting ? '处理中...' : '🚀 保存提交' }}
        </button>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay">
      <div class="modal-content">
        <span class="close-btn" @click="showDetailModal = false">&times;</span>
        <h2 style="margin-top: 0;">{{ currentPost.title }}</h2>
        <img v-if="currentPost.imageUrl" :src="currentPost.imageUrl" style="width: 100%; max-height: 250px; object-fit: cover; border-radius: 5px; margin-bottom: 15px;" />
        <p><strong>状态：</strong><span :class="['badge', currentPost.type === 'LOST' ? 'bg-danger' : 'bg-success']">{{ currentPost.type === 'LOST' ? '寻物' : '招领' }}</span></p>
        <p><strong>日期：</strong>{{ currentPost.incidentStartDate }} ~ {{ currentPost.incidentEndDate || currentPost.incidentStartDate }}</p>
        <p><strong>时间补充：</strong>{{ currentPost.incidentTimeDesc || '无' }}</p>
        <p><strong>详细地点：</strong>{{ currentPost.locationDesc || '无' }}</p>
        <p><strong>联系方式：</strong><span style="color:red;font-weight:bold;">{{ currentPost.contact || '登录后查看/未留联系方式' }}</span></p>
        <div class="desc-box">{{ currentPost.description }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';

// 接收 App.vue 传来的当前选中的 Tab
const props = defineProps(['activeTab']);

const API_BASE = 'http://localhost:8080/api';
const defaultImg = 'data:image/gif;base64,R0lGODlhAQABAIAAAMLCwgAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw==';

// 数据状态
const posts = ref([]);
const myPosts = ref([]);
const profileData = ref(null);
const buildings = ref([]); // 建筑物字典
const isMyPostsView = ref(false);

// 🚀 高级搜索状态
const searchQuery = ref({
  type: 'ALL', keyword: '', buildingId: '', startDate: '', endDate: ''
});

// 弹窗状态
const showModal = ref(false);
const showDetailModal = ref(false);
const isSubmitting = ref(false);
const currentPost = ref({});
const selectedFile = ref(null);

// 表单数据
const postForm = ref({
  id: null, type: 'LOST', title: '', description: '', contact: '',
  incidentStartDate: '', incidentEndDate: '', incidentTimeDesc: '',
  buildingId: '', locationDesc: '', latitude: null, longitude: null, imageUrl: ''
});

// 🐛 修复前端 Bug：监听表单类型的变化 (必须放在 postForm 定义之后！)
watch(() => postForm.value.type, (newType) => {
  if (newType === 'LOST') {
    // 切换到 LOST 时，清空暂存的 GPS 数据
    postForm.value.latitude = null;
    postForm.value.longitude = null;
    if (postForm.value.locationDesc && postForm.value.locationDesc.includes('已获取GPS坐标')) {
      postForm.value.locationDesc = '';
    }
  }
});

// 监听 Tab 切换
watch(() => props.activeTab, (newTab) => {
  if (newTab === 'home') { isMyPostsView.value = false; loadPosts(); }
  if (newTab === 'profile') { isMyPostsView.value = true; loadProfile(); loadMyPosts(); }
});

const getHeaders = (isUpload = false) => {
  const headers = { 'Authorization': localStorage.getItem('token') || '' };
  if (!isUpload) headers['Content-Type'] = 'application/json';
  return headers;
};

// 页面初始化
onMounted(() => {
  loadBuildings();
  loadPosts();
});

// 加载建筑物字典
const loadBuildings = async () => {
  try {
    const res = await fetch(`${API_BASE}/buildings`);
    buildings.value = await res.json();
  } catch (e) {
    console.error('建筑物字典加载失败');
  }
};

// 加载帖子 (带高级搜索参数)
const loadPosts = async () => {
  const params = new URLSearchParams();
  if (searchQuery.value.type !== 'ALL') params.append('type', searchQuery.value.type);
  if (searchQuery.value.keyword.trim()) params.append('keyword', searchQuery.value.keyword.trim());
  if (searchQuery.value.buildingId) params.append('buildingId', searchQuery.value.buildingId);
  if (searchQuery.value.startDate) params.append('startDate', searchQuery.value.startDate);
  if (searchQuery.value.endDate) params.append('endDate', searchQuery.value.endDate);

  try {
    const res = await fetch(`${API_BASE}/posts/search?${params.toString()}`);
    posts.value = await res.json();
  } catch (e) {
    console.error('搜索加载失败', e);
  }
};

const loadProfile = async () => {
  const res = await fetch(`${API_BASE}/users/profile`, { headers: getHeaders() });
  const data = await res.json();
  if (data.success) profileData.value = data.data;
};

const loadMyPosts = async () => {
  const res = await fetch(`${API_BASE}/posts/my`, { headers: getHeaders() });
  myPosts.value = await res.json();
};

const showDetail = (post) => {
  currentPost.value = post;
  showDetailModal.value = true;
};

const openPostModal = (post = null) => {
  if (!localStorage.getItem('token')) return alert('请先登录！');
  selectedFile.value = null;
  if (post) {
    postForm.value = { ...post };
  } else {
    postForm.value = { id: null, type: 'LOST', title: '', description: '', contact: '', incidentStartDate: '', incidentEndDate: '', incidentTimeDesc: '', buildingId: '', locationDesc: '', latitude: null, longitude: null, imageUrl: '' };
  }
  showModal.value = true;
};

const handleFileChange = (e) => {
  if (e.target.files.length > 0) selectedFile.value = e.target.files[0];
};

// 优化后的 GPS 定位
const autoGetLocation = (event) => {
  if (!navigator.geolocation) return alert('浏览器不支持定位');
  const btn = event.target;
  const originalText = btn.innerText;
  btn.innerText = '⏳ 定位中...';

  navigator.geolocation.getCurrentPosition(async (pos) => {
    postForm.value.latitude = pos.coords.latitude;
    postForm.value.longitude = pos.coords.longitude;
    btn.innerText = '✅ 定位成功';
    alert('📍 GPS 精确坐标已在后台获取！您可以继续手动输入详细地名。');
    setTimeout(() => { btn.innerText = originalText; }, 3000);
  }, (err) => {
    alert('定位失败，请允许权限');
    btn.innerText = originalText;
  });
};

const submitPost = async () => {
  isSubmitting.value = true;
  try {
    if (selectedFile.value) {
      const formData = new FormData();
      formData.append('file', selectedFile.value);
      const uploadRes = await fetch(`${API_BASE}/files/upload`, { method: 'POST', headers: getHeaders(true), body: formData });
      const uploadData = await uploadRes.json();
      if (uploadData.success) {
        postForm.value.imageUrl = uploadData.imageUrl;
        if(uploadData.aiTags) postForm.value.description += '\n' + uploadData.aiTags;
      }
    }

    const method = postForm.value.id ? 'PUT' : 'POST';
    const url = postForm.value.id ? `${API_BASE}/posts/${postForm.value.id}` : `${API_BASE}/posts`;

    const payload = { ...postForm.value };
    if(!payload.buildingId) payload.buildingId = null;
    if(!payload.incidentStartDate) payload.incidentStartDate = null;
    if(!payload.incidentEndDate) payload.incidentEndDate = null;

    const res = await fetch(url, { method, headers: getHeaders(), body: JSON.stringify(payload) });
    if (res.ok || (await res.json()).success) {
      alert('操作成功！');
      showModal.value = false;
      props.activeTab === 'home' ? loadPosts() : loadMyPosts();
    } else {
      alert('操作失败');
    }
  } catch (e) {
    alert('网络请求错误');
  } finally {
    isSubmitting.value = false;
  }
};

const deletePost = async (id) => {
  if (!confirm('确认删除？')) return;
  const res = await fetch(`${API_BASE}/posts/${id}`, { method: 'DELETE', headers: getHeaders() });
  if ((await res.json()).success) {
    alert('删除成功');
    loadMyPosts();
    loadProfile();
  }
};
</script>

<style scoped>
/* 局部样式，避免污染全局 */
.container { max-width: 1000px; margin: 20px auto; padding: 0 15px; }
.section-view { background: white; padding: 25px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 20px; }
.search-bar input { flex-grow: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px; }
.search-bar select { padding: 10px; border: 1px solid #ddd; border-radius: 5px; }

/* 卡片样式 */
.post-list { display: flex; flex-direction: column; gap: 15px; }
.post-card { display: flex; gap: 15px; padding: 15px; border: 1px solid #eee; border-radius: 8px; cursor: pointer; transition: 0.2s; }
.post-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.post-img { width: 100px; height: 100px; object-fit: cover; border-radius: 5px; }
.meta-text { color: #666; font-size: 14px; margin: 5px 0; }
.badge { padding: 3px 8px; border-radius: 4px; color: white; font-size: 12px; margin-right: 5px; }
.bg-danger { background-color: #dc3545; }
.bg-success { background-color: #28a745; }

/* 弹窗样式 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-content { background: white; padding: 30px; border-radius: 10px; width: 90%; max-width: 500px; max-height: 85vh; overflow-y: auto; position: relative; }
.close-btn { position: absolute; top: 15px; right: 20px; font-size: 24px; cursor: pointer; color: #888; }
.modal-content input, .modal-content select, .modal-content textarea { width: 100%; padding: 10px; margin-bottom: 12px; border: 1px solid #ddd; border-radius: 5px; box-sizing: border-box; }
.flex-row { display: flex; gap: 10px; align-items: center; margin-bottom: 12px; }
.flex-row input, .flex-row select { margin-bottom: 0; }
.desc-box { background: #f8f9fa; padding: 15px; border-radius: 5px; white-space: pre-wrap; font-size: 14px; line-height: 1.6; }

/* 个人中心 */
.profile-header { display: flex; gap: 20px; align-items: center; margin-bottom: 30px; border-bottom: 1px solid #eee; padding-bottom: 20px; }
.avatar { width: 80px; height: 80px; border-radius: 50%; }
.stat-cards { display: flex; gap: 20px; margin-bottom: 30px; }
.stat-card { flex: 1; background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; }
.text-primary { color: #007bff; }
.text-gray { color: #aaa; }

/* 通用按钮 */
.btn { padding: 10px 15px; border: none; border-radius: 5px; cursor: pointer; color: white; font-weight: bold; }
.btn-sm { padding: 5px 10px; font-size: 12px; margin-right: 5px; }
.btn-primary { background-color: #007bff; }
.btn-danger { background-color: #dc3545; }
.submit-btn { width: 100%; margin-top: 10px; font-size: 16px; }
</style>