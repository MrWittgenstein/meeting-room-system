<template>
  <div class="body">
    <div class="no0">
      <div class="head-container">
        <!-- 左侧 Logo 区域 -->
        <div class="head-left">
          <!-- 移动端菜单切换按钮 -->
          <div class="mobile-menu-toggle" @click="toggleMenu">
            <el-icon class="menu-icon"><Menu /></el-icon>
          </div>
          <div class="logo">
            <el-icon class="logo-icon"><Grid /></el-icon>
            <span class="logo-text">智能会议室系统</span>
          </div>
        </div>

        <!-- 右侧功能区 -->
        <div class="head-right">
          <!-- 个人信息 -->
          <div class="user-info" @click.stop @click="tpinfo()">
            <el-avatar class="user-avatar" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
          </div>

          <el-button class="logout-btn" @click="handleExit">退出登录</el-button>
        </div>
      </div>
    </div>
    <div class="main-container">
      <el-menu
        :default-active="selectedIndex"
        @select="handleMenuSelect"
        :collapse="false"
        mode="vertical"
        background-color="#f5f5f5"
        text-color="black"
        active-text-color="#87CEEB"
        width="130px"
        class="no1"
      >
        <el-menu-item index="1">
          <el-icon><House/></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="2">
          <el-icon><EditPen /></el-icon>
          <span>预约会议</span>
        </el-menu-item>
        <el-menu-item index="3">
          <el-icon><ChatLineSquare /></el-icon>
          <span>查询预约</span>
        </el-menu-item>
        <el-menu-item index="5">
          <el-icon><PieChart /></el-icon>
          <span>会议记录</span>
        </el-menu-item>
        <el-menu-item index="4">
          <el-icon><User/></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <el-menu-item index="6">
          <el-icon><Bell/></el-icon>
          <span>消息通知</span>
        </el-menu-item>
        <el-menu-item index="7">
          <el-icon><Setting /></el-icon>
          <span>会议控制</span>
        </el-menu-item>
      </el-menu>
      <div class="no2">
        <el-calendar v-model="currentDate" class="custom-calendar" :first-day-of-week="1">
          <!-- 自定义月份导航 -->
          <template #header>
            <div class="calendar-header">
              <div class="welcome-message">欢迎您，{{ userName }}</div>
              <div class="header-controls">
                <button class="month-btn" @click="changeMonth(-1)">上月</button>
                <h3 class="current-month">{{ currentMonthText }}</h3>
                <button class="month-btn" @click="changeMonth(1)">下月</button>
                <button class="today-btn" @click="currentDate = new Date()">今天</button>
              </div>
            </div>
          </template>

          <!-- 自定义星期标题 -->
          <template #week-header>
            <tr>
              <th>一</th>
              <th>二</th>
              <th>三</th>
              <th>四</th>
              <th>五</th>
              <th>六</th>
              <th>日</th>
            </tr>
          </template>

          <!-- 自定义日期单元格内容 -->
          <template #date-cell="{ data }">
            <div
              :class="['calendar-cell', {
                'is-current': data.type === 'current-month' && data.day.split('-')[2] === String(new Date().getDate()),
                'is-other-month': data.type !== 'current-month',
                'is-selected': data.isSelected
              }]"
              @click="handleDateClick(data)"
            >
              <span class="cell-day">{{ data.day.split('-')[2] }}</span>
              <div v-if="isBookedDate(data.day)" class="booked-indicator"></div>
            </div>
          </template>
        </el-calendar>
        <div style="margin-top: 10px;">
          <span v-if="bookedDates.length > 0">
            你在{{ bookedDates.join('、') }}有预约会议室,请注意按时参加会议
          </span>
          <span v-else>暂无预约会议</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfoAPI, getUserReservationsAPI } from '@/apis/userhome.js'
const router = useRouter()
// 导航相关状态
const selectedIndex = ref("1")
// 日历相关状态
const currentDate = ref(new Date())
const bookedDates = ref([])
// 用户信息状态（设置默认值，确保界面有内容）
const userName = ref('临时测试用户')
const userAvatar = ref('/lsj.jpg')
const tpinfo=()=>{
  router.push('./info')
}
// 月份导航相关
const currentMonthText = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  return `${year}年${month}月`
})

// 切换月份
const changeMonth = (step) => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() + step)
  currentDate.value = newDate
}
/**
 * 菜单选择事件（保留真实路由跳转）
 */
  const handleMenuSelect = (menuIndex) => {
    switch (menuIndex) {
      case "1":
        router.push('/user/home')
        break
      case "2":
        router.push('/user/appoint')
        break
      case "3":
        router.push('/user/check')
        break
      case "4":
        router.push('/user/info')
        break
      case "5":
        router.push('/user/meetingMinu')
        break
      case "6":
        router.push('/user/notifications')
        break
      case "7":
        router.push('/user/control')
        break
      default:
        router.push('/user/home')
    }
  }
/**
 * 判断日期是否是已预约日期
 */
const isBookedDate = (dateStr) => {
  return bookedDates.value.includes(dateStr)
}
/**
 * 退出登录（仅跳转，不做权限清理）
 */
const handleExit = () => {
  router.push('/')
}

/**
 * 移动端菜单切换
 */
const toggleMenu = () => {
  const menuElement = document.querySelector('.no1')
  const overlayElement = document.querySelector('.menu-overlay') || createOverlay()

  if (menuElement) {
    menuElement.classList.toggle('menu-expanded')
    overlayElement.classList.toggle('menu-overlay-visible')
  }
}

/**
 * 创建菜单遮罩
 */
const createOverlay = () => {
  const overlay = document.createElement('div')
  overlay.className = 'menu-overlay'
  overlay.addEventListener('click', toggleMenu)
  document.body.appendChild(overlay)
  return overlay
}
/**
 * 获取用户信息（真实接口调用，失败时用默认值）
 */
const fetchUserInfo = async () => {
  try {
    const res = await getUserInfoAPI()
    if (res.data?.data?.username) {
      userName.value = res.data.data.username
    }
    if (res.data?.data?.thumbnailUrl) {
      userAvatar.value = res.data.data.thumbnailUrl
    }
  } catch (error) {
    // 接口失败时不报错，继续使用默认值
    console.log('使用默认用户信息展示界面')
  }
}
/**
 * 获取用户预约记录（添加静态数据以便展示效果）
 */
const fetchUserReservations = async () => {
  try {
    // 调用接口获取用户预约记录
    const res = await getUserReservationsAPI()
    const reservations = res.data.data || []

    // 获取今天的日期，格式化为YYYY-MM-DD
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    // 过滤条件：1. 今天及以后的日期 2. 状态为已通过
    const filteredReservations = reservations.filter(item => {
      if (!item.reserveDate || !item.status) return false

      // 解析预约日期
      const reserveDate = new Date(item.reserveDate)
      reserveDate.setHours(0, 0, 0, 0)

      // 检查日期是否在今天及以后，且状态为已通过
      return reserveDate >= today && item.status === '已通过'
    })

    const dates = filteredReservations.map(item => item.reserveDate).filter(Boolean)
    bookedDates.value = dates
  } catch (error) {
    // 接口失败时使用空数组
    bookedDates.value = []
    console.error('获取用户预约记录失败:', error)
  }
}

// 处理日期点击事件
const handleDateClick = (data) => {
  console.log('Date clicked:', data)
  // 这里可以添加日期选择的逻辑
  // 确保日期被正确选中
  currentDate.value = new Date(data.day)
}

// 页面挂载时加载数据（不做任何登录验证）
onMounted(async () => {
  // 并行加载数据，即使失败也不影响界面展示
  await Promise.all([
    fetchUserInfo(),
    fetchUserReservations()
  ])
})
console.log(localStorage.getItem('token'))
</script>
<style scoped>
/* 页面背景渐变 */
.body {
  border-radius: 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  background-attachment: fixed;
}

/* 顶部导航栏 - 与管理员页面一致的样式 */
.no0 {
  height: 60px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.1);
  transform-style: preserve-3d;
  perspective: 1000px;
  transition: all 0.3s ease;
  &:hover {
    box-shadow: 0 12px 40px rgba(31, 38, 135, 0.15);
    transform: translateY(-2px);
  }
}

.head-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 24px;
  max-width: 1600px;
  margin: 0 auto;
}

/* 左侧Logo区域 */
.head-left {
  display: flex;
  align-items: center;

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    height: 48px;
    padding: 0 16px;
    border-radius: 20px;
    background: linear-gradient(145deg, rgb(179, 219, 225), rgba(159, 199, 205, 1));
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    &:hover {
      box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
    }

    .logo-icon {
      font-size: 28px;
      color: #fff;
      animation: rotate 6s linear infinite;
    }

    .logo-text {
      font-size: 20px;
      font-weight: 700;
      color: #fff;
      letter-spacing: 0.5px;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
    }
  }
}

/* 右侧功能区 */
.head-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  &:hover {
    background: rgba(255, 255, 255, 1);
    transform: translateY(-3px);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border: 3px solid rgb(179, 219, 225);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    &:hover {
      transform: scale(1.1);
    }
  }

  .user-name {
    color: rgb(179, 219, 225);
    font-size: 16px;
    font-weight: 600;
    max-width: 120px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* 退出登录按钮 */
.logout-btn {
  color: rgb(244, 162, 175);
  height: 48px;
  padding: 0 16px;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  &:hover {
    background: rgb(244, 162, 175);
    color: #fff;
    transform: translateY(-3px);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  }
}

/* 旋转动画 */
@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 主容器 */
.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 15px;
  gap: 15px;
}

/* 左侧菜单 - 玻璃质感 */
.no1 {
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
}

/* 右侧内容区域 */
.no2 {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
}

/* 移动端菜单切换按钮样式 */
.mobile-menu-toggle {
  display: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-right: 12px;
  transition: all 0.3s ease;

  .menu-icon {
    font-size: 20px;
    color: rgb(179, 219, 225);
  }

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  /* 移动端头部导航栏 */
  .head-container {
    padding: 0 12px !important;
  }

  .logo-text {
    display: none !important;
  }

  /* 移动端显示用户名 */
  .user-name {
    display: block !important;
    font-size: 12px !important;
    max-width: 80px !important;
  }

  /* 调整移动端用户信息布局 */
  .user-info {
    padding: 0 12px !important;
    gap: 6px !important;
  }

  .user-avatar {
    width: 28px !important;
    height: 28px !important;
  }

  /* 显示移动端菜单切换按钮 */
  .mobile-menu-toggle {
    display: flex !important;
    align-items: center;
    justify-content: center;
  }

  /* 移动端主容器 */
  .main-container {
    flex-direction: column !important;
    padding: 0 !important;
    gap: 0 !important;
    position: relative !important;
  }

  /* 移动端左侧菜单 */
  .no1 {
    width: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    transform: translateX(-100%) !important;
    position: fixed !important;
    left: 0 !important;
    top: 60px !important;
    bottom: 0 !important;
    z-index: 100 !important;
    height: calc(100vh - 60px) !important;
    width: 200px !important;
    transition: all 0.3s ease !important;
  }

  /* 移动端右侧内容区域，添加滑动功能 */
  .no2 {
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow-y: auto !important;
    -webkit-overflow-scrolling: touch !important;
    scroll-behavior: smooth !important;
    flex: 1 !important;
    min-height: 450px !important;
    border-radius: 0 !important;
  }

  /* 确保主容器高度足够 */
  .main-container {
    flex: 1 !important;
    min-height: calc(100vh - 60px) !important;
  }

  /* 确保整个页面没有多余的边距 */
  body {
    margin: 0 !important;
    padding: 0 !important;
  }

  /* 移动端整体容器 */
  .body {
    border-radius: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  /* 移动端头部 */
  .no0 {
    border-radius: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  /* 移动端头部容器 */
  .head-container {
    padding: 0 8px !important;
  }

  /* 菜单展开时的样式 */
  .no1.menu-expanded {
    transform: translateX(0) !important;
    width: 200px !important;
    padding: 10px !important;
  }

  /* 菜单遮罩 */
  .menu-overlay {
    position: fixed !important;
    top: 60px !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    background: rgba(0, 0, 0, 0.3) !important;
    z-index: 99 !important;
    opacity: 0 !important;
    visibility: hidden !important;
    transition: all 0.3s ease !important;
  }

  /* 菜单展开时显示遮罩 */
  .menu-overlay.menu-overlay-visible {
    opacity: 1 !important;
    visibility: visible !important;
  }

  /* 移动端日历头部 */
  .calendar-header {
    flex-direction: column !important;
    gap: 10px !important;
    padding: 10px !important;
    align-items: flex-start !important;
  }

  /* 移动端欢迎信息 */
  .welcome-message {
    font-size: 16px !important;
  }

  /* 移动端日历头部控制 */
  .header-controls {
    width: 100% !important;
    justify-content: space-between !important;
    flex-wrap: nowrap !important;
    gap: 8px !important;
  }

  /* 移动端月份按钮 */
  .month-btn {
    padding: 6px 10px !important;
    font-size: 12px !important;
    flex: 1 !important;
    text-align: center !important;
  }

  /* 移动端当前月份 */
  .current-month {
    font-size: 14px !important;
    min-width: 120px !important;
    margin: 0 !important;
    padding: 8px 12px !important;
    flex: 2 !important;
    text-align: center !important;
  }

  /* 移动端今天按钮 */
  .today-btn {
    padding: 6px 10px !important;
    font-size: 12px !important;
    flex: 1 !important;
    text-align: center !important;
  }

  /* 移动端日历整体布局优化 */
  .custom-calendar {
    padding: 8px !important;
    min-height: 320px !important;
    height: auto !important;
    background: rgba(255, 255, 255, 0.95) !important;
    border-radius: 0 !important;
    box-shadow: none !important;
  }

  /* 移动端日历头部 */
  .calendar-header {
    flex-direction: column !important;
    gap: 8px !important;
    padding: 8px 4px !important;
    align-items: flex-start !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }

  /* 移动端欢迎信息 */
  .welcome-message {
    font-size: 14px !important;
    font-weight: 600 !important;
    color: #333 !important;
    margin: 0 !important;
    padding: 0 4px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }

  /* 移动端头部控制 */
  .header-controls {
    width: 100% !important;
    justify-content: space-between !important;
    flex-wrap: nowrap !important;
    gap: 6px !important;
    padding: 0 4px !important;
  }

  /* 移动端月份按钮 */
  .month-btn {
    padding: 4px 8px !important;
    font-size: 12px !important;
    border-radius: 12px !important;
    flex: 1 !important;
    text-align: center !important;
    margin: 0 !important;
  }

  /* 移动端当前月份 */
  .current-month {
    font-size: 12px !important;
    min-width: 100px !important;
    padding: 4px 12px !important;
    text-align: center !important;
    flex: 2 !important;
    margin: 0 !important;
  }

  /* 移动端今天按钮 */
  .today-btn {
    padding: 4px 8px !important;
    font-size: 12px !important;
    border-radius: 12px !important;
    flex: 1 !important;
    text-align: center !important;
    margin: 0 !important;
  }

  /* 移动端日历单元格，调整为更合理的大小 */
  .calendar-cell {
    padding: 0 !important;
    margin: 0 !important;
    height: 28px !important;
    width: 28px !important;
    box-sizing: border-box !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    border-radius: 50% !important;
    background: white !important;
    border: 1px solid #E3F2FD !important;
    flex-shrink: 0 !important;
    position: static !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  }

  /* 选中状态样式 - 浅天蓝色背景 */
  .calendar-cell.is-current,
  .calendar-cell.is-selected {
    padding: 0 !important;
    margin: 0 !important;
    height: 28px !important;
    width: 28px !important;
    background: #E3F2FD !important;
    border: 1px solid #B3E5FC !important;
    position: static !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  }

  /* 确保Element Plus选中状态应用浅天蓝色背景 */
  :deep(.el-calendar-table__row td.is-selected .calendar-cell) {
    padding: 0 !important;
    margin: 0 !important;
    height: 28px !important;
    width: 28px !important;
    background: #E3F2FD !important;
    border: 1px solid #B3E5FC !important;
    position: static !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  }

  /* 移动端单元格日期 */
  .cell-day {
    font-size: 12px !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    width: 100% !important;
    height: 100% !important;
    color: #4A4A4A !important;
    font-weight: 500 !important;
    margin: 0 !important;
    padding: 0 !important;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  }

  /* 移除选中状态文本颜色样式，保持所有日期文本样式一致 */
  .calendar-cell.is-current .cell-day,
  .calendar-cell.is-selected .cell-day,
  :deep(.el-calendar-table__row td.is-selected .calendar-cell .cell-day) {
    color: #4A4A4A !important;
    font-weight: 500 !important;
    text-shadow: none !important;
  }

  /* 移动端其他月份日期 */
  .calendar-cell.is-other-month .cell-day {
    color: #D1D5DB !important;
    opacity: 0.6 !important;
  }

  /* 确保表格单元格大小一致 */
  :deep(.el-calendar-table td) {
    padding: 4px !important;
    width: calc(100% / 7) !important;
    height: 32px !important;
    text-align: center !important;
    vertical-align: middle !important;
  }

  /* 确保表格行高一致 */
  :deep(.el-calendar-table__row) {
    height: 32px !important;
  }

  /* 确保所有日期单元格对齐一致 */
  :deep(.el-calendar-table__cell) {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    padding: 0 !important;
    margin: 0 !important;
    height: 32px !important;
    width: 100% !important;
  }

  /* 确保没有移动的样式 */
  :deep(.el-calendar-table__row td.is-selected),
  :deep(.el-calendar-table__today) {
    padding: 4px !important;
    margin: 0 !important;
    background: none !important;
  }

  /* 确保没有额外的间距或边距 */
  :deep(.el-calendar-table) {
    border-collapse: separate !important;
    border-spacing: 0 !important;
    width: 100% !important;
    table-layout: fixed !important;
  }

  /* 确保Element Plus选中状态不覆盖我们的样式 */
  :deep(.el-calendar-table__row td.is-selected div),
  :deep(.el-calendar-table__today div) {
    background: none !important;
  }

  :deep(.el-calendar-table__row td.is-selected .el-calendar-table__cell span),
  :deep(.el-calendar-table__today .el-calendar-table__cell span) {
    color: inherit !important;
  }

  /* 移除任何可能导致蓝色长方形背景框的样式 */
  :deep(.el-calendar-table__row td),
  :deep(.el-calendar-table__cell),
  :deep(.el-calendar-table__row td div),
  :deep(.el-calendar-table__row td span) {
    background: none !important;
    box-shadow: none !important;
    border: none !important;
  }

  /* 确保没有额外的背景样式 */
  :deep(.el-calendar-table__row),
  :deep(.el-calendar-table__body) {
    background: none !important;
  }

  /* 确保日历显示完整 */
  :deep(.el-calendar) {
    height: auto !important;
    min-height: 320px !important;
    touch-action: pan-y !important;
    overflow-y: auto !important;
    -webkit-overflow-scrolling: touch !important;
  }

  /* 确保日历内容区域高度足够 */
  :deep(.el-calendar__body) {
    height: auto !important;
    min-height: 280px !important;
  }

  /* 隐藏滚动条 */
  .no2::-webkit-scrollbar,
  :deep(.el-calendar)::-webkit-scrollbar {
    display: none !important;
  }

  /* 移动端日历星期标题 */
  :deep(.el-calendar-table__row th) {
    padding: 10px 0 !important;
    font-size: 12px !important;
    font-weight: 600 !important;
    color: #666 !important;
    text-align: center !important;
    background: rgba(240, 248, 255, 0.5) !important;
    border-bottom: 1px solid #E3F2FD !important;
  }

  /* 移动端日历表格 */
  :deep(.el-calendar-table) {
    width: 100% !important;
    table-layout: fixed !important;
  }

  /* 优化移动端日历的整体视觉效果 */
  :deep(.el-calendar) {
    --el-calendar-bg-color: transparent !important;
  }

  /* 移除鼠标悬停效果 */
  .calendar-cell:hover {
    background: white !important;
    border-color: #E3F2FD !important;
  }

  /* 已预约日期标记优化 */
  .booked-indicator {
    bottom: 6px !important;
    width: 16px !important;
    height: 3px !important;
    background: linear-gradient(145deg, #87CEEB, #B0E0E6) !important;
    border-radius: 1.5px !important;
  }

  /* 移动端日历下方的提示文字 */
  .no2 > div:last-child {
    margin-top: 8px !important;
    padding: 8px !important;
    font-size: 12px !important;
    color: #666 !important;
    text-align: center !important;
    background: rgba(240, 248, 255, 0.5) !important;
    border-radius: 0 !important;
    line-height: 1.4 !important;
    margin: 0 !important;
  }
  :deep(.el-calendar-table) {
    width: 100% !important;
    table-layout: fixed !important;
  }

  /* 移动端日历星期标题 */
  :deep(.el-calendar-table th) {
    padding: 8px 0 !important;
    font-size: 12px !important;
  }
}

/* 日历样式 - 与管理员页面一致 */
.custom-calendar {
  border-radius: 20px;
  overflow: hidden;
  border: none;
  width: 100%;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  flex: 1;
}
/* 自定义日历头部 */
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.1), rgba(179, 219, 225, 0.05));
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-radius: 20px 20px 0 0;
}
.welcome-message {
  font-size: 18px;
  font-weight: 700;
  color: rgb(179, 219, 225);
  display: flex;
  align-items: center;
  gap: 12px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}
.welcome-message::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 24px;
  background: linear-gradient(180deg, rgb(179, 219, 225), rgb(197, 186, 233));
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}
.header-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}
.month-btn {
  padding: 8px 18px;
  background: linear-gradient(145deg, #f8f9fa, #e9ecef);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-size: 16px;
  font-weight: 600;
  color: #4e5969;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  &:hover {
    background: linear-gradient(145deg, rgb(197, 186, 233), rgb(217, 206, 243));
    border-color: rgb(197, 186, 233);
    color: #fff;
    transform: translateY(-3px);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  }
}
.current-month {
  margin: 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: rgb(179, 219, 225);
  padding: 10px 20px;
  min-width: 160px;
  text-align: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}
.today-btn {
  padding: 8px 22px;
  background: linear-gradient(145deg, rgb(179, 219, 225), rgb(199, 239, 245));
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  &:hover {
    background: linear-gradient(145deg, rgb(189, 229, 235), rgb(209, 249, 255));
    transform: translateY(-3px) scale(1.05);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  }
  &:active {
    transform: translateY(-1px) scale(0.98);
  }
}
/* 日历表格样式 */
:deep(.el-calendar-table) {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: transparent;
}
:deep(.el-calendar-table__row) {
  height: 100px;
  transition: all 0.3s ease;
  &:hover {
    transform: scale(1.01);
  }
}
:deep(.el-calendar-table td) {
  vertical-align: top;
  padding: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  width: 14.2857%;
  transition: all 0.3s ease;
  &:hover {
    background: rgba(255, 255, 255, 0.5);
  }
}
:deep(.el-calendar-table td:last-child) {
  border-right: none;
}
:deep(.el-calendar-table th) {
  padding: 16px 0;
  text-align: center;
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.15), rgba(179, 219, 225, 0.1));
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  font-weight: 600;
  color: rgb(179, 219, 225);
  font-size: 16px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}
/* 完全重置日历样式 */
:deep(.el-calendar) {
  --el-calendar-bg-color: transparent !important;
}

:deep(.el-calendar-table) {
  border-collapse: separate !important;
  border-spacing: 0 !important;
}

:deep(.el-calendar-table__row) {
  height: 60px !important;
}

:deep(.el-calendar-table td) {
  padding: 0 !important;
  width: 14.2857% !important;
  height: 60px !important;
  background: none !important;
  border: none !important;
}

:deep(.el-calendar-table th) {
  padding: 12px 0 !important;
  text-align: center !important;
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.15), rgba(179, 219, 225, 0.1)) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3) !important;
  font-weight: 600 !important;
  color: rgb(179, 219, 225) !important;
  font-size: 16px !important;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8) !important;
}

/* 移除所有默认选中样式 */
:deep(.el-calendar-table__row td.is-selected),
:deep(.el-calendar-table__today),
:deep(.el-calendar-table__row td.is-selected div),
:deep(.el-calendar-table__today div) {
  background: none !important;
  border: none !important;
  box-shadow: none !important;
  transform: none !important;
}

:deep(.el-calendar-table__row td.is-selected .el-calendar-table__cell span),
:deep(.el-calendar-table__today .el-calendar-table__cell span) {
  color: inherit !important;
  font-weight: inherit !important;
}

/* 日期单元格样式 */
.calendar-cell {
  position: relative;
  width: 48px;
  height: 48px;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.6);
  margin: 6px auto;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 保持所有日期样式一致 */
.calendar-cell:hover {
  background: rgba(255, 255, 255, 0.6);
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 6px auto;
  padding: 0;
  border-radius: 50%;
}

/* 选中状态样式 - 浅天蓝色背景 */
.calendar-cell.is-selected,
.calendar-cell.is-current {
  background: #E3F2FD;
  border: 2px solid #B3E5FC;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 6px auto;
  padding: 0;
  border-radius: 50%;
}

/* 确保Element Plus选中状态也能应用我们的样式 */
:deep(.el-calendar-table__row td.is-selected .calendar-cell) {
  background: #E3F2FD;
  border: 2px solid #B3E5FC;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 6px auto;
  padding: 0;
  border-radius: 50%;
}

/* 确保单元格内容居中 */
.cell-day {
  font-size: 18px;
  font-weight: 700;
  color: rgb(179, 219, 225);
  text-shadow: 2px 2px 4px rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 保持所有日期文本样式一致 */
:deep(.el-calendar-table__row td.is-selected .calendar-cell .cell-day),
.calendar-cell.is-selected .cell-day,
.calendar-cell.is-current .cell-day {
  color: rgb(179, 219, 225);
  font-weight: 700;
}

/* 移除任何可能的背景条 */
:deep(.el-calendar-table__row td),
:deep(.el-calendar-table__row),
:deep(.el-calendar__body),
:deep(.el-calendar-table__cell) {
  background: none !important;
  border: none !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 确保没有额外的样式 */
:deep(.el-calendar-table__cell) {
  position: relative !important;
  height: auto !important;
  cursor: pointer !important;
}

/* 其他月份样式 */
.calendar-cell.is-other-month {
  opacity: 0.6;
  background: rgba(255, 255, 255, 0.3);
}
/* 已预约日期标记 */
.booked-indicator {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 4px;
  background: linear-gradient(145deg, rgb(179, 219, 225), rgb(199, 239, 245));
  border-radius: 2px;
  box-shadow: 0 4px 12px rgba(179, 219, 225, 0.3);
  margin-top: 8px;
}

/* 菜单项样式 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 55px !important;
  line-height: 55px !important;
  margin-bottom: 8px !important;
  border-radius: 16px !important;
  padding: 0 20px !important;
  background: rgba(255, 255, 255, 0.8) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  color: #666 !important;
  font-weight: 600 !important;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.15), rgba(179, 219, 225, 0.05)) !important;
  border-color: rgba(179, 219, 225, 0.4) !important;
  color: rgb(179, 219, 225) !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12) !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.25), rgba(179, 219, 225, 0.15)) !important;
  border-color: rgb(179, 219, 225) !important;
  color: rgb(179, 219, 225) !important;
  font-weight: 700 !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12) !important;
}

/* 折叠按钮样式 */
:deep(.el-button) {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  &:hover {
    background: rgba(179, 219, 225, 0.8);
    border-color: rgba(179, 219, 225, 0.6);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
  }
}

/* 图标样式 */
:deep(.el-icon) {
  font-size: 22px;
  margin-right: 12px;
  color: rgb(179, 219, 225);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}
</style>
