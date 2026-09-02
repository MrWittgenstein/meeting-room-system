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
import { getMeetingRoomList, pendingApprovalList } from '@/apis/meetingRoomList.js'

const app = createApp(App)
getMeetingRoomList().then(() => {
    //console.log('会议室列表接口返回的数据：', res.data)
})
pendingApprovalList(0).then(() => {
   // console.log('待审批列表接口返回的数据：', res.data)
})
pendingApprovalList(1).then(() => {
    //console.log('已审批列表接口返回的数据：', res.data)
})

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.use(createPinia())
app.use(router)

app.use(ElementPlus,{
  locale:zhCn     //注册的同时设置elementplus组件库的区域语言为简体中文
})

app.mount('#app')
