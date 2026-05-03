<template>
  <div class="simple-container">
    <div class="zh-card hero-card">
      <div class="hero-text">
        <h2>你好，{{ currentUser.nickname || '同学' }}</h2>
        <p>捡到或丢失了物品？点击右侧大按钮快速记录。</p>
      </div>
      <button class="big-post-btn" @click="openQuickPost">
        <span class="icon">📷</span> 快速拍照发布
      </button>
    </div>

    <div class="simple-list">
      <div v-if="posts.length === 0" class="empty-state">暂无物品信息</div>
      <div v-for="post in posts" :key="post.id" class="zh-card simple-item" @click="viewDetail(post)">
        <div class="item-badge" :class="post.type">{{ post.type === 'LOST' ? '寻物' : '招领' }}</div>

        <div class="simple-thumb-wrapper">
          <img v-if="post.imageUrl" :src="post.imageUrl" class="simple-thumb" />
          <div v-else class="simple-thumb-placeholder">无图片</div>
        </div>

        <div class="simple-info">
          <h3 class="simple-title">{{ post.title }}</h3>
          <p class="simple-meta">📍 {{ post.locationDesc || '自动定位区域' }}</p>
          <p class="simple-meta">🕒 {{ post.incidentStartDate }}</p>
        </div>
        <div class="simple-arrow">详情 〉</div>
      </div>
    </div>

    <div v-if="showModal" class="zh-modal-mask">
      <div class="zh-modal simple-post-modal">
        <div class="modal-header">
          <h2>📷 极简发布模式</h2>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">

          <div class="form-group">
            <label>1. 这是什么类型的物品？</label>
            <div class="simple-toggle">
              <button :class="{ active: postForm.type === 'FOUND' }" @click="postForm.type = 'FOUND'">我捡到了物品</button>
              <button :class="{ active: postForm.type === 'LOST' }" @click="postForm.type = 'LOST'">我丢失了物品</button>
            </div>
          </div>

          <div class="form-group">
            <label>2. 拍摄或上传照片 (必填)</label>
            <div class="upload-section" @click="triggerFile" :class="{'has-image': previewUrl}">
              <div v-if="!previewUrl" class="upload-placeholder">
                <div class="plus-icon">＋</div>
                <p>点击此处拍摄或选择照片</p>
              </div>
              <img v-else :src="previewUrl" class="upload-preview" />
            </div>
            <input type="file" ref="fileInput" hidden @change="handleFile" accept="image/*" capture="environment" />
            <p class="ai-hint">✨ 上传后，AI 将自动为您识别物品特征并填写描述</p>
          </div>

          <div class="form-group">
            <label>3. 当前位置</label>
            <div class="location-status" :class="{'loc-success': autoLocation.includes('成功'), 'loc-error': autoLocation.includes('失败')}">
              {{ autoLocation }}
            </div>
          </div>

          <button class="zh-btn zh-btn-primary submit-btn" :disabled="loading" @click="submitQuickPost">
            {{ loading ? '处理中，请稍候...' : '立即发布' }}
          </button>

          <p class="consecutive-hint">发布成功后可直接继续拍摄下一件物品</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue';

const showMessage = inject('showMessage');
const API_BASE = 'http://localhost:8080/api';
const currentUser = ref(JSON.parse(localStorage.getItem('user')) || {});

const posts = ref([]);
const showModal = ref(false);
const previewUrl = ref('');
const fileInput = ref(null);
const loading = ref(false);

const postForm = ref({ type: 'FOUND' });
const lat = ref(null);
const lng = ref(null);
const autoLocation = ref('📍 正在获取 GPS 定位...');

onMounted(() => {
  fetchPosts();
});

const fetchPosts = async () => {
  try {
    const res = await fetch(`${API_BASE}/posts/search?type=ALL`);
    posts.value = await res.json();
  } catch (e) {
    console.error("加载帖子失败");
  }
};

const triggerFile = () => fileInput.value.click();

const handleFile = (e) => {
  const file = e.target.files[0];
  if (file) {
    previewUrl.value = URL.createObjectURL(file);
  }
};

const openQuickPost = () => {
  showModal.value = true;
  resetForm();
  getLocation();
};

const closeModal = () => {
  showModal.value = false;
  fetchPosts(); // 关闭时刷新一次列表，看刚才发的所有帖子
};

// 重置表单以支持连发
const resetForm = () => {
  previewUrl.value = '';
  if (fileInput.value) fileInput.value.value = ''; // 清空文件输入框
  // 保留用户刚才选的 type，方便连发
};

// 自动获取定位
const getLocation = () => {
  autoLocation.value = '📍 正在获取当前精确位置...';
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
        (pos) => {
          lat.value = pos.coords.latitude;
          lng.value = pos.coords.longitude;
          autoLocation.value = '📍 定位成功！已获取精确坐标';
        },
        () => { autoLocation.value = '⚠️ 定位失败，将按全校区发布'; }
    );
  } else {
    autoLocation.value = '⚠️ 浏览器不支持定位';
  }
};

const submitQuickPost = async () => {
  if (!localStorage.getItem('token')) return showMessage('请先登录！', 'error');
  const file = fileInput.value?.files[0];
  if (!file) return showMessage('请先拍摄或上传一张照片', 'error');

  loading.value = true;
  try {
    // 1. 上传图片 (🐛 之前就是这里漏了 body: fd 导致全盘崩溃)
    const fd = new FormData();
    fd.append('file', file);
    const uRes = await fetch(`${API_BASE}/files/upload`, {
      method: 'POST',
      headers: { 'Authorization': localStorage.getItem('token') },
      body: fd  // 👈 核心修复：把图片传给后端！
    });
    const uData = await uRes.json();

    if (!uData.success) throw new Error("图片上传失败");

    // 2. 组装发帖数据
    const payload = {
      type: postForm.value.type,
      latitude: lat.value,
      longitude: lng.value,
      locationDesc: lat.value ? '已自动获取精确GPS' : '位置未知',
      imageUrl: uData.imageUrl,
      title: `[极简发布] ${uData.aiTags?.split(',')[0] || '物品'}`,
      description: `[AI 自动识别特征]: ${uData.aiTags || '暂无特征'}\n(由保洁/后勤人员快速记录，请失主尽快认领)`,
      contact: currentUser.value.email || '请在系统中私信联系',
      incidentStartDate: new Date().toISOString().split('T')[0]
    };

    const res = await fetch(`${API_BASE}/posts`, {
      method: 'POST',
      headers: {
        'Authorization': localStorage.getItem('token'),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (data.success) {
      showMessage('✅ 发布成功！您可以直接拍摄下一件物品', 'success');
      // 沉浸式连发：不关闭弹窗，只清空上一张照片
      resetForm();
      // 后台静默刷新列表，等用户关掉弹窗时就能看到了
      fetchPosts();
    } else {
      showMessage(data.message || '发布失败', 'error');
    }
  } catch (e) {
    showMessage('发布失败，请检查网络或重新上传', 'error');
  } finally {
    loading.value = false;
  }
};

const viewDetail = (post) => showMessage('请切换至标准模式查看帖子详情与评论', 'info');
</script>

<style scoped>
.simple-container { width: 100%; max-width: 700px; margin: 0 auto; padding-top: 10px; }

/* 顶部大横幅 */
.hero-card { padding: 30px; display: flex; justify-content: space-between; align-items: center; background: #fff; margin-bottom: 20px; border-radius: 4px; border-left: 6px solid #056de8; }
.hero-text h2 { font-size: 26px; margin-bottom: 8px; color: #121212; }
.hero-text p { color: #8590a6; font-size: 16px; margin: 0; }
.big-post-btn { background: #056de8; color: #fff; border: none; padding: 18px 30px; border-radius: 6px; font-size: 18px; font-weight: bold; cursor: pointer; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 12px rgba(5,109,232,0.3); transition: .2s; }
.big-post-btn:active { transform: translateY(2px); box-shadow: none; }

/* 列表样式优化 */
.empty-state { text-align: center; color: #8590a6; padding: 40px; background: #fff; border-radius: 4px; }
.simple-item { display: flex; padding: 15px; align-items: center; gap: 20px; position: relative; margin-bottom: 12px; cursor: pointer; border-radius: 4px; transition: .2s; }
.simple-item:hover { background: #fcfcfc; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.simple-thumb-wrapper { width: 100px; height: 100px; border-radius: 4px; overflow: hidden; background: #f6f6f6; flex-shrink: 0; }
.simple-thumb { width: 100%; height: 100%; object-fit: cover; }
.simple-thumb-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #ccc; font-size: 13px; }
.simple-info { flex: 1; min-width: 0; }
.simple-title { font-size: 20px; margin: 0 0 8px 0; color: #121212; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.simple-meta { font-size: 15px; color: #8590a6; margin: 0 0 4px 0; }
.simple-arrow { color: #8590a6; font-size: 14px; padding-right: 10px; }

/* 标签样式 */
.item-badge { position: absolute; top: 0; left: 0; padding: 4px 10px; font-size: 13px; font-weight: bold; color: #fff; border-radius: 4px 0 4px 0; }
.item-badge.FOUND { background: #0084ff; }
.item-badge.LOST { background: #f1403c; }

/* 弹窗样式对齐知乎风 */
.simple-post-modal { width: 90%; max-width: 480px; padding: 0; overflow: hidden; }
.modal-header { padding: 20px 24px; border-bottom: 1px solid #ebebeb; display: flex; justify-content: space-between; align-items: center; background: #fcfcfc; }
.modal-header h2 { margin: 0; font-size: 20px; color: #121212; }
.close-btn { background: none; border: none; font-size: 28px; color: #8590a6; cursor: pointer; line-height: 1; }
.modal-body { padding: 24px; }

.form-group { margin-bottom: 24px; }
.form-group label { display: block; font-weight: 600; margin-bottom: 12px; font-size: 16px; color: #121212; }

/* 按钮组 */
.simple-toggle { display: flex; gap: 12px; }
.simple-toggle button { flex: 1; padding: 14px; border: 1px solid #ebebeb; background: #f6f6f6; font-size: 16px; font-weight: 500; border-radius: 4px; color: #444; cursor: pointer; transition: .2s; }
.simple-toggle button.active { background: #056de8; color: #fff; border-color: #056de8; box-shadow: 0 2px 6px rgba(5,109,232,0.2); }

/* 上传框优化 */
.upload-section { width: 100%; height: 220px; background: #f8f9fa; border: 2px dashed #ccd0d7; border-radius: 6px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; transition: .2s; overflow: hidden; }
.upload-section:hover { border-color: #056de8; background: #f0f6ff; }
.upload-section.has-image { border: none; background: #000; }
.upload-placeholder { text-align: center; color: #8590a6; }
.plus-icon { font-size: 40px; margin-bottom: 10px; color: #b0b8c6; }
.upload-preview { width: 100%; height: 100%; object-fit: contain; }
.ai-hint { color: #056de8; font-size: 13px; margin-top: 8px; }

/* 定位状态 */
.location-status { padding: 12px 16px; background: #f6f6f6; border-radius: 4px; color: #444; font-size: 14px; display: flex; align-items: center; border: 1px solid #ebebeb; }
.loc-success { color: #0084ff; background: #f0f8ff; border-color: #b3d8ff; }
.loc-error { color: #f1403c; background: #fdf0f0; border-color: #fbdcdb; }

.submit-btn { width: 100%; padding: 16px; font-size: 18px; font-weight: bold; border-radius: 6px; margin-top: 10px; }
.consecutive-hint { text-align: center; color: #8590a6; font-size: 13px; margin-top: 16px; }
</style>