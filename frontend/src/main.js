import { createApp } from 'vue'
import { createPinia } from 'pinia' // 新增
import App from './App.vue'

const app = createApp(App)
app.use(createPinia()) // 新增
app.mount('#app')