<template>
  <div class="home-container">
    <div class="main-content">

      <div class="zh-card search-card">
        <div class="search-row">
          <input type="text" class="zh-input" v-model="searchQuery.keyword" placeholder="搜索关键词 (如: 黑色 校园卡)" @keyup.enter="loadPosts" />
          <select class="zh-input" v-model="searchQuery.type">
            <option value="ALL">全部类型</option>
            <option value="LOST">寻物启事</option>
            <option value="FOUND">招领启事</option>
          </select>
          <select class="zh-input" v-model="searchQuery.buildingId">
            <option value="">所有区域</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">
              {{ b.name }} {{ b.aliases ? `(${b.aliases})` : '' }}
            </option>
          </select>
        </div>
        <div class="search-row align-center">
          <span class="label">时间段：</span>
          <input type="date" class="zh-input date-input" v-model="searchQuery.startDate" />
          <span class="label">-</span>
          <input type="date" class="zh-input date-input" v-model="searchQuery.endDate" />
          <div class="actions">
            <button class="zh-btn zh-btn-primary" @click="loadPosts">搜索</button>
            <button class="zh-btn zh-btn-outline" @click="subscribeSearch" title="当有符合条件的帖子时邮件通知我">🔔 订阅</button>
          </div>
        </div>
      </div>

      <div class="zh-card" style="padding: 0;">
        <div v-if="posts.length === 0" class="empty-state">暂无相关数据</div>
        <div v-for="post in posts" :key="post.id" class="post-item" @click="showDetail(post)">
          <div class="post-content">
            <h2 class="post-title">
              <span v-if="post.itemStatus === 'RESOLVED'" class="tag tag-gray">✅ 已解决</span>
              <span v-else :class="['tag', post.type === 'LOST' ? 'tag-red' : 'tag-green']">
                {{ post.type === 'LOST' ? '寻物' : '招领' }}
              </span>
              {{ post.title }}
            </h2>
            <p class="post-desc" :style="{ opacity: post.itemStatus === 'RESOLVED' ? 0.5 : 1 }">
              {{ post.description }}
            </p>
            <div class="post-meta">
              <span>📍 {{ getBuildingName(post.buildingId) }}</span>
              <span>🕒 {{ formatTimeRange(post.incidentStartDate, post.incidentEndDate, '') }}</span>
            </div>
          </div>
          <img v-if="post.imageUrl" :src="post.imageUrl" class="post-thumb" />
        </div>
      </div>
    </div>

    <div class="side-content">
      <div class="zh-card">
        <button class="zh-btn zh-btn-primary zh-btn-block" @click="openPostModal()">➕ 发布新信息</button>
        <p style="margin-top:15px; color:#8590a6; font-size:13px; line-height: 1.6;">校园失物招领中心<br>帮助同学找回遗失的美好。</p>
      </div>
    </div>

    <div v-if="showModal" class="zh-modal-mask">
      <div class="zh-modal" style="width: 500px; max-height: 90vh; overflow-y: auto;">
        <button class="zh-modal-close" @click="showModal = false">&times;</button>
        <div class="zh-modal-header"><h2>发布新信息</h2></div>
        <div class="zh-modal-body">
          <select class="zh-input" v-model="postForm.type"><option value="LOST">我丢了东西(寻物)</option><option value="FOUND">我捡到东西(招领)</option></select>
          <input type="text" class="zh-input" v-model="postForm.title" placeholder="一句话标题" />
          <div style="display:flex; gap:10px;">
            <input type="date" class="zh-input" v-model="postForm.incidentStartDate" />
            <input type="date" class="zh-input" v-model="postForm.incidentEndDate" />
          </div>
          <input type="text" class="zh-input" v-model="postForm.incidentTimeDesc" placeholder="模糊时间 (如: 下午三点左右)" />
          <div style="display:flex; gap:10px;">
            <select class="zh-input" v-model="postForm.buildingId">
              <option value="">-- 选择相关建筑 (可选) --</option>
              <option v-for="b in buildings" :key="b.id" :value="b.id">
                {{ b.name }} {{ b.aliases ? `(${b.aliases})` : '' }}
              </option>
            </select>
            <button v-if="postForm.type === 'FOUND' && !postForm.latitude" class="zh-btn zh-btn-outline" @click="autoGetLocation" style="white-space:nowrap; margin-bottom:15px;">📍 定位</button>
            <button v-if="postForm.type === 'FOUND' && postForm.latitude" class="zh-btn zh-btn-outline" @click="postForm.latitude=null; postForm.longitude=null; postForm.locationDesc=''" style="color:red; border-color:red; white-space:nowrap; margin-bottom:15px;">❌ 清除</button>
          </div>
          <input type="text" class="zh-input" v-model="postForm.locationDesc" placeholder="详细位置 (如: 二楼自习室靠窗)" />
          <textarea class="zh-input" v-model="postForm.description" rows="3" placeholder="物品详细特征描述..."></textarea>
          <input type="text" class="zh-input" v-model="postForm.contact" placeholder="联系方式 (必填)" />
          <input type="file" @change="handleFileChange" accept="image/*" style="margin-bottom:15px;" />
          <button class="zh-btn zh-btn-primary zh-btn-block" @click="submitPost" :disabled="isSubmitting">{{ isSubmitting ? '处理中...' : '提交' }}</button>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="zh-modal-mask">
      <div class="zh-modal" style="width: 600px; max-height: 90vh; overflow-y: auto; background: #f6f6f6;">
        <div class="zh-card" style="margin:0; border-radius: 2px 2px 0 0;">
          <button class="zh-modal-close" @click="showDetailModal = false">&times;</button>
          <h2>{{ currentPost.title }}</h2>
          <div class="post-meta" style="margin: 10px 0;">
            <span v-if="currentPost.itemStatus === 'RESOLVED'" class="tag tag-gray">✅ 已解决</span>
            <span v-else :class="['tag', currentPost.type === 'LOST' ? 'tag-red' : 'tag-green']">
              {{ currentPost.type === 'LOST' ? '寻物' : '招领' }}
            </span>

            <span>📍 {{ getBuildingName(currentPost.buildingId) }} {{ currentPost.locationDesc ? '- ' + currentPost.locationDesc : '' }}</span>

            <span>🕒 {{ formatTimeRange(currentPost.incidentStartDate, currentPost.incidentEndDate, currentPost.incidentTimeDesc) }}</span>
          </div>
          <img v-if="currentPost.imageUrl" :src="currentPost.imageUrl" style="width:100%; max-height:300px; object-fit:contain; background:#000;" />
          <p style="margin: 15px 0; line-height: 1.6;">{{ currentPost.description }}</p>
          <div style="background: rgba(5,109,232,.05); padding: 10px; border-radius: 3px; color: #056de8;">
            <strong>联系方式：</strong>{{ !isLoggedIn ? '需登录后查看' : (currentPost.contact || '未留联系方式') }}
          </div>
        </div>

        <div style="padding: 15px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
            <h3 style="font-size:15px; color:#121212;">✨ 智能匹配推荐</h3>
            <button class="zh-btn zh-btn-outline" style="padding:4px 10px; font-size:12px;" @click="getRecommendations">开始匹配</button>
          </div>
          <div v-if="recommendations.length > 0" class="recommend-list">
            <div v-for="rec in recommendations" :key="rec.id" class="zh-card recommend-item" @click="showDetail(rec)">
              <div style="font-weight:bold; font-size:14px; margin-bottom:5px;">{{ rec.title }}</div>
              <div class="post-meta">📍 {{ rec.locationDesc }}</div>
            </div>
          </div>
          <div v-else-if="searchedRecs" style="text-align:center; color:#8590a6; font-size:13px; padding:10px;">
            暂未找到相似的帖子
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';

const API_BASE = 'http://localhost:8080/api';
// 检查是否登录，用于控制联系方式显示
const isLoggedIn = ref(!!localStorage.getItem('token'));

const posts = ref([]);
const buildings = ref([]);
const recommendations = ref([]);
const searchedRecs = ref(false);

const searchQuery = ref({ type: 'ALL', keyword: '', buildingId: '', startDate: '', endDate: '' });

const showModal = ref(false);
const showDetailModal = ref(false);
const isSubmitting = ref(false);
const currentPost = ref({});
const selectedFile = ref(null);

const postForm = ref({ type: 'LOST', title: '', description: '', contact: '', incidentStartDate: '', incidentEndDate: '', incidentTimeDesc: '', buildingId: '', locationDesc: '', latitude: null, longitude: null, imageUrl: '' });

const getHeaders = (isUpload = false) => {
  const h = { 'Authorization': localStorage.getItem('token') || '' };
  if (!isUpload) h['Content-Type'] = 'application/json';
  return h;
};

onMounted(() => {
  loadBuildings();
  loadPosts();
});

const loadBuildings = async () => {
  try {
    const res = await fetch(`${API_BASE}/buildings`);
    buildings.value = await res.json();
  } catch (e) {}
};

const loadPosts = async () => {
  const params = new URLSearchParams();
  if (searchQuery.value.type !== 'ALL') params.append('type', searchQuery.value.type);
  if (searchQuery.value.keyword.trim()) params.append('keyword', searchQuery.value.keyword.trim());
  if (searchQuery.value.buildingId) params.append('buildingId', searchQuery.value.buildingId);
  if (searchQuery.value.startDate) params.append('startDate', searchQuery.value.startDate);
  if (searchQuery.value.endDate) params.append('endDate', searchQuery.value.endDate);
  const res = await fetch(`${API_BASE}/posts/search?${params.toString()}`);
  posts.value = await res.json();
};

const subscribeSearch = async () => {
  if (!isLoggedIn.value) return alert('请先登录！');
  if (!searchQuery.value.keyword && !searchQuery.value.buildingId) return alert('请至少填写关键词或选择区域！');
  const res = await fetch(`${API_BASE}/posts/subscribe`, {
    method: 'POST', headers: getHeaders(),
    body: JSON.stringify({ keyword: searchQuery.value.keyword, buildingId: searchQuery.value.buildingId || null })
  });
  alert((await res.json()).message);
};

const showDetail = (post) => {
  currentPost.value = post;
  recommendations.value = [];
  searchedRecs.value = false;
  showDetailModal.value = true;
};

const getRecommendations = async () => {
  searchedRecs.value = true;
  const res = await fetch(`${API_BASE}/posts/${currentPost.value.id}/recommendations`, { headers: getHeaders() });
  const data = await res.json();
  if (data.success) recommendations.value = data.data;
};

const openPostModal = () => {
  if (!isLoggedIn.value) return alert('请先登录！');
  postForm.value = { type: 'LOST', title: '', description: '', contact: '', incidentStartDate: '', incidentEndDate: '', incidentTimeDesc: '', buildingId: '', locationDesc: '', latitude: null, longitude: null, imageUrl: '' };
  showModal.value = true;
};

watch(() => postForm.value.type, (newType) => {
  if (newType === 'LOST') { postForm.value.latitude = null; postForm.value.longitude = null; }
});

const autoGetLocation = () => {
  if (!navigator.geolocation) return alert('不支持定位');
  navigator.geolocation.getCurrentPosition((pos) => {
    postForm.value.latitude = pos.coords.latitude;
    postForm.value.longitude = pos.coords.longitude;
    alert('📍 GPS获取成功！');
  }, () => alert('定位失败'));
};

// 🛠️ 辅助函数 1：根据 ID 查找建筑名称
const getBuildingName = (id) => {
  if (!id) return '未知区域';
  const building = buildings.value.find(b => b.id === id);
  return building ? building.name : '未知区域';
};

// 🛠️ 辅助函数 2：格式化时间范围
const formatTimeRange = (start, end, desc) => {
  if (!start) return '未知日期';
  let timeStr = start;
  // 如果有结束时间，且结束时间跟开始时间不一样，就拼接范围
  if (end && end !== start) {
    timeStr += ` 至 ${end}`;
  }
  // 如果有模糊时间描述，补在后面
  if (desc) {
    timeStr += ` ${desc}`;
  }
  return timeStr;
};

const handleFileChange = (e) => { if (e.target.files.length > 0) selectedFile.value = e.target.files[0]; };

const submitPost = async () => {
  isSubmitting.value = true;
  try {
    if (selectedFile.value) {
      const fd = new FormData(); fd.append('file', selectedFile.value);
      const uRes = await fetch(`${API_BASE}/files/upload`, { method: 'POST', headers: getHeaders(true), body: fd });
      const uData = await uRes.json();
      if (uData.success) { postForm.value.imageUrl = uData.imageUrl; if(uData.aiTags) postForm.value.description += '\n' + uData.aiTags; }
    }
    const payload = { ...postForm.value };
    if(!payload.buildingId) payload.buildingId = null;
    const res = await fetch(`${API_BASE}/posts`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(payload) });
    if (res.ok || (await res.json()).success) { alert('发布成功！'); showModal.value = false; loadPosts(); }
  } catch (e) { alert('请求失败'); } finally { isSubmitting.value = false; }
};
</script>

<style scoped>
.home-container { display: flex; gap: 10px; align-items: flex-start; }
.main-content { flex: 1; min-width: 0; }
.side-content { width: 296px; flex-shrink: 0; }
.search-card { padding: 15px; }
.search-row { display: flex; gap: 10px; margin-bottom: 10px; }
.search-row.align-center { align-items: center; margin-bottom: 0; }
.search-row .zh-input { margin-bottom: 0; }
.date-input { width: 140px; }
.label { font-size: 14px; color: #8590a6; white-space: nowrap; }
.actions { margin-left: auto; display: flex; gap: 10px; }
.empty-state { padding: 40px; text-align: center; color: #8590a6; }
.post-item { display: flex; padding: 16px 20px; border-bottom: 1px solid #f0f2f7; cursor: pointer; transition: .2s; }
.post-item:hover { background: #fcfcfc; }
.post-item:last-child { border-bottom: none; }
.post-content { flex: 1; }
.post-title { font-size: 18px; font-weight: 600; line-height: 1.6; margin-bottom: 6px; }
.post-desc { font-size: 15px; line-height: 1.6; color: #121212; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 10px; }
.post-meta { font-size: 14px; color: #8590a6; display: flex; gap: 15px; align-items: center; }
.post-thumb { width: 190px; height: 105px; object-fit: cover; border-radius: 4px; margin-left: 16px; }
.tag { padding: 2px 6px; border-radius: 2px; font-size: 12px; font-weight: normal; margin-right: 6px; vertical-align: text-bottom; }
.tag-red { color: #f1403c; background: rgba(241,64,60,.1); }
.tag-green { color: #0084ff; background: rgba(0,132,255,.1); }
.recommend-list { display: flex; flex-direction: column; gap: 10px; }
.recommend-item { padding: 12px; margin: 0; cursor: pointer; border: 1px solid transparent; }
.recommend-item:hover { border-color: #056de8; }
.tag-gray { color: #8590a6; background: #f6f6f6; border: 1px solid #ebebeb; }
</style>