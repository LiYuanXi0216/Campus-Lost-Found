<template>
  <div class="container">
    <section v-if="activeTab === 'home'" class="section-view">
      <div class="header-row">
        <h2>📋 信息大厅</h2>
        <button class="btn btn-primary" @click="openPostModal()">+ 发布新信息</button>
      </div>

      <div class="search-panel">
        <div class="search-row">
          <input
            v-model="searchQuery.keyword"
            type="text"
            placeholder="输入多个关键词请用空格隔开 (如: 黑色 校园卡)"
            @keyup.enter="loadPosts"
          />
          <select v-model="searchQuery.type">
            <option value="ALL">全部类型</option>
            <option value="LOST">寻物启事</option>
            <option value="FOUND">招领启事</option>
          </select>
          <select v-model="searchQuery.buildingId">
            <option value="">所有区域</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
        </div>

        <div class="search-row compact">
          <span class="label-text">发生时间段：</span>
          <input v-model="searchQuery.startDate" type="date" class="date-input" />
          <span>至</span>
          <input v-model="searchQuery.endDate" type="date" class="date-input" />
          <button class="btn btn-primary search-btn" @click="loadPosts">🔍 精准搜索</button>
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
      <div class="profile-header" v-if="displayProfileUser">
        <img :src="displayProfileUser.avatar || defaultAvatar(displayProfileUser.username)" class="avatar" />
        <div>
          <h2>{{ displayProfileUser.nickname || displayProfileUser.username || '未登录用户' }}</h2>
          <p>{{ displayProfileUser.email || '未绑定邮箱' }}</p>
        </div>
      </div>

      <div class="stat-cards" v-if="displayProfileStats">
        <div class="stat-card">
          <h3>发布总数</h3>
          <h2 class="text-primary">{{ displayProfileStats.postCount }}</h2>
        </div>
        <div class="stat-card">
          <h3>我的评论</h3>
          <h2 class="text-primary">{{ displayProfileStats.commentCount }}</h2>
        </div>
        <div class="stat-card">
          <h3>我的订阅</h3>
          <h2 class="text-gray">{{ displayProfileStats.subscriptionCount }}</h2>
        </div>
      </div>

      <p v-if="profileErrorMessage" class="profile-error-text">{{ profileErrorMessage }}</p>

      <h3>📝 我发布的帖子</h3>
      <div class="post-list">
        <p v-if="myPosts.length === 0" class="empty-text">您还没有发布过帖子</p>
        <div v-for="post in myPosts" :key="post.id" class="post-card" @click="showDetail(post)">
          <img :src="post.imageUrl || defaultImg" class="post-img" />
          <div class="post-info">
            <h3>
              <span :class="['badge', post.type === 'LOST' ? 'bg-danger' : 'bg-success']">
                {{ post.type === 'LOST' ? '寻物' : '招领' }}
              </span>
              {{ post.title }}
            </h3>
            <div class="action-btns">
              <button class="btn btn-sm btn-primary" @click.stop="openPostModal(post)">编辑</button>
              <button class="btn btn-sm btn-danger" @click.stop="deletePost(post.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content post-modal">
        <span class="close-btn" @click="showModal = false">&times;</span>
        <h2>{{ postForm.id ? '修改帖子信息' : '发布新信息' }}</h2>

        <select v-model="postForm.type">
          <option value="LOST">寻物启事</option>
          <option value="FOUND">招领启事</option>
        </select>
        <input v-model="postForm.title" type="text" placeholder="标题" />

        <div class="flex-row">
          <input v-model="postForm.incidentStartDate" type="date" title="起始日期" />
          <span>至</span>
          <input v-model="postForm.incidentEndDate" type="date" title="结束日期" />
        </div>
        <input v-model="postForm.incidentTimeDesc" type="text" placeholder="模糊时间补充 (例：下午二节课后)" />

        <div class="flex-row">
          <select v-model="postForm.buildingId">
            <option value="">-- 选择附近建筑 (可选) --</option>
            <option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
          <button v-if="postForm.type === 'FOUND'" class="btn btn-success" @click="autoGetLocation">📍 自动获取GPS</button>
        </div>

        <input v-model="postForm.locationDesc" type="text" placeholder="具体位置说明 (例：二楼靠窗)" />
        <textarea v-model="postForm.description" rows="3" placeholder="详细描述物品特征..."></textarea>
        <input v-model="postForm.contact" type="text" placeholder="联系方式 (微信/手机号)" />
        <input type="file" accept="image/*" @change="handleFileChange" />

        <button class="btn btn-primary submit-btn" :disabled="isSubmitting" @click="submitPost">
          {{ isSubmitting ? '处理中...' : '🚀 保存提交' }}
        </button>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay">
      <div class="modal-content detail-modal">
        <span class="close-btn" @click="closeDetailModal">&times;</span>

        <div class="detail-header">
          <div class="detail-main">
            <h2>{{ currentPost.title }}</h2>
            <div class="detail-meta">
              <span :class="['badge', currentPost.type === 'LOST' ? 'bg-danger' : 'bg-success']">
                {{ currentPost.type === 'LOST' ? '寻物' : '招领' }}
              </span>
              <span>📅 {{ currentPost.incidentStartDate || '未提供' }} ~ {{ currentPost.incidentEndDate || currentPost.incidentStartDate || '未提供' }}</span>
              <span>📍 {{ currentPost.locationDesc || '未提供' }}</span>
            </div>
          </div>
        </div>

        <img
          v-if="currentPost.imageUrl"
          :src="currentPost.imageUrl"
          class="detail-image"
        />

        <div class="detail-grid">
          <div class="detail-card">
            <div class="detail-label">时间补充</div>
            <div>{{ currentPost.incidentTimeDesc || '无' }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">联系方式</div>
            <div class="contact-text">{{ currentPost.contact || '登录后查看/未留联系方式' }}</div>
          </div>
        </div>

        <div class="desc-box">{{ currentPost.description || '暂无详细描述' }}</div>

        <section class="comment-section">
          <div class="comment-section-header">
            <h3>评论区</h3>
            <div class="comment-sort-box">
              <span>排序方式</span>
              <select v-model="commentSortBy" @change="loadComments">
                <option value="latest">按发布时间</option>
                <option value="likes">按点赞量</option>
              </select>
            </div>
          </div>

          <div class="comment-editor">
            <textarea
              v-model="commentDraft.content"
              rows="3"
              placeholder="写下你的评论内容，支持文字和一张图片..."
            />

            <div class="comment-editor-actions">
              <div class="comment-upload-area">
                <input
                  id="main-comment-image-input"
                  type="file"
                  accept="image/*"
                  class="hidden-input"
                  @change="handleCommentImageChange($event, null)"
                />
                <button class="btn btn-light" @click="triggerCommentImageInput(null)">上传图片</button>
                <span class="hint-text">{{ commentDraft.fileName || '仅支持上传 1 张图片' }}</span>
              </div>
              <button class="btn btn-primary" :disabled="isCommentSubmitting" @click="submitComment()">
                {{ isCommentSubmitting ? '评论中...' : '发表评论' }}
              </button>
            </div>

            <div v-if="commentDraft.imageUrl" class="comment-image-preview">
              <img :src="commentDraft.imageUrl" alt="评论图片预览" />
              <button class="link-btn danger" @click="clearCommentDraftImage(null)">移除图片</button>
            </div>

            <p v-if="!props.isLoggedIn" class="comment-login-tip">登录后即可发表评论、点赞和回复。</p>
          </div>

          <div class="comment-list">
            <p v-if="!isCommentLoading && comments.length === 0" class="empty-text">这条帖子还没有评论，来抢个沙发吧。</p>
            <p v-if="isCommentLoading" class="empty-text">评论加载中...</p>

            <template v-for="comment in visibleComments" :key="comment.id">
              <article class="comment-card">
                <div class="comment-avatar-wrap">
                  <img :src="comment.publisherAvatar" class="comment-avatar" />
                </div>

                <div class="comment-body">
                  <div class="comment-topline">
                    <strong>{{ comment.publisherName }}</strong>
                    <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
                  </div>

                  <div class="comment-content">
                    <span v-if="comment.replyToUserName" class="reply-target">回复 {{ comment.replyToUserName }}：</span>
                    {{ comment.content || ' ' }}
                  </div>

                  <img
                    v-if="comment.imageUrl"
                    :src="comment.imageUrl"
                    class="comment-image"
                  />

                  <div class="comment-actions">
                    <button class="link-btn" @click="likeComment(comment.id)">👍 {{ comment.likeCount || 0 }}</button>
                    <button class="link-btn" @click="toggleReplyBox(comment)">回复</button>
                  </div>

                  <div v-if="activeReplyTargetId === comment.id" class="reply-editor">
                    <textarea
                      v-model="replyDrafts[comment.id].content"
                      rows="2"
                      :placeholder="`回复 ${comment.publisherName}...`"
                    />
                    <div class="comment-editor-actions">
                      <div class="comment-upload-area">
                        <input
                          :id="`reply-image-input-${comment.id}`"
                          type="file"
                          accept="image/*"
                          class="hidden-input"
                          @change="handleCommentImageChange($event, comment.id)"
                        />
                        <button class="btn btn-light" @click="triggerCommentImageInput(comment.id)">上传图片</button>
                        <span class="hint-text">{{ replyDrafts[comment.id].fileName || '仅支持上传 1 张图片' }}</span>
                      </div>
                      <button class="btn btn-primary btn-sm-action" @click="submitComment(comment)">
                        发送回复
                      </button>
                    </div>
                    <div v-if="replyDrafts[comment.id].imageUrl" class="comment-image-preview">
                      <img :src="replyDrafts[comment.id].imageUrl" alt="回复图片预览" />
                      <button class="link-btn danger" @click="clearCommentDraftImage(comment.id)">移除图片</button>
                    </div>
                  </div>

                  <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                    <article
                      v-for="reply in visibleReplies(comment)"
                      :key="reply.id"
                      class="reply-card"
                    >
                      <div class="comment-avatar-wrap">
                        <img :src="reply.publisherAvatar" class="comment-avatar small" />
                      </div>

                      <div class="comment-body">
                        <div class="comment-topline">
                          <strong>{{ reply.publisherName }}</strong>
                          <span class="comment-time">{{ formatDateTime(reply.createTime) }}</span>
                        </div>

                        <div class="comment-content">
                          <span v-if="reply.replyToUserName" class="reply-target">回复 {{ reply.replyToUserName }}：</span>
                          {{ reply.content || ' ' }}
                        </div>

                        <img
                          v-if="reply.imageUrl"
                          :src="reply.imageUrl"
                          class="comment-image"
                        />

                        <div class="comment-actions">
                          <button class="link-btn" @click="likeComment(reply.id)">👍 {{ reply.likeCount || 0 }}</button>
                          <button class="link-btn" @click="toggleReplyBox(reply)">回复</button>
                        </div>

                        <div v-if="activeReplyTargetId === reply.id" class="reply-editor">
                          <textarea
                            v-model="replyDrafts[reply.id].content"
                            rows="2"
                            :placeholder="`回复 ${reply.publisherName}...`"
                          />
                          <div class="comment-editor-actions">
                            <div class="comment-upload-area">
                              <input
                                :id="`reply-image-input-${reply.id}`"
                                type="file"
                                accept="image/*"
                                class="hidden-input"
                                @change="handleCommentImageChange($event, reply.id)"
                              />
                              <button class="btn btn-light" @click="triggerCommentImageInput(reply.id)">上传图片</button>
                              <span class="hint-text">{{ replyDrafts[reply.id].fileName || '仅支持上传 1 张图片' }}</span>
                            </div>
                            <button class="btn btn-primary btn-sm-action" @click="submitComment(reply)">
                              发送回复
                            </button>
                          </div>
                          <div v-if="replyDrafts[reply.id].imageUrl" class="comment-image-preview">
                            <img :src="replyDrafts[reply.id].imageUrl" alt="回复图片预览" />
                            <button class="link-btn danger" @click="clearCommentDraftImage(reply.id)">移除图片</button>
                          </div>
                        </div>
                      </div>
                    </article>

                    <button
                      v-if="comment.replies.length > defaultReplyVisibleCount"
                      class="expand-btn"
                      @click="toggleReplies(comment.id)"
                    >
                      {{ expandedReplies[comment.id] ? '收起回复' : `展开更多回复 (${comment.replies.length - defaultReplyVisibleCount})` }}
                    </button>
                  </div>
                </div>
              </article>
            </template>

            <button
              v-if="comments.length > defaultCommentVisibleCount"
              class="expand-btn"
              @click="showAllComments = !showAllComments"
            >
              {{ showAllComments ? '收起更多评论' : `展开更多评论 (${comments.length - defaultCommentVisibleCount})` }}
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';

const props = defineProps(['activeTab', 'isLoggedIn']);

const API_BASE = 'http://localhost:8080/api';
const defaultImg = 'data:image/gif;base64,R0lGODlhAQABAIAAAMLCwgAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw==';
const defaultCommentVisibleCount = 5;
const defaultReplyVisibleCount = 2;

const posts = ref([]);
const myPosts = ref([]);
const profileData = ref(null);
const buildings = ref([]);
const isMyPostsView = ref(false);

const searchQuery = ref({
  type: 'ALL',
  keyword: '',
  buildingId: '',
  startDate: '',
  endDate: ''
});

const showModal = ref(false);
const showDetailModal = ref(false);
const isSubmitting = ref(false);
const isCommentSubmitting = ref(false);
const isCommentLoading = ref(false);
const currentPost = ref({});
const selectedFile = ref(null);

const postForm = ref({
  id: null,
  type: 'LOST',
  title: '',
  description: '',
  contact: '',
  incidentStartDate: '',
  incidentEndDate: '',
  incidentTimeDesc: '',
  buildingId: '',
  locationDesc: '',
  latitude: null,
  longitude: null,
  imageUrl: ''
});

const comments = ref([]);
const commentSortBy = ref('latest');
const showAllComments = ref(false);
const expandedReplies = ref({});
const activeReplyTargetId = ref(null);
const replyDrafts = ref({});
const commentDraft = ref(createEmptyDraft());
const profileErrorMessage = ref('');
const authExpiredHandled = ref(false);

watch(() => postForm.value.type, (newType) => {
  if (newType === 'LOST') {
    postForm.value.latitude = null;
    postForm.value.longitude = null;
  }
});

watch(() => props.activeTab, (newTab) => {
  if (newTab === 'home') {
    isMyPostsView.value = false;
    loadPosts();
  }
  if (newTab === 'profile') {
    isMyPostsView.value = true;
    loadProfile();
    loadMyPosts();
  }
});

const visibleComments = computed(() => {
  return showAllComments.value ? comments.value : comments.value.slice(0, defaultCommentVisibleCount);
});

const displayProfileUser = computed(() => {
  if (profileData.value?.userInfo) return profileData.value.userInfo;
  const rawUser = localStorage.getItem('user');
  if (!rawUser) return null;
  try {
    return JSON.parse(rawUser);
  } catch (e) {
    return null;
  }
});

const displayProfileStats = computed(() => {
  if (profileData.value?.stats) return profileData.value.stats;
  return {
    postCount: myPosts.value.length,
    commentCount: 0,
    subscriptionCount: '开发中'
  };
});

onMounted(() => {
  loadBuildings();
  loadPosts();
});

function createEmptyDraft() {
  return {
    content: '',
    imageUrl: '',
    fileName: ''
  };
}

const getHeaders = (isUpload = false) => {
  const headers = { Authorization: localStorage.getItem('token') || '' };
  if (!isUpload) headers['Content-Type'] = 'application/json';
  return headers;
};

const defaultAvatar = (username) => {
  return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${username || 'guest'}`;
};

const handleUnauthorized = () => {
  // 避免同一轮多个接口同时 401 时连续弹出多个提示
  if (authExpiredHandled.value) return;
  authExpiredHandled.value = true;
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  window.dispatchEvent(new Event('auth-state-changed'));
  alert('登录状态已失效，请重新登录后再试');
};

const fetchJson = async (url, options = {}) => {
  const res = await fetch(url, options);

  if (res.status === 401) {
    handleUnauthorized();
    return { res, data: null, unauthorized: true };
  }

  let data = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    data = await res.json();
  }

  // 只要请求成功到达后端，就允许下一次再次触发 401 提示
  authExpiredHandled.value = false;
  return { res, data, unauthorized: false };
};

const loadBuildings = async () => {
  try {
    const res = await fetch(`${API_BASE}/buildings`);
    buildings.value = await res.json();
  } catch (e) {
    console.error('建筑物字典加载失败', e);
  }
};

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
    console.error('帖子搜索失败', e);
  }
};

const loadProfile = async () => {
  try {
    profileErrorMessage.value = '';
    const { data, unauthorized } = await fetchJson(`${API_BASE}/users/profile`, { headers: getHeaders() });
    if (unauthorized) {
      profileData.value = null;
      profileErrorMessage.value = '登录状态已失效，请重新登录。';
      return;
    }
    if (data?.success) {
      profileData.value = data.data;
    } else {
      profileData.value = null;
      profileErrorMessage.value = data?.message || '个人信息加载失败';
    }
  } catch (e) {
    profileData.value = null;
    profileErrorMessage.value = '个人信息加载失败，请稍后重试';
    console.error('个人中心加载失败', e);
  }
};

const loadMyPosts = async () => {
  try {
    const { data, unauthorized } = await fetchJson(`${API_BASE}/posts/my`, { headers: getHeaders() });
    if (unauthorized) {
      myPosts.value = [];
      return;
    }
    myPosts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    console.error('我的帖子加载失败', e);
  }
};

const showDetail = async (post) => {
  currentPost.value = post;
  showDetailModal.value = true;
  resetCommentUiState();
  await loadComments();
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  currentPost.value = {};
  resetCommentUiState();
};

const resetCommentUiState = () => {
  comments.value = [];
  commentSortBy.value = 'latest';
  showAllComments.value = false;
  expandedReplies.value = {};
  activeReplyTargetId.value = null;
  replyDrafts.value = {};
  commentDraft.value = createEmptyDraft();
};

const openPostModal = (post = null) => {
  if (!localStorage.getItem('token')) return alert('请先登录！');
  selectedFile.value = null;
  if (post) {
    postForm.value = { ...post };
  } else {
    postForm.value = {
      id: null,
      type: 'LOST',
      title: '',
      description: '',
      contact: '',
      incidentStartDate: '',
      incidentEndDate: '',
      incidentTimeDesc: '',
      buildingId: '',
      locationDesc: '',
      latitude: null,
      longitude: null,
      imageUrl: ''
    };
  }
  showModal.value = true;
};

const handleFileChange = (e) => {
  selectedFile.value = e.target.files.length > 0 ? e.target.files[0] : null;
};

const autoGetLocation = (event) => {
  if (!navigator.geolocation) return alert('浏览器不支持定位');
  const btn = event.target;
  const originalText = btn.innerText;
  btn.innerText = '⏳ 定位中...';

  navigator.geolocation.getCurrentPosition((pos) => {
    postForm.value.latitude = pos.coords.latitude;
    postForm.value.longitude = pos.coords.longitude;
    btn.innerText = '✅ 定位成功';
    alert('GPS 坐标已获取成功，您还可以继续补充详细地名。');
    setTimeout(() => {
      btn.innerText = originalText;
    }, 3000);
  }, () => {
    alert('定位失败，请检查浏览器定位权限');
    btn.innerText = originalText;
  });
};

const submitPost = async () => {
  isSubmitting.value = true;
  try {
    if (selectedFile.value) {
      const formData = new FormData();
      formData.append('file', selectedFile.value);
      const { data: uploadData, unauthorized } = await fetchJson(`${API_BASE}/files/upload`, {
        method: 'POST',
        headers: getHeaders(true),
        body: formData
      });
      if (unauthorized) return;
      if (uploadData.success) {
        postForm.value.imageUrl = uploadData.imageUrl;
        if (uploadData.aiTags) postForm.value.description += `\n${uploadData.aiTags}`;
      }
    }

    const method = postForm.value.id ? 'PUT' : 'POST';
    const url = postForm.value.id ? `${API_BASE}/posts/${postForm.value.id}` : `${API_BASE}/posts`;

    const payload = { ...postForm.value };
    if (!payload.buildingId) payload.buildingId = null;
    if (!payload.incidentStartDate) payload.incidentStartDate = null;
    if (!payload.incidentEndDate) payload.incidentEndDate = null;

    const { res, data, unauthorized } = await fetchJson(url, {
      method,
      headers: getHeaders(),
      body: JSON.stringify(payload)
    });
    if (unauthorized) return;
    const success = res.ok || !!data?.success;

    if (success) {
      alert('操作成功！');
      showModal.value = false;
      if (props.activeTab === 'home') loadPosts();
      if (props.activeTab === 'profile') {
        loadMyPosts();
        loadProfile();
      }
    } else {
      alert(data?.message || '操作失败');
    }
  } catch (e) {
    alert('网络请求错误');
  } finally {
    isSubmitting.value = false;
  }
};

const deletePost = async (id) => {
  if (!confirm('确认删除这条帖子吗？')) return;
  const { data, unauthorized } = await fetchJson(`${API_BASE}/posts/${id}`, {
    method: 'DELETE',
    headers: getHeaders()
  });
  if (unauthorized) return;
  if (data.success) {
    alert('删除成功');
    loadMyPosts();
    loadProfile();
    loadPosts();
  } else {
    alert(data.message || '删除失败');
  }
};

const loadComments = async () => {
  if (!currentPost.value.id) return;
  isCommentLoading.value = true;
  try {
    const { data } = await fetchJson(`${API_BASE}/comments/post/${currentPost.value.id}?sortBy=${commentSortBy.value}`);
    if (data.success) {
      comments.value = data.data || [];
    } else {
      comments.value = [];
      alert(data.message || '评论加载失败');
    }
  } catch (e) {
    comments.value = [];
    console.error('评论加载失败', e);
  } finally {
    isCommentLoading.value = false;
  }
};

const triggerCommentImageInput = (targetId) => {
  const inputId = targetId ? `reply-image-input-${targetId}` : 'main-comment-image-input';
  const input = document.getElementById(inputId);
  if (input) input.click();
};

const handleCommentImageChange = async (event, targetId) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  try {
    const formData = new FormData();
    formData.append('file', file);
    const { data: uploadData, unauthorized } = await fetchJson(`${API_BASE}/files/upload`, {
      method: 'POST',
      headers: getHeaders(true),
      body: formData
    });
    if (unauthorized) return;

    if (!uploadData.success) {
      return alert(uploadData.message || '图片上传失败');
    }

    const draft = getDraftByTargetId(targetId);
    draft.imageUrl = uploadData.imageUrl;
    draft.fileName = file.name;
  } catch (e) {
    alert('评论图片上传失败');
  } finally {
    event.target.value = '';
  }
};

const clearCommentDraftImage = (targetId) => {
  const draft = getDraftByTargetId(targetId);
  draft.imageUrl = '';
  draft.fileName = '';
};

const getDraftByTargetId = (targetId) => {
  if (!targetId) return commentDraft.value;
  ensureReplyDraft(targetId);
  return replyDrafts.value[targetId];
};

const ensureReplyDraft = (targetId) => {
  if (!replyDrafts.value[targetId]) {
    replyDrafts.value[targetId] = createEmptyDraft();
  }
};

const toggleReplyBox = (comment) => {
  if (!props.isLoggedIn) return alert('请先登录后再回复');
  ensureReplyDraft(comment.id);
  activeReplyTargetId.value = activeReplyTargetId.value === comment.id ? null : comment.id;
};

const submitComment = async (replyTarget = null) => {
  if (!props.isLoggedIn) return alert('请先登录后再评论');

  const draft = replyTarget ? getDraftByTargetId(replyTarget.id) : commentDraft.value;
  if (!draft.content.trim() && !draft.imageUrl) {
    return alert('评论内容和图片不能都为空');
  }

  isCommentSubmitting.value = true;
  try {
    const payload = {
      postId: currentPost.value.id,
      parentCommentId: replyTarget ? replyTarget.id : null,
      content: draft.content.trim(),
      imageUrl: draft.imageUrl || null
    };

    const { data, unauthorized } = await fetchJson(`${API_BASE}/comments`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(payload)
    });
    if (unauthorized) return;

    if (data.success) {
      if (replyTarget) {
        replyDrafts.value[replyTarget.id] = createEmptyDraft();
        activeReplyTargetId.value = null;
      } else {
        commentDraft.value = createEmptyDraft();
      }
      await loadComments();
      if (props.activeTab === 'profile') loadProfile();
    } else {
      alert(data.message || '评论失败');
    }
  } catch (e) {
    alert('评论提交失败');
  } finally {
    isCommentSubmitting.value = false;
  }
};

const likeComment = async (commentId) => {
  if (!props.isLoggedIn) return alert('请先登录后再点赞');
  try {
    const { data, unauthorized } = await fetchJson(`${API_BASE}/comments/${commentId}/like`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (unauthorized) return;
    if (data.success) {
      updateCommentLikeCount(comments.value, commentId, data.data.likeCount);
    } else {
      alert(data.message || '点赞失败');
    }
  } catch (e) {
    alert('点赞失败');
  }
};

const updateCommentLikeCount = (commentList, commentId, likeCount) => {
  for (const comment of commentList) {
    if (comment.id === commentId) {
      comment.likeCount = likeCount;
      return true;
    }
    if (comment.replies && comment.replies.length > 0) {
      const found = updateCommentLikeCount(comment.replies, commentId, likeCount);
      if (found) return true;
    }
  }
  return false;
};

const visibleReplies = (comment) => {
  if (!comment.replies) return [];
  return expandedReplies.value[comment.id] ? comment.replies : comment.replies.slice(0, defaultReplyVisibleCount);
};

const toggleReplies = (commentId) => {
  expandedReplies.value[commentId] = !expandedReplies.value[commentId];
};

const formatDateTime = (value) => {
  if (!value) return '刚刚';
  return value.replace('T', ' ').slice(0, 16);
};
</script>

<style scoped>
.container { max-width: 1000px; margin: 20px auto; padding: 0 15px; }
.section-view { background: white; padding: 25px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05); }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-panel { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
.search-row { display: flex; gap: 10px; margin-bottom: 10px; }
.search-row.compact { align-items: center; margin-bottom: 0; }
.search-row input, .search-row select { margin: 0; }
.search-row input[type='text'] { flex: 2; }
.search-row select { flex: 1; }
.date-input { width: auto; }
.label-text { font-size: 14px; color: #555; white-space: nowrap; }
.search-btn { margin-left: auto; padding: 8px 30px; }

.post-list { display: flex; flex-direction: column; gap: 15px; }
.post-card { display: flex; gap: 15px; padding: 15px; border: 1px solid #eee; border-radius: 8px; cursor: pointer; transition: 0.2s; }
.post-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.post-img { width: 100px; height: 100px; object-fit: cover; border-radius: 5px; }
.post-info { flex: 1; }
.meta-text { color: #666; font-size: 14px; margin: 5px 0; }
.action-btns { margin-top: 10px; }
.empty-text { color: #777; text-align: center; padding: 30px 0; }

.badge { display: inline-block; padding: 3px 8px; border-radius: 999px; color: white; font-size: 12px; margin-right: 6px; }
.bg-danger { background-color: #dc3545; }
.bg-success { background-color: #28a745; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-content { background: white; padding: 30px; border-radius: 12px; width: 90%; max-height: 88vh; overflow-y: auto; position: relative; }
.post-modal { max-width: 520px; }
.detail-modal { max-width: 860px; }
.close-btn { position: absolute; top: 15px; right: 20px; font-size: 24px; cursor: pointer; color: #888; }
.modal-content input, .modal-content select, .modal-content textarea { width: 100%; padding: 10px; margin-bottom: 12px; border: 1px solid #ddd; border-radius: 8px; box-sizing: border-box; font: inherit; }
.flex-row { display: flex; gap: 10px; align-items: center; margin-bottom: 12px; }
.flex-row input, .flex-row select { margin-bottom: 0; }

.detail-header h2 { margin: 0 0 12px 0; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 10px; color: #555; font-size: 14px; }
.detail-image { width: 100%; max-height: 320px; object-fit: cover; border-radius: 12px; margin: 18px 0; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.detail-card { background: #f8f9fa; border-radius: 10px; padding: 14px; }
.detail-label { font-size: 12px; color: #777; margin-bottom: 8px; }
.contact-text { color: #d93025; font-weight: bold; }
.desc-box { background: #f8f9fa; padding: 16px; border-radius: 10px; white-space: pre-wrap; font-size: 14px; line-height: 1.7; margin-bottom: 20px; }

.profile-header { display: flex; gap: 20px; align-items: center; margin-bottom: 30px; border-bottom: 1px solid #eee; padding-bottom: 20px; }
.avatar { width: 80px; height: 80px; border-radius: 50%; }
.stat-cards { display: flex; gap: 20px; margin-bottom: 30px; }
.stat-card { flex: 1; background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; }
.text-primary { color: #007bff; }
.text-gray { color: #aaa; }
.profile-error-text { color: #d93025; font-size: 14px; margin: -10px 0 18px; }

.comment-section { border-top: 1px solid #eee; padding-top: 24px; }
.comment-section-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.comment-section-header h3 { margin: 0; }
.comment-sort-box { display: flex; align-items: center; gap: 10px; color: #666; font-size: 14px; }
.comment-sort-box select { width: auto; margin: 0; }

.comment-editor { background: #f8f9fa; border-radius: 12px; padding: 16px; margin-bottom: 18px; }
.comment-editor textarea { margin-bottom: 12px; min-height: 90px; resize: vertical; }
.comment-editor-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.comment-upload-area { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hidden-input { display: none; }
.hint-text { color: #777; font-size: 13px; }
.comment-image-preview { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.comment-image-preview img { width: 96px; height: 96px; object-fit: cover; border-radius: 10px; border: 1px solid #eee; }
.comment-login-tip { margin: 12px 0 0 0; color: #666; font-size: 13px; }

.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-card, .reply-card { display: flex; gap: 12px; }
.reply-list { margin-top: 14px; padding-top: 10px; border-top: 1px dashed #e4e4e4; display: flex; flex-direction: column; gap: 12px; }
.reply-card { padding-left: 6px; }
.comment-avatar-wrap { flex-shrink: 0; }
.comment-avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; background: #eee; }
.comment-avatar.small { width: 34px; height: 34px; }
.comment-body { flex: 1; min-width: 0; }
.comment-topline { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 6px; }
.comment-time { color: #888; font-size: 12px; }
.comment-content { white-space: pre-wrap; word-break: break-word; line-height: 1.75; color: #222; }
.reply-target { color: #007bff; font-weight: 600; }
.comment-image { margin-top: 10px; max-width: 220px; width: 100%; border-radius: 12px; border: 1px solid #eee; }
.comment-actions { display: flex; gap: 14px; margin-top: 10px; }
.reply-editor { background: #fafafa; border: 1px solid #eee; border-radius: 10px; padding: 12px; margin-top: 12px; }
.reply-editor textarea { min-height: 72px; margin-bottom: 10px; resize: vertical; }

.expand-btn { align-self: flex-start; background: transparent; border: none; color: #007bff; cursor: pointer; padding: 0; font-size: 14px; }
.link-btn { background: transparent; border: none; color: #666; cursor: pointer; padding: 0; font-size: 14px; }
.link-btn:hover, .expand-btn:hover { color: #007bff; }
.link-btn.danger:hover { color: #dc3545; }

.btn { padding: 10px 15px; border: none; border-radius: 8px; cursor: pointer; color: white; font-weight: bold; }
.btn-sm { padding: 5px 10px; font-size: 12px; margin-right: 5px; }
.btn-sm-action { padding: 8px 12px; white-space: nowrap; }
.btn-primary { background-color: #007bff; }
.btn-danger { background-color: #dc3545; }
.btn-success { background-color: #28a745; }
.btn-light { background: #e9ecef; color: #333; }
.submit-btn { width: 100%; margin-top: 10px; font-size: 16px; }

@media (max-width: 768px) {
  .section-view { padding: 18px; }
  .header-row, .comment-section-header, .comment-editor-actions, .search-row, .detail-grid, .stat-cards { flex-direction: column; align-items: stretch; }
  .search-row input[type='text'], .search-row select, .date-input, .search-btn { width: 100%; }
  .detail-grid { display: flex; }
  .detail-modal { width: 94%; padding: 20px; }
  .post-card { flex-direction: column; }
  .post-img { width: 100%; height: 180px; }
}
</style>
