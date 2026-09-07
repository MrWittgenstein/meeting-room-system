import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'   //导入element-plus的中文语言包
//引入初始化样式文件
import '@/styles/common.scss'
// 引入Element Plus样式
import 'element-plus/dist/index.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.use(createPinia())
app.use(router)

app.use(ElementPlus,{
  locale:zhCn     //注册的同时设置elementplus组件库的区域语言为简体中文
})

app.mount('#app')
