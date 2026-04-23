<template>
  <div class="chat-box">
    <!-- 顶部搜索用户栏（实时搜索+按钮搜索） -->
    <div class="chat-search-bar">
      <input
          v-model="searchKeyword"
          placeholder="输入用户名搜索联系人"
          class="search-input"
          @input="searchUserList"
          @keyup.enter="searchUserList"
      />
      <button class="search-btn" @click="searchUserList">搜索联系人</button>
      <button class="clear-btn" @click="clearSearch">清空</button>
    </div>

    <div class="chat-container">
      <!-- 左侧联系人列表（从数据库加载真实用户） -->
      <div class="contact-list">
        <div class="list-title">联系人列表</div>
        <div class="contact-item" v-for="user in filterUserList" :key="user.id" @click="selectContact(user)">
          <div class="contact-name">{{ user.nickname || user.username }}</div>
        </div>
      </div>

      <!-- 右侧聊天窗口 -->
      <div class="chat-main">
        <div class="chat-title" v-if="currentChatUser">
          正在与：{{ currentChatUser.nickname || currentChatUser.username }} 聊天
        </div>
        <div class="empty-tip" v-else>请从左侧选择一位联系人开始聊天</div>

        <!-- 消息列表区域：从全局缓存读取 -->
        <div class="msg-list" ref="msgListRef">
          <div v-for="msg in messageList" :key="msg.id" class="msg-item" :class="{ self: msg.senderId === userId }">
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.sendTime) }}</div>
          </div>
        </div>

        <!-- 消息输入发送框 -->
        <div class="input-box">
          <input
              v-model="inputText"
              placeholder="输入消息，回车发送"
              @keyup.enter="sendMsg"
              :disabled="!currentChatUser"
          />
          <button @click="sendMsg" :disabled="!currentChatUser">发送消息</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chatStore'

// 全局状态
const chatStore = useChatStore()

// 当前登录用户信息
const userInfo = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const userId = ref(userInfo.value.id || 1)

// 搜索配置
const searchKeyword = ref('')
const allUserList = ref([]) // 从数据库加载的真实用户
const filterUserList = ref([])

// 聊天状态
const currentChatUser = ref(chatStore.currentChatUser)
const messageList = ref(chatStore.messageList)
const inputText = ref('')
const msgListRef = ref(null)

// ========== 页面加载：从数据库加载真实用户列表 ==========
onMounted(() => {
  // 1. 从后端加载所有真实用户
  loadAllRealUsers()

  // 2. 如果之前有缓存的聊天对象，自动加载历史消息
  if (chatStore.currentChatUser) {
    currentChatUser.value = chatStore.currentChatUser
    loadChatHistory(chatStore.currentChatUser.id)
  }
})

// ========== 核心：从数据库加载所有真实用户 ==========
const loadAllRealUsers = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/users/list')
    const users = await res.json()
    // 不过滤自己，显示所有用户
    allUserList.value = users
    filterUserList.value = [...allUserList.value]
  } catch (e) {
    console.error("加载真实用户失败", e)
  }
}

// ========== 监听全局状态变化 ==========
watch(() => chatStore.messageList, (newList) => {
  messageList.value = newList
  // 自动滚动到底部
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}, { deep: true })

watch(() => chatStore.currentChatUser, (newUser) => {
  currentChatUser.value = newUser
})

// ========== 加载历史聊天记录（从后端数据库读取） ==========
const loadChatHistory = async (targetUserId) => {
  if (!targetUserId) return

  try {
    // 1. 先获取或创建会话
    const sessionRes = await fetch(`http://localhost:8080/api/chat/session?userId1=${userId.value}&userId2=${targetUserId}`)
    const session = await sessionRes.json()

    chatStore.setCurrentSessionId(session.id)

    // 2. 根据sessionId从后端加载历史消息
    const msgRes = await fetch(`http://localhost:8080/api/chat/messages?sessionId=${session.id}&pageNum=1&pageSize=100`)
    const history = await msgRes.json()

    chatStore.setMessageList(history)
  } catch (e) {
    console.error("加载历史消息失败", e)
    chatStore.setMessageList([])
  }
}

// ========== 选择联系人 + 加载历史记录 ==========
const selectContact = (user) => {
  chatStore.setCurrentChatUser(user)
  inputText.value = ''
  loadChatHistory(user.id)
}

// ========== 外部调用：评论头像点击直接聊天 ==========
window.startChatWithUserId = (targetUserId) => {
  const targetUser = allUserList.value.find(u => u.id == targetUserId) || { id: targetUserId, nickname: '用户 ' + targetUserId }

  window.activeTab = 'chat'
  chatStore.setCurrentChatUser(targetUser)
  loadChatHistory(targetUserId)
}

// ========== 搜索用户（实时生效） ==========
const searchUserList = () => {
  const key = searchKeyword.value.toLowerCase().trim()
  if (!key) {
    filterUserList.value = [...allUserList.value]
    return
  }
  filterUserList.value = allUserList.value.filter(u =>
      (u.nickname && u.nickname.toLowerCase().includes(key)) ||
      (u.username && u.username.toLowerCase().includes(key))
  )
}

// ========== 清空搜索 ==========
const clearSearch = () => {
  searchKeyword.value = ''
  filterUserList.value = [...allUserList.value]
}

// ========== 发送消息（只用 REST API，100% 存数据库） ==========
const sendMsg = async () => {
  if (!inputText.value.trim() || !currentChatUser.value) return

  try {
    // 1. 用 REST API 发送给后端，直接写入数据库
    const res = await fetch('http://localhost:8080/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        senderId: userId.value,
        receiverId: currentChatUser.value.id,
        content: inputText.value.trim()
      })
    })
    const newMsg = await res.json()

    // 2. 加到消息列表（立刻显示）
    chatStore.addMessage(newMsg)
    inputText.value = ''
  } catch (e) {
    console.error("发送消息失败", e)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
/* 样式保持不变 */
.chat-box { width: 100%; height: 70vh; padding: 20px; box-sizing: border-box; }
.chat-search-bar { display: flex; gap:10px; margin-bottom:15px; }
.search-input { flex:1; padding:10px; border:1px solid #eee; border-radius:6px; outline:none; }
.search-btn { padding:0 20px; background:#007dff; color:#fff; border:none; border-radius:6px; cursor:pointer; }
.clear-btn { padding:0 15px; background:#f5f7fa; color:#666; border:none; border-radius:6px; cursor:pointer; }
.clear-btn:hover { background:#e6f7ff; }

.chat-container { display:flex; gap:20px; height: calc(70vh - 70px); }

.contact-list { width:220px; border:1px solid #eee; border-radius:8px; padding:10px; overflow-y:auto; }
.list-title { font-size:16px; font-weight:bold; margin-bottom:15px; padding-bottom:10px; border-bottom:1px solid #eee; }
.contact-item { padding:12px 10px; border-radius:6px; cursor:pointer; margin-bottom:8px; }
.contact-item:hover { background:#f5f7fa; }
.contact-name { font-size:14px; }

.chat-main { flex:1; display:flex; flex-direction:column; height:100%; }
.chat-title { padding:10px; border-bottom:1px solid #eee; font-weight:600; }
.empty-tip { padding:20px; color:#999; text-align:center; border-bottom:1px solid #eee; }

.msg-list { flex:1; overflow-y: auto; padding:10px; border:1px solid #eee; border-radius:8px; }
.msg-item { margin:8px; max-width:60%; }
.msg-content { padding:10px; border-radius:8px; word-break: break-all; }
.msg-item.self { margin-left:auto; text-align:right; }
.msg-item.self .msg-content { background:#007dff; color:#fff; }
.msg-item:not(.self) .msg-content { background:#f5f7fa; }
.msg-time { font-size:12px; color:#999; margin-top:4px; }

.input-box { display:flex; gap:10px; margin-top:10px; }
.input-box input { flex:1; padding:12px; border:1px solid #ddd; border-radius:4px; outline:none; }
.input-box button { padding:0 20px; background:#007dff; color:#fff; border:none; border-radius:4px; cursor:pointer; }
.input-box button:disabled { background:#ccc; cursor:not-allowed; }
</style>