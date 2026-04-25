<template>
  <div class="home-container">
    <div class="main-content">
      <div class="zh-card search-card">
        <div class="search-row">
          <input
            type="text"
            class="zh-input"
            v-model="searchQuery.keyword"
            placeholder="搜索关键词 (如: 黑色 校园卡)"
            @keyup.enter="loadPosts"
          />
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
            <button class="zh-btn zh-btn-outline" @click="subscribeSearch" title="当有符合条件的帖子时邮件通知我">
              🔔 订阅
            </button>
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
        <p class="side-hint">校园失物招领中心<br />帮助同学找回遗失的美好。</p>
      </div>
    </div>

    <div v-if="showModal" class="zh-modal-mask">
      <div class="zh-modal post-modal">
        <button class="zh-modal-close" @click="showModal = false">&times;</button>
        <div class="zh-modal-header"><h2>{{ postForm.id ? '编辑帖子' : '发布新信息' }}</h2></div>
        <div class="zh-modal-body">
          <select class="zh-input" v-model="postForm.type">
            <option value="LOST">我丢了东西(寻物)</option>
            <option value="FOUND">我捡到东西(招领)</option>
          </select>
          <input type="text" class="zh-input" v-model="postForm.title" placeholder="一句话标题" />
          <div class="inline-grid two-columns">
            <input type="date" class="zh-input" v-model="postForm.incidentStartDate" />
            <input type="date" class="zh-input" v-model="postForm.incidentEndDate" />
          </div>
          <input type="text" class="zh-input" v-model="postForm.incidentTimeDesc" placeholder="模糊时间 (如: 下午三点左右)" />
          <div class="inline-grid location-row">
            <select class="zh-input" v-model="postForm.buildingId">
              <option value="">-- 选择相关建筑 (可选) --</option>
              <option v-for="b in buildings" :key="b.id" :value="b.id">
                {{ b.name }} {{ b.aliases ? `(${b.aliases})` : '' }}
              </option>
            </select>
            <button
              v-if="postForm.type === 'FOUND' && !postForm.latitude"
              class="zh-btn zh-btn-outline"
              @click="autoGetLocation"
            >
              📍 定位
            </button>
            <button
              v-if="postForm.type === 'FOUND' && postForm.latitude"
              class="zh-btn clear-location-btn"
              @click="clearLocation"
            >
              ❌ 清除
            </button>
          </div>
          <input type="text" class="zh-input" v-model="postForm.locationDesc" placeholder="详细位置 (如: 二楼自习室靠窗)" />
          <textarea class="zh-input" v-model="postForm.description" rows="3" placeholder="物品详细特征描述..."></textarea>
          <input type="text" class="zh-input" v-model="postForm.contact" placeholder="联系方式 (必填)" />
          <input type="file" @change="handleFileChange" accept="image/*" class="file-input" />
          <button class="zh-btn zh-btn-primary zh-btn-block" @click="submitPost" :disabled="isSubmitting">
            {{ isSubmitting ? '处理中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="zh-modal-mask">
      <div class="zh-modal detail-modal">
        <div class="zh-card detail-card-shell">
          <button class="zh-modal-close" @click="closeDetailModal">&times;</button>
          <h2>{{ currentPost.title }}</h2>
          <div class="post-meta detail-meta">
            <span v-if="currentPost.itemStatus === 'RESOLVED'" class="tag tag-gray">✅ 已解决</span>
            <span v-else :class="['tag', currentPost.type === 'LOST' ? 'tag-red' : 'tag-green']">
              {{ currentPost.type === 'LOST' ? '寻物' : '招领' }}
            </span>
            <span>📍 {{ getBuildingName(currentPost.buildingId) }}{{ currentPost.locationDesc ? ` - ${currentPost.locationDesc}` : '' }}</span>
            <span>🕒 {{ formatTimeRange(currentPost.incidentStartDate, currentPost.incidentEndDate, currentPost.incidentTimeDesc) }}</span>
          </div>
          <img v-if="currentPost.imageUrl" :src="currentPost.imageUrl" class="detail-image" />
          <p class="detail-desc">{{ currentPost.description }}</p>
          <div class="contact-box">
            <strong>联系方式：</strong>{{ !isLoggedIn ? '需登录后查看' : (currentPost.contact || '未留联系方式') }}
          </div>
        </div>

        <div class="detail-section">
          <div class="section-head">
            <h3>✨ 智能匹配推荐</h3>
            <button class="zh-btn zh-btn-outline compact-btn" @click="getRecommendations">开始匹配</button>
          </div>
          <div v-if="recommendations.length > 0" class="recommend-list">
            <div v-for="rec in recommendations" :key="rec.id" class="zh-card recommend-item" @click="showDetail(rec)">
              <div class="recommend-title">{{ rec.title }}</div>
              <div class="post-meta">📍 {{ rec.locationDesc }}</div>
            </div>
          </div>
          <div v-else-if="searchedRecs" class="section-empty">暂未找到相似的帖子</div>
        </div>

        <div class="detail-section comment-section">
          <div class="section-head comment-section-header">
            <h3>评论区</h3>
            <div class="comment-sort-box">
              <span>排序方式</span>
              <select class="zh-input sort-select" v-model="commentSortBy" @change="loadComments">
                <option value="latest">按发布时间</option>
                <option value="likes">按点赞量</option>
              </select>
            </div>
          </div>

          <div class="comment-editor">
            <textarea
              class="zh-input"
              v-model="commentDraft.content"
              rows="3"
              placeholder="写下你的评论内容，支持文字和最多一张图片"
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
                <button class="zh-btn secondary-btn" @click="triggerCommentImageInput(null)">上传图片</button>
                <span class="hint-text">{{ commentDraft.fileName || '仅支持上传 1 张图片' }}</span>
              </div>
              <button class="zh-btn zh-btn-primary" :disabled="isCommentSubmitting" @click="submitComment()">
                {{ isCommentSubmitting ? '评论中...' : '发表评论' }}
              </button>
            </div>

            <div v-if="commentDraft.imageUrl" class="comment-image-preview">
              <img :src="commentDraft.imageUrl" alt="评论图片预览" />
              <button class="link-btn danger" @click="clearCommentDraftImage(null)">移除图片</button>
            </div>

            <p v-if="!isLoggedIn" class="comment-login-tip">登录后即可发表评论、点赞和回复。</p>
          </div>

          <div class="comment-list">
            <p v-if="isCommentLoading" class="empty-state small-empty">评论加载中...</p>
            <p v-else-if="comments.length === 0" class="empty-state small-empty">这条帖子还没有评论，来抢个沙发吧。</p>

            <template v-for="comment in visibleComments" :key="comment.id">
              <article class="comment-card">
                <div class="comment-avatar-wrap">
                  <img :src="comment.publisherAvatar || defaultAvatar(comment.publisherName)" class="comment-avatar" />
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

                  <img v-if="comment.imageUrl" :src="comment.imageUrl" class="comment-image" />

                  <div class="comment-actions">
                    <button
                      class="link-btn like-btn"
                      :class="{ liked: comment.likedByCurrentUser, popping: likeAnimations[comment.id] }"
                      @click="likeComment(comment.id)"
                    >
                      <img :src="comment.likedByCurrentUser ? likedIconUrl : likeIconUrl" alt="点赞" class="like-icon" />
                      <span>{{ comment.likeCount || 0 }}</span>
                    </button>
                    <button class="link-btn" @click="toggleReplyBox(comment)">回复</button>
                  </div>

                  <div v-if="activeReplyTargetId === comment.id" class="reply-editor">
                    <textarea
                      class="zh-input"
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
                        <button class="zh-btn secondary-btn" @click="triggerCommentImageInput(comment.id)">上传图片</button>
                        <span class="hint-text">{{ replyDrafts[comment.id].fileName || '仅支持上传 1 张图片' }}</span>
                      </div>
                      <button class="zh-btn zh-btn-primary compact-btn" @click="submitComment(comment)">发送回复</button>
                    </div>
                    <div v-if="replyDrafts[comment.id].imageUrl" class="comment-image-preview">
                      <img :src="replyDrafts[comment.id].imageUrl" alt="回复图片预览" />
                      <button class="link-btn danger" @click="clearCommentDraftImage(comment.id)">移除图片</button>
                    </div>
                  </div>

                  <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                    <article v-for="reply in visibleReplies(comment)" :key="reply.id" class="reply-card">
                      <div class="comment-avatar-wrap">
                        <img :src="reply.publisherAvatar || defaultAvatar(reply.publisherName)" class="comment-avatar small" />
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

                        <img v-if="reply.imageUrl" :src="reply.imageUrl" class="comment-image" />

                        <div class="comment-actions">
                          <button
                            class="link-btn like-btn"
                            :class="{ liked: reply.likedByCurrentUser, popping: likeAnimations[reply.id] }"
                            @click="likeComment(reply.id)"
                          >
                            <img :src="reply.likedByCurrentUser ? likedIconUrl : likeIconUrl" alt="点赞" class="like-icon" />
                            <span>{{ reply.likeCount || 0 }}</span>
                          </button>
                          <button class="link-btn" @click="toggleReplyBox(reply)">回复</button>
                        </div>

                        <div v-if="activeReplyTargetId === reply.id" class="reply-editor">
                          <textarea
                            class="zh-input"
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
                              <button class="zh-btn secondary-btn" @click="triggerCommentImageInput(reply.id)">上传图片</button>
                              <span class="hint-text">{{ replyDrafts[reply.id].fileName || '仅支持上传 1 张图片' }}</span>
                            </div>
                            <button class="zh-btn zh-btn-primary compact-btn" @click="submitComment(reply)">发送回复</button>
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';

const API_BASE = 'http://localhost:8080/api';
const defaultCommentVisibleCount = 5;
const defaultReplyVisibleCount = 2;
const likeIconUrl = new URL('../../resources/icons/like.svg', import.meta.url).href;
const likedIconUrl = new URL('../../resources/icons/liked.svg', import.meta.url).href;

// =========================
// 页面级状态
// =========================
// 这个 HomeView 现在承担两个职责：
// 1. 首页帖子大厅的搜索、列表展示、发帖弹窗
// 2. 帖子详情弹窗里的推荐区 + 评论区
//
// 所以下面的状态会同时服务于“列表区”和“详情弹窗区”。
// 读代码时可以先按这个分法理解：
// - posts / searchQuery / postForm      -> 首页大厅和发帖
// - currentPost / recommendations      -> 详情弹窗顶部
// - comments / replyDrafts / likeAnimations -> 评论区交互
const isLoggedIn = ref(!!localStorage.getItem('token'));
const posts = ref([]);
const buildings = ref([]);
const recommendations = ref([]);
const searchedRecs = ref(false);

// 搜索条件统一收敛到一个对象里，方便直接拼 URLSearchParams。
const searchQuery = ref({ type: 'ALL', keyword: '', buildingId: '', startDate: '', endDate: '' });

// 发帖弹窗与详情弹窗是两个互不影响的浮层状态。
const showModal = ref(false);
const showDetailModal = ref(false);
const isSubmitting = ref(false);
const isCommentSubmitting = ref(false);
const isCommentLoading = ref(false);
const currentPost = ref({});
const selectedFile = ref(null);

// 发帖表单：
// - 新建帖子时，id = null
// - 编辑帖子时，会把已有帖子对象浅拷贝进来
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

// 评论区状态：
// - comments: 当前帖子下的评论树
// - commentSortBy: 前端切换“按时间 / 按点赞”后会重新请求接口
// - showAllComments / expandedReplies: 控制折叠展开
// - activeReplyTargetId: 当前打开的是哪一条回复输入框
// - replyDrafts / commentDraft: 主评论框与各回复框各自维护独立草稿
const comments = ref([]);
const commentSortBy = ref('latest');
const showAllComments = ref(false);
const expandedReplies = ref({});
const activeReplyTargetId = ref(null);
const replyDrafts = ref({});
const commentDraft = ref(createEmptyDraft());
const likeAnimations = ref({});
const authExpiredHandled = ref(false);

// 评论默认只展示一部分，避免长帖详情弹窗初次打开时信息过载。
// 这里故意只对“顶级评论列表”做折叠；
// 单条顶级评论下面的回复则由 visibleReplies 单独控制。
const visibleComments = computed(() => {
  return showAllComments.value ? comments.value : comments.value.slice(0, defaultCommentVisibleCount);
});

watch(
  () => postForm.value.type,
  (newType) => {
    if (newType === 'LOST') {
      postForm.value.latitude = null;
      postForm.value.longitude = null;
    }
  }
);

onMounted(() => {
  loadBuildings();
  loadPosts();
  // 登录、退出登录都可能由外层页面触发，这里监听广播后同步本组件展示态。
  window.addEventListener('storage', syncLoginState);
  window.addEventListener('auth-state-changed', syncLoginState);
});

function createEmptyDraft() {
  // 主评论框和回复框都复用同一种草稿结构：
  // - content  文字内容
  // - imageUrl 已上传成功的图片地址
  // - fileName 仅用于前端展示“你选了哪张图片”
  return { content: '', imageUrl: '', fileName: '' };
}

function syncLoginState() {
  // 这里只同步“当前是否登录”这个最小状态，
  // 具体用户信息仍由外层页面负责维护。
  isLoggedIn.value = !!localStorage.getItem('token');
}

const getHeaders = (isUpload = false) => {
  // 上传文件时不能手动指定 application/json，
  // 让浏览器自己给 FormData 补 multipart boundary 才能正常解析。
  const headers = { Authorization: localStorage.getItem('token') || '' };
  if (!isUpload) headers['Content-Type'] = 'application/json';
  return headers;
};

const defaultAvatar = (username) => {
  return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${username || 'guest'}`;
};

const handleUnauthorized = () => {
  // 多个接口并发 401 时只提示一次，避免评论区和详情区同时弹窗打断操作。
  if (authExpiredHandled.value) return;
  authExpiredHandled.value = true;
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  syncLoginState();
  window.dispatchEvent(new Event('auth-state-changed'));
  alert('登录状态已失效，请重新登录后再试');
};

const fetchJson = async (url, options = {}) => {
  // 统一处理 JSON 解析和 401 失效逻辑，评论、点赞、发帖都走这一层。
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

  // 只要这次请求不是 401，就允许下一次 401 再次触发提示。
  authExpiredHandled.value = false;
  return { res, data, unauthorized: false };
};

const loadBuildings = async () => {
  // 建筑字典用于：
  // 1. 搜索条件里的区域选择
  // 2. 发帖时选择建筑
  // 3. 帖子列表 / 详情把 buildingId 翻译成人类可读名称
  try {
    const res = await fetch(`${API_BASE}/buildings`);
    buildings.value = await res.json();
  } catch (e) {
    console.error('建筑物字典加载失败', e);
  }
};

const loadPosts = async () => {
  // 搜索参数全部走可选拼接，空字段不传给后端。
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

const subscribeSearch = async () => {
  if (!isLoggedIn.value) return alert('请先登录！');
  if (!searchQuery.value.keyword && !searchQuery.value.buildingId) return alert('请至少填写关键词或选择区域！');

  // 订阅规则沿用当前搜索条件，所以用户在筛选后可以直接一键订阅。
  const { data, unauthorized } = await fetchJson(`${API_BASE}/posts/subscribe`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ keyword: searchQuery.value.keyword, buildingId: searchQuery.value.buildingId || null })
  });
  if (unauthorized) return;
  alert(data?.message || '订阅成功');
};

const showDetail = async (post) => {
  // 每次切换到新的帖子详情时，先清空上一条帖子留下的评论交互状态。
  // 比如：
  // - 上一条帖子的回复输入框正在打开
  // - 上一条帖子里已经上传了一张评论图片
  // - 上一条帖子切到了“按点赞量排序”
  // 这些状态都不应该串到下一条帖子里。
  currentPost.value = post;
  recommendations.value = [];
  searchedRecs.value = false;
  showDetailModal.value = true;
  resetCommentUiState();
  await loadComments();
};

const closeDetailModal = () => {
  // 关闭详情时把与当前帖子强绑定的状态一起释放掉，避免下次打开出现旧数据闪烁。
  showDetailModal.value = false;
  currentPost.value = {};
  recommendations.value = [];
  searchedRecs.value = false;
  resetCommentUiState();
};

const resetCommentUiState = () => {
  // 这里同时重置评论排序、展开状态、主评论草稿和所有回复草稿。
  comments.value = [];
  commentSortBy.value = 'latest';
  showAllComments.value = false;
  expandedReplies.value = {};
  activeReplyTargetId.value = null;
  replyDrafts.value = {};
  commentDraft.value = createEmptyDraft();
};

const getRecommendations = async () => {
  if (!currentPost.value.id) return;
  searchedRecs.value = true;
  try {
    // 推荐区不参与主流程，只影响详情弹窗下半部分，所以失败时只清空推荐区即可。
    const { data } = await fetchJson(`${API_BASE}/posts/${currentPost.value.id}/recommendations`, { headers: getHeaders() });
    if (data?.success) recommendations.value = data.data;
    else recommendations.value = [];
  } catch (e) {
    recommendations.value = [];
    console.error('智能匹配失败', e);
  }
};

const openPostModal = (post = null) => {
  if (!isLoggedIn.value) return alert('请先登录！');
  selectedFile.value = null;
  // 传入 post 代表编辑，否则按新建帖子初始化表单。
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

const clearLocation = () => {
  // 只清理自动定位结果，不碰 type / title / description 等其他表单字段。
  postForm.value.latitude = null;
  postForm.value.longitude = null;
  postForm.value.locationDesc = '';
};

const handleFileChange = (e) => {
  // 这里只暂存文件对象；真正上传发生在 submitPost 里。
  selectedFile.value = e.target.files.length > 0 ? e.target.files[0] : null;
};

const autoGetLocation = (event) => {
  // 这是一个纯前端交互增强：
  // - 定位成功后把经纬度填进表单
  // - 位置文字说明仍然允许用户手动补充
  if (!navigator.geolocation) return alert('浏览器不支持定位');
  const btn = event?.target;
  const originalText = btn?.innerText;
  if (btn) btn.innerText = '⏳ 定位中...';

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      postForm.value.latitude = pos.coords.latitude;
      postForm.value.longitude = pos.coords.longitude;
      if (btn) btn.innerText = '✅ 定位成功';
      alert('GPS 坐标已获取成功，您还可以继续补充详细地名。');
      if (btn) {
        setTimeout(() => {
          btn.innerText = originalText;
        }, 3000);
      }
    },
    () => {
      alert('定位失败，请检查浏览器定位权限');
      if (btn) btn.innerText = originalText;
    }
  );
};

const submitPost = async () => {
  isSubmitting.value = true;
  try {
  // 先上传图片，再把返回的 URL 拼进帖子 payload，保持后端接口简单。
    if (selectedFile.value) {
      const formData = new FormData();
      formData.append('file', selectedFile.value);
      const { data: uploadData, unauthorized } = await fetchJson(`${API_BASE}/files/upload`, {
        method: 'POST',
        headers: getHeaders(true),
        body: formData
      });
      if (unauthorized) return;
      if (uploadData?.success) {
        postForm.value.imageUrl = uploadData.imageUrl;
        if (uploadData.aiTags) postForm.value.description += `\n${uploadData.aiTags}`;
      }
    }

    const method = postForm.value.id ? 'PUT' : 'POST';
    const url = postForm.value.id ? `${API_BASE}/posts/${postForm.value.id}` : `${API_BASE}/posts`;
    const payload = { ...postForm.value };

    // 这些字段允许为空；如果保留空字符串，后端和数据库层处理起来反而更麻烦。
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
      loadPosts();
    } else {
      alert(data?.message || '操作失败');
    }
  } catch (e) {
    alert('网络请求错误');
  } finally {
    isSubmitting.value = false;
  }
};

const loadComments = async () => {
  if (!currentPost.value.id) return;
  isCommentLoading.value = true;
  try {
  // 评论列表允许游客查看，所以这里只在本地有 token 时附带 Authorization。
    const headers = {};
    const token = localStorage.getItem('token');
    if (token) headers.Authorization = token;

    const { data } = await fetchJson(`${API_BASE}/comments/post/${currentPost.value.id}?sortBy=${commentSortBy.value}`, {
      headers
    });

    // 后端已经返回了“树形评论 + 当前用户点赞状态”，
    // 前端这里只负责接收，不再自行重组结构。
    if (data?.success) {
      comments.value = data.data || [];
    } else {
      comments.value = [];
      if (data?.message) alert(data.message);
    }
  } catch (e) {
    comments.value = [];
    console.error('评论加载失败', e);
  } finally {
    isCommentLoading.value = false;
  }
};

const triggerCommentImageInput = (targetId) => {
  // 页面里存在多个隐藏 file input：
  // - 一个属于主评论框
  // - 若干个属于各条回复框
  // 所以这里通过 targetId 拼接 DOM id，定向触发对应输入框。
  const inputId = targetId ? `reply-image-input-${targetId}` : 'main-comment-image-input';
  const input = document.getElementById(inputId);
  if (input) input.click();
};

const getDraftByTargetId = (targetId) => {
  // `null` 代表主评论框；其余 id 代表某条评论/回复对应的独立回复草稿。
  if (!targetId) return commentDraft.value;
  ensureReplyDraft(targetId);
  return replyDrafts.value[targetId];
};

const ensureReplyDraft = (targetId) => {
  // 只有当用户真的展开某个回复框时，才给这条评论初始化回复草稿。
  // 这样可以避免一进页面就为所有评论都创建空对象。
  if (!replyDrafts.value[targetId]) {
    replyDrafts.value[targetId] = createEmptyDraft();
  }
};

const handleCommentImageChange = async (event, targetId) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  try {
  // 评论图片与帖子图片共用上传接口，上传完成后只回填到对应草稿，不直接发评论。
    const formData = new FormData();
    formData.append('file', file);
    const { data: uploadData, unauthorized } = await fetchJson(`${API_BASE}/files/upload`, {
      method: 'POST',
      headers: getHeaders(true),
      body: formData
    });
    if (unauthorized) return;
    if (!uploadData?.success) return alert(uploadData?.message || '图片上传失败');

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
  // 删除草稿中的图片，只影响当前输入框，不影响已发出的评论。
  const draft = getDraftByTargetId(targetId);
  draft.imageUrl = '';
  draft.fileName = '';
};

const toggleReplyBox = (comment) => {
  if (!isLoggedIn.value) return alert('请先登录后再回复');
  // 同一时间只保留一个展开的回复框，避免长评论串里出现多个输入框。
  ensureReplyDraft(comment.id);
  activeReplyTargetId.value = activeReplyTargetId.value === comment.id ? null : comment.id;
};

const submitComment = async (replyTarget = null) => {
  if (!isLoggedIn.value) return alert('请先登录后再评论');

  // 主评论和回复共用一个提交流程，只通过 parentCommentId 区分层级。
  // 这能保证主评论和回复在校验、上传图片、异常处理上的行为完全一致。
  const draft = replyTarget ? getDraftByTargetId(replyTarget.id) : commentDraft.value;
  if (!draft.content.trim() && !draft.imageUrl) return alert('评论内容和图片不能都为空');

  isCommentSubmitting.value = true;
  try {
    const payload = {
      postId: currentPost.value.id,
      parentCommentId: replyTarget ? replyTarget.id : null,
      content: draft.content.trim(),
      imageUrl: draft.imageUrl || null
    };

    // 顶级评论与回复复用同一个接口，后端会根据 parentCommentId 自己推导 rootCommentId / isReply。
    const { data, unauthorized } = await fetchJson(`${API_BASE}/comments`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(payload)
    });
    if (unauthorized) return;

    if (data?.success) {
      if (replyTarget) {
        // 回复成功后只清空当前这条回复框的草稿，不动主评论框。
        replyDrafts.value[replyTarget.id] = createEmptyDraft();
        activeReplyTargetId.value = null;
      } else {
        // 顶级评论成功后只清空主评论框。
        commentDraft.value = createEmptyDraft();
      }
      // 这里选择直接重拉评论列表，换取更简单可靠的 UI 一致性：
      // - 后端新生成的昵称 / 头像 / 回复对象会同步回来
      // - 点赞数、排序结果也不会因为前端手工插入而出错
      await loadComments();
    } else {
      alert(data?.message || '评论失败');
    }
  } catch (e) {
    alert('评论提交失败');
  } finally {
    isCommentSubmitting.value = false;
  }
};

const likeComment = async (commentId) => {
  if (!isLoggedIn.value) return alert('请先登录后再点赞');
  try {
    const { data, unauthorized } = await fetchJson(`${API_BASE}/comments/${commentId}/like`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (unauthorized) return;
    if (data?.success) {
      // 点赞接口返回的是“这条评论最新的点赞结果”，
      // 所以前端只需要局部回写，不必把整棵评论树重新请求一遍。
      updateCommentLikeState(comments.value, commentId, data.data.likeCount, data.data.likedByCurrentUser);
      triggerLikeAnimation(commentId);
    } else {
      alert(data?.message || '点赞失败');
    }
  } catch (e) {
    alert('点赞失败');
  }
};

const updateCommentLikeState = (commentList, commentId, likeCount, likedByCurrentUser) => {
  // 后端返回的是单条点赞结果，这里递归回写到树状评论中，避免整页重新加载。
  // commentList 既可能是顶级评论列表，也可能是某条评论的 replies。
  for (const comment of commentList) {
    if (comment.id === commentId) {
      comment.likeCount = likeCount;
      comment.likedByCurrentUser = likedByCurrentUser;
      return true;
    }
    if (comment.replies && comment.replies.length > 0) {
      const found = updateCommentLikeState(comment.replies, commentId, likeCount, likedByCurrentUser);
      if (found) return true;
    }
  }
  return false;
};

const triggerLikeAnimation = (commentId) => {
  // 先复位再触发，保证连续点赞/取消点赞时动画还能重复播放。
  likeAnimations.value[commentId] = false;
  requestAnimationFrame(() => {
    likeAnimations.value[commentId] = true;
    setTimeout(() => {
      likeAnimations.value[commentId] = false;
    }, 240);
  });
};

const visibleReplies = (comment) => {
  if (!comment.replies) return [];
  // 回复默认折叠，点“展开更多回复”后才显示完整列表。
  return expandedReplies.value[comment.id] ? comment.replies : comment.replies.slice(0, defaultReplyVisibleCount);
};

const toggleReplies = (commentId) => {
  // 这里的展开状态是按“顶级评论 id”分别保存的，互不影响。
  expandedReplies.value[commentId] = !expandedReplies.value[commentId];
};

const getBuildingName = (id) => {
  // buildingId 是后端存储字段，前端展示时统一翻译成建筑名称。
  if (!id) return '未知区域';
  const building = buildings.value.find((item) => item.id === id);
  return building ? building.name : '未知区域';
};

const formatTimeRange = (start, end, desc) => {
  // 这里容忍字段缺失，因为帖子数据里时间范围是允许不完整填写的。
  if (!start) return '未知日期';
  let timeStr = start;
  if (end && end !== start) timeStr += ` 至 ${end}`;
  if (desc) timeStr += ` ${desc}`;
  return timeStr;
};

const formatDateTime = (value) => {
  // 后端当前返回的是 ISO 风格时间串，这里直接转成更适合界面阅读的格式。
  if (!value) return '刚刚';
  return value.replace('T', ' ').slice(0, 16);
};
</script>

<style scoped>
.home-container { display: flex; gap: 10px; align-items: flex-start; }
.main-content { flex: 1; min-width: 0; }
.side-content { width: 296px; flex-shrink: 0; }
.side-hint { margin-top: 15px; color: #8590a6; font-size: 13px; line-height: 1.6; }

.search-card { padding: 15px; }
.search-row { display: flex; gap: 10px; margin-bottom: 10px; }
.search-row.align-center { align-items: center; margin-bottom: 0; }
.search-row .zh-input { margin-bottom: 0; }
.date-input { width: 140px; }
.label { font-size: 14px; color: #8590a6; white-space: nowrap; }
.actions { margin-left: auto; display: flex; gap: 10px; }

.empty-state { padding: 40px; text-align: center; color: #8590a6; }
.small-empty { padding: 18px 0; }

.post-item { display: flex; padding: 16px 20px; border-bottom: 1px solid #f0f2f7; cursor: pointer; transition: .2s; }
.post-item:hover { background: #fcfcfc; }
.post-item:last-child { border-bottom: none; }
.post-content { flex: 1; }
.post-title { font-size: 18px; font-weight: 600; line-height: 1.6; margin-bottom: 6px; }
.post-desc { font-size: 15px; line-height: 1.6; color: #121212; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 10px; }
.post-meta { font-size: 14px; color: #8590a6; display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }
.post-thumb { width: 190px; height: 105px; object-fit: cover; border-radius: 4px; margin-left: 16px; }
.tag { padding: 2px 6px; border-radius: 2px; font-size: 12px; font-weight: normal; margin-right: 6px; vertical-align: text-bottom; }
.tag-red { color: #f1403c; background: rgba(241,64,60,.1); }
.tag-green { color: #0084ff; background: rgba(0,132,255,.1); }
.tag-gray { color: #8590a6; background: #f6f6f6; border: 1px solid #ebebeb; }

.post-modal { width: 500px; max-height: 90vh; overflow-y: auto; }
.detail-modal { width: 680px; max-height: 90vh; overflow-y: auto; background: #f6f6f6; }
.detail-card-shell { margin: 0; border-radius: 2px 2px 0 0; }
.detail-meta { margin: 10px 0; }
.detail-image { width: 100%; max-height: 300px; object-fit: contain; background: #000; border-radius: 2px; }
.detail-desc { margin: 15px 0; line-height: 1.6; white-space: pre-wrap; }
.contact-box { background: rgba(5,109,232,.05); padding: 10px; border-radius: 3px; color: #056de8; }

.detail-section { padding: 15px; }
.section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 10px; }
.section-head h3 { margin: 0; font-size: 15px; color: #121212; }
.section-empty { text-align: center; color: #8590a6; font-size: 13px; padding: 10px; }
.recommend-list { display: flex; flex-direction: column; gap: 10px; }
.recommend-item { padding: 12px; margin: 0; cursor: pointer; border: 1px solid transparent; }
.recommend-item:hover { border-color: #056de8; }
.recommend-title { font-weight: bold; font-size: 14px; margin-bottom: 5px; }

.inline-grid { display: flex; gap: 10px; }
.inline-grid .zh-input { margin-bottom: 15px; }
.two-columns .zh-input { flex: 1; }
.location-row { align-items: flex-start; }
.location-row .zh-input { flex: 1; }
.clear-location-btn { color: #f1403c; border-color: #f1403c; background: #fff; }
.file-input { margin-bottom: 15px; }

.comment-section { border-top: 1px solid #e8edf5; }
.comment-section-header { margin-bottom: 16px; }
.comment-sort-box { display: flex; align-items: center; gap: 10px; color: #666; font-size: 14px; }
.sort-select { width: auto; margin-bottom: 0; }

.comment-editor { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 18px; box-shadow: 0 1px 3px rgba(18,18,18,.06); }
.comment-editor .zh-input { margin-bottom: 12px; min-height: 90px; resize: vertical; }
.comment-editor-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.comment-upload-area { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.secondary-btn { background: #eef3fb; color: #056de8; border: 1px solid #d7e5fb; }
.compact-btn { padding: 6px 12px; font-size: 13px; }
.hidden-input { display: none; }
.hint-text { color: #777; font-size: 13px; }
.comment-image-preview { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.comment-image-preview img { width: 96px; height: 96px; object-fit: cover; border-radius: 10px; border: 1px solid #eee; }
.comment-login-tip { margin: 12px 0 0; color: #666; font-size: 13px; }

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
.reply-editor .zh-input { min-height: 72px; margin-bottom: 10px; resize: vertical; }

.expand-btn { align-self: flex-start; background: transparent; border: none; color: #007bff; cursor: pointer; padding: 0; font-size: 14px; }
.link-btn { background: transparent; border: none; color: #666; cursor: pointer; padding: 0; font-size: 14px; }
.like-btn { display: inline-flex; align-items: center; gap: 6px; transform-origin: center; transition: color .2s ease, transform .2s ease; }
.like-btn .like-icon { width: 18px; height: 18px; display: block; filter: grayscale(1) brightness(.7); opacity: .72; transition: transform .2s ease, filter .2s ease, opacity .2s ease; }
.like-btn:hover .like-icon { transform: translateY(-1px) scale(1.06); }
.like-btn.liked { color: #0f766e; }
.like-btn.liked .like-icon { filter: none; opacity: 1; }
.like-btn.popping { animation: like-pop .24s ease-out; }
.like-btn.popping .like-icon { animation: like-icon-pop .24s ease-out; }
.link-btn:hover, .expand-btn:hover { color: #007bff; }
.link-btn.danger:hover { color: #dc3545; }

@keyframes like-pop {
  0% { transform: scale(1); }
  45% { transform: scale(1.14); }
  100% { transform: scale(1); }
}

@keyframes like-icon-pop {
  0% { transform: scale(1) rotate(0deg); }
  45% { transform: scale(1.2) rotate(-8deg); }
  100% { transform: scale(1) rotate(0deg); }
}

@media (max-width: 900px) {
  .home-container { flex-direction: column; }
  .side-content { width: 100%; }
}

@media (max-width: 768px) {
  .search-row,
  .search-row.align-center,
  .comment-editor-actions,
  .section-head,
  .inline-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    margin-left: 0;
  }

  .date-input,
  .sort-select {
    width: 100%;
  }

  .detail-modal,
  .post-modal {
    width: calc(100vw - 24px);
  }

  .post-item {
    flex-direction: column;
  }

  .post-thumb {
    width: 100%;
    height: 180px;
    margin-left: 0;
  }
}
</style>
