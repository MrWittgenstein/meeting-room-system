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
import dataAnalysis from '../views/manager/components/dataAnalysis.vue' // 第 4 周数据分析页面
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
      path: '/system-admin',
      component: () => import('@/views/system-admin/index.vue'),
      meta: { systemAdmin: true },
      children: [
        { path: '', redirect: '/system-admin/users' },
        { path: 'users', component: () => import('@/views/system-admin/users.vue') },
        { path: 'logs', component: () => import('@/views/system-admin/logs.vue') },
      ],
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
        // 数据分析
        {
          path: 'dataAnalysis',
          component: dataAnalysis,
          name: 'dataAnalysis'
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

router.beforeEach(async to => {
  if (!to.matched.some(record => record.meta.systemAdmin)) return true
  if (!localStorage.getItem('sessionId') && !localStorage.getItem('token')) return '/'
  try {
    const { default: http } = await import('@/utils/http')
    let response
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        response = await http.get('/user/session', { timeout: 4000, skipAuthRedirect: true })
        break
      } catch (error) {
        const status = error.response?.status
        if (attempt > 0 || (status && status < 500)) throw error
        // Only retry this read-only identity check, never the login POST.
      }
    }
    const { data } = response
    if (data.code !== 1) throw new Error(data.message || '会话校验失败')
    if (data.data.role !== 'super_admin') {
      const { ElMessage } = await import('element-plus')
      ElMessage.error('仅系统管理员可访问此页面')
      return data.data.role === 'room_admin' ? '/manager' : '/user/home'
    }
    localStorage.setItem('userInfo', JSON.stringify(data.data))
    return true
  } catch (error) {
    if (error.response?.status === 401) return '/'
    const { ElMessage } = await import('element-plus')
    ElMessage.error(error.response?.status === 403 ? '没有系统管理员访问权限' : '暂时无法校验登录状态，请重试；无需重新输入账号密码')
    console.warn('System admin session verification failed', error.code || error.response?.status || 'invalid-response')
    return false
  }
})

export default router
