import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
    state: () => ({
        currentChatUser: null,
        messageList: [], // 强制初始化为空数组
        currentSessionId: null
    }),
    actions: {
        setCurrentChatUser(user) {
            this.currentChatUser = user
        },
        setMessageList(messages) {
            // 强制保证是数组，不管后端返回什么
            this.messageList = Array.isArray(messages) ? messages : []
        },
        addMessage(message) {
            // 先确保messageList是数组
            if (!Array.isArray(this.messageList)) {
                this.messageList = []
            }
            this.messageList.push(message)
        },
        setCurrentSessionId(sessionId) {
            this.currentSessionId = sessionId
        }
    }
})