// 纯原生 WebSocket（兼容 Vite，不报错！）
class ChatSocket {
    constructor() {
        this.socket = null
        this.userId = localStorage.getItem('userId')
    }

    // 连接 WebSocket
    connect() {
        if (this.socket) return

        // 原生 WebSocket 地址（兼容你的后端）
        const wsUrl = 'ws://localhost:8080/ws/chat/websocket'
        this.socket = new WebSocket(wsUrl)

        this.socket.onopen = () => {
            console.log('✅ WebSocket 连接成功')
        }

        this.socket.onmessage = (event) => {
            const msg = JSON.parse(event.data)
            window.dispatchEvent(new CustomEvent('newChatMessage', { detail: msg }))
        }

        this.socket.onerror = () => {
            console.log('❌ WebSocket 连接失败')
        }
    }

    // 发送消息
    sendMessage(senderId, receiverId, content) {
        if (!this.socket) return
        this.socket.send(
            JSON.stringify({
                senderId,
                receiverId,
                content,
            })
        )
    }

    disconnect() {
        if (this.socket) this.socket.close()
    }
}

export default new ChatSocket()