//createRouter: 创建路由实例
//createWebHistory: 创建基于浏览器history的路由实例
import { createRouter, createWebHistory } from 'vue-router'
import login from '@/views/login/index.vue'
import manager from '@/views/manager/index.vue'

import home from '@/views/manager/components/home.vue'
import meetingRoomInfo from '../views/manager/components/meetingRoomInfo.vue' // 会议室信息页面
import meetingRoomRecords from '../views/manager/components/meetingRoomRecords.vue' // 会议室使用记录页面
import pendingApprovalList from '../views/manager/components/pendingApprovalLIst.vue'// 待审批页面
import approvedList from '../views/manager/components/approvedList.vue'// 已审批页面
import personalInfo from '../views/manager/components/personalInfo.vue'// 个人信息页面
import notificationManagement from '../views/manager/components/notificationManagement.vue'// 消息通知管理页面
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  //path和component的映射关系
  routes: [
    {
      path: '/',
      name: 'login',
      component: login
    },

    {
      path: '/manager',
      name: 'manager',
      component: manager,
      children: [
        {
          // 二级路由（默认显示会议室信息）
          path: '',
          name: 'home',
          component: home
        },
        // 会议室信息
        {
          path: 'meetingRoomInfo',
          component: meetingRoomInfo,
          name: 'meetingRoomInfo' // 路由名称，用于跳转
        },
        // 会议室使用记录
        {
          path: 'meetingRoomRecords',
          component: meetingRoomRecords,
          name: 'meetingRoomRecords'
        },

        // 待审批
        {
          path: 'pendingApprovalList',
          component: pendingApprovalList,
          name: 'pendingApprovalList'
        },
        // 已审批
        {
          path: 'approvedList',
          component: approvedList,
          name: 'approvedList'
        },
        // 个人信息
        {
          path: 'personalInfo',
          component: personalInfo,
          name: 'personalInfo' // 路由名称，用于跳转
        },
        // 消息通知管理
        {
          path: 'notificationManagement',
          component: notificationManagement,
          name: 'notificationManagement' // 路由名称，用于跳转
        }
      ]
    },
    {
        path:"/user/home",
        component:()=>import("../views/user/home.vue")
    },
    {
        path:"/user/appoint",
        component:()=>import("../views/user/appoint.vue")
    },
    {
        path:"/user/check",
        component:()=>import("../views/user/check-appoint.vue")
    },
    {
        path:"/user/info",
        component:()=>import("../views/user/info.vue")
    },
    {
        path:"/user/meetingMinu",
        component:()=>import("../views/user/meetingMinu.vue")
    }
    ,
    {
        path:"/user/notifications",
        component:()=>import("../views/user/notifications.vue")
    },
    {
      path:"/user/control",
      component:()=>import("../views/user/meetingControl.vue")
    }
  ],
})

export default router
