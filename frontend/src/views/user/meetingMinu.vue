<template>
  <div class="body">
    <div class="no0">
      <div class="head-container">
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
        <div class="head-right">
          <div class="user-info" @click="tpinfo">
            <el-avatar class="user-avatar" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
          </div>
          <el-button class="logout-btn" @click="handleExit">退出登录</el-button>
        </div>
      </div>
    </div>
    <div class="main-container">
      <el-menu  :default-active="selectedIndex" @select="selected" :collapse="false" mode="vertical" background-color="#f5f5f5" text-color="black" active-text-color="#87CEEB" width="130px" class="no1">
        <el-menu-item index="1"><el-icon><House/></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="2"><el-icon><EditPen /></el-icon><span>预约会议</span></el-menu-item>
        <el-menu-item index="3"><el-icon><ChatLineSquare /></el-icon><span>查询预约</span></el-menu-item>
        <el-menu-item index="5"><el-icon><PieChart /></el-icon><span>会议记录</span></el-menu-item>
        <el-menu-item index="4"><el-icon><User/></el-icon><span>个人信息</span></el-menu-item>
        <el-menu-item index="6"><el-icon><Bell /></el-icon><span>消息通知</span></el-menu-item>
        <el-menu-item index="7"><el-icon><Setting /></el-icon><span>会议控制</span></el-menu-item>
      </el-menu>

      <div class="no2">
        <div class="container">
          <div class="n1">
            <!-- 用户会议统计卡片 -->
            <el-row :gutter="20" class="stats-row">
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-item">
                    <div class="stat-icon">
                      <el-icon color="#52c41a" size="24"><UserFilled /></el-icon>
                    </div>
                    <div class="stat-number">{{ userStats.participated }}</div>
                    <div class="stat-label">我参与的会议</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-item">
                    <div class="stat-icon">
                      <el-icon color="#faad14" size="24"><Clock /></el-icon>
                    </div>
                    <div class="stat-number">{{ userStats.upcoming }}</div>
                    <div class="stat-label">即将开始的会议</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-item">
                    <div class="stat-icon">
                      <el-icon color="#722ed1" size="24"><CircleCheckFilled /></el-icon>
                    </div>
                    <div class="stat-number">{{ userStats.completed }}</div>
                    <div class="stat-label">已完成的会议</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 用户会议图表区域 -->
            <el-row :gutter="20" class="charts-row">
              <el-col :span="24">
                <el-card class="chart-card">
                  <template v-slot:header>
                    <div  class="card-header">
                      <span>我常用的会议室</span>

                    </div>
                  </template>
                  <div class="chart-container">
                    <div id="roomChart" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" class="charts-row">
              <el-col :span="24">
                <el-card class="chart-card">
                  <template v-slot:header>
                    <div  class="card-header">
                      <span>我的会议时间分布</span>

                    </div>
                  </template>
                  <div class="chart-container">
                    <div id="timeChart" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
// 导入API和辅助函数
import { getUserInfo, getUserReservations, getRoomNameByRoomId, getRoomTypeByRoomId } from '@/apis/usermeeting.js'
// 导入Element图标


const router = useRouter()
const selected = (a) => {
  console.log(a)
  if (a == "1") {
    router.push('./home')
  } else if (a == "2") {
    router.push('./appoint')
  } else if (a == "3") {
    router.push('./check')
  } else if (a == "4") {
    router.push('./info')
  } else if (a == '5') {
    router.push('./meetingMinu')
  } else if(a=='6'){
    router.push('./notifications')
  } else if(a=='7'){
    router.push('/user/control')
  }
}

const selectedIndex = ref("5")

// 右上角用户信息（从接口获取）
const userAvatar = ref('/lsj.jpg') // 默认头像
const userName = ref('临时测试用户') // 默认用户名

// 用户会议统计数据（从接口计算得出）
const userStats = ref({
  participated: 0,  // 我参与的会议总数
  upcoming: 0,       // 即将开始的会议
  completed: 0       // 已完成的会议
})

// 图表筛选条件
const roomRange = ref('30d')
const timeRange = ref('day')

// 用户会议数据（从接口计算得出）
const userMeetingData = ref({
  frequentRooms: {
    labels: [],
    values: []
  },
  timeDistribution: {
    hours: ['9:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'],
    counts: [0, 0, 0, 0, 0, 0, 0, 0, 0]
  }
})

// 图表实例
let roomChartInstance = null
let timeChartInstance = null

/**
 * 1. 计算用户会议统计（参与/即将开始/已完成）
 * @param {Array} reservations - 接口返回的预约记录数组
 */
const calculateUserStats = (reservations) => {
  const now = new Date()
  let upcoming = 0
  let completed = 0
  console.log("reser",reservations)
  reservations.forEach(item => {
    // 处理会议时间（接口返回date可能为null，这里用createTime的日期补全）
    const meetingDate = item.reserveDate || item.createTime.split('T')[0]
    const startTime = new Date(`${meetingDate}T${item.startTime}`)
    const endTime = new Date(`${meetingDate}T${item.endTime}`)


    if (startTime > now) upcoming++ // 会议未开始
    if (endTime < now) completed++ // 会议已结束
  })

  userStats.value = {
    participated: reservations.length,
    upcoming,
    completed
  }
}

/**
 * 2. 计算常用会议室（按使用次数排序）
 * @param {Array} reservations - 接口返回的预约记录数组
 */
const calculateFrequentRooms = (reservations) => {
  const roomCount = {}
  // 统计每个会议室的使用次数
  reservations.forEach(item => {
    const roomName = getRoomNameByRoomId(item.roomName)
    roomCount[roomName] = (roomCount[roomName] || 0) + 1
  })
  // 转换为数组并排序
  const roomArr = Object.entries(roomCount).sort((a, b) => b[1] - a[1])
  userMeetingData.value.frequentRooms = {
    labels: roomArr.map(item => item[0]),
    values: roomArr.map(item => item[1])
  }
}

/**
 * 3. 计算会议时间分布（按小时统计）
 * @param {Array} reservations - 接口返回的预约记录数组
 */
const calculateTimeDistribution = (reservations) => {
  const hourCount = new Array(9).fill(0) // 对应hours数组的9个时段
  const hourMap = {
    '9:00': 0, '10:00': 1, '11:00': 2,
    '13:00': 3, '14:00': 4, '15:00': 5,
    '16:00': 6, '17:00': 7, '18:00': 8
  }

  reservations.forEach(item => {
    const startHour = item.startTime.split(':')[0] + ':00'
    if (hourMap[startHour] !== undefined) {
      hourCount[hourMap[startHour]]++
    }
  })

  userMeetingData.value.timeDistribution.counts = hourCount
}

/**
 * 4. 加载接口数据并初始化图表（添加静态数据以便展示效果）
 */
const tpinfo=()=>{
  router.push('./info')
}
const loadDataAndInitCharts = async () => {
  try {
    // 步骤1：获取用户信息（头像、用户名）
    try {
      const userRes = await getUserInfo()
      if (userRes.data.code === 1 && userRes.data.message === 'success') {
        const user = userRes.data.data
        userName.value =  user.username || '临时测试用户'
        userAvatar.value = user.thumbnailUrl || user.originUrl || '/lsj.jpg'
      }
    } catch (userErr) {
      // 使用默认用户信息
      userName.value = '临时测试用户'
      userAvatar.value = '/lsj.jpg'
    }

    // 步骤2：获取用户预约记录
    try {
      // 调用接口获取用户预约记录
      const res = await getUserReservations()
      const reservations = res.data.data || []

      // 步骤3：计算统计数据和图表数据
      calculateUserStats(reservations)
      calculateFrequentRooms(reservations)
      calculateTimeDistribution(reservations)
      // 步骤4：初始化图表
      initCharts()
    } catch (resvErr) {
      // 接口失败时使用空数据
      console.error('获取会议数据失败:', resvErr)
      ElMessage.error('获取会议数据失败，请重试')

      // 使用空数据初始化图表
      calculateUserStats([])
      calculateFrequentRooms([])
      calculateTimeDistribution([])
      initCharts()
    }
  } catch (err) {
    ElMessage.error('加载会议数据失败，请重试')
    console.error('接口请求失败：', err)
  }
}

// -------------------------- 图表相关逻辑 --------------------------
// 初始化所有图表
const initCharts = () => {
  // 常用会议室图表（柱状图）
  roomChartInstance = echarts.init(document.getElementById('roomChart'))
  updateRoomChart()

  // 会议时间分布图表（折线图）
  timeChartInstance = echarts.init(document.getElementById('timeChart'))
  updateTimeChart()
}

// 更新常用会议室图表
const updateRoomChart = () => {
  roomChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}: {c}次'
    },
    grid: { left: '3%', right: '4%', bottom: '20%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: userMeetingData.value.frequentRooms.labels },
    yAxis: { type: 'value', name: '使用次数' },
    series: [{
      name: '使用次数',
      type: 'bar',
      data: userMeetingData.value.frequentRooms.values,
      itemStyle: { color: '#1890ff' },
      barWidth: '60%'
    }]
  })
}

// 更新时间分布图表
const updateTimeChart = () => {
  timeChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c}次会议'
    },
    grid: { left: '3%', right: '4%', bottom: '20%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: userMeetingData.value.timeDistribution.hours
    },
    yAxis: { type: 'value', name: '会议数量' },
    series: [{
      name: '会议数量',
      type: 'line',
      data: userMeetingData.value.timeDistribution.counts,
      smooth: true,
      lineStyle: { color: '#52c41a', width: 3 },
      itemStyle: { color: '#52c41a', size: 6 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(82, 196, 26, 0.3)' },
          { offset: 1, color: 'rgba(82, 196, 26, 0)' }
        ])
      }
    }]
  })
}

// 更新所有图表
const updateCharts = () => {
  updateRoomChart()
  updateTimeChart()
}

// 处理窗口大小变化
const handleResize = () => {
  roomChartInstance?.resize()
  timeChartInstance?.resize()
}

// 退出登录
const handleExit = () => {
  localStorage.removeItem('token')
  router.push('/')
  ElMessage.success('退出登录成功')
}

// 移动端菜单切换
const toggleMenu = () => {
  const menuElement = document.querySelector('.no1')
  const overlayElement = document.querySelector('.menu-overlay') || createOverlay()

  if (menuElement) {
    menuElement.classList.toggle('menu-expanded')
    overlayElement.classList.toggle('menu-overlay-visible')
  }
}

// 创建菜单遮罩
const createOverlay = () => {
  const overlay = document.createElement('div')
  overlay.className = 'menu-overlay'
  overlay.addEventListener('click', toggleMenu)
  document.body.appendChild(overlay)
  return overlay
}

// 页面挂载时加载数据
onMounted(() => {
  loadDataAndInitCharts()
  window.addEventListener('resize', handleResize)
})
</script>

<style  scoped>
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
  overflow: auto;
  padding: 16px;
  gap: 16px;
  position: relative;
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
}

/* 右侧内容区域 */
.no2 {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  padding: 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 容器 */
.container{
  width: 100%;
  max-width: 100%;
  padding: 0;
}

/* 内容容器 - 3D效果 */
.n1{
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  width: 100%;
  padding: 10px;
  border-radius: 16px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
  margin-bottom: 10px;
}

/* 统计卡片样式 */
.stats-row {
  margin-bottom: 10px;
}

/* 统计卡片 - 3D效果 */
.stat-card {
  height: 140px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
  margin-bottom: 10px;
}

.stat-item {
  padding: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transform-style: preserve-3d;
  height: 100%;
}

.stat-icon {
  margin-bottom: 6px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(179, 219, 225, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 3px 8px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform-style: preserve-3d;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: rgb(179, 219, 225);
  margin-bottom: 3px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-label {
  font-size: 12px;
  color: #666;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
  text-align: center;
}

/* 图表区域样式 */
.charts-row {
  margin-bottom: 10px;
}

/* 图表卡片 - 3D效果 */
.chart-card {
  height: 400px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  padding: 10px;
  box-sizing: border-box;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.3);
  font-weight: 600;
  color: #666;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

/* 图表容器 */
.chart-container {
  width: 100%;
  height: 350px;
  padding: 20px;
  overflow-x: auto;
  overflow-y: hidden;
  box-sizing: border-box;
}

/* 图表 */
.chart {
  width: 100%;
  height: 100%;
  min-width: 400px;
  min-height: 300px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.05);
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

/* 按钮样式 */
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

    /* 移动端右侧内容区域 */
    .no2 {
        width: 100% !important;
        margin: 0 !important;
        padding: 0 !important;
        border-radius: 0 !important;
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

    /* 移动端容器 */
    .container {
        padding: 0 !important;
        margin: 0 !important;
    }

    /* 移动端统计卡片行 */
    .stats-row {
        margin-bottom: 8px !important;
        padding: 8px !important;
    }

    /* 移动端统计卡片 */
    .stat-card {
        margin-bottom: 8px !important;
        height: 120px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
        padding: 8px !important;
    }

    /* 移动端图表卡片 */
    .chart-card {
        margin-bottom: 8px !important;
        height: 380px !important;
        padding: 10px !important;
        box-sizing: border-box !important;
    }

    /* 移动端图表容器 */
    .chart-container {
        height: 330px !important;
        padding: 15px !important;
        box-sizing: border-box !important;
    }

    /* 移动端卡片头部 */
    .card-header {
        flex-direction: column !important;
        align-items: flex-start !important;
        gap: 6px !important;
        padding: 6px 8px !important;
    }

    /* 移动端卡片标题 */
    .card-header span {
        font-size: 14px !important;
    }

    /* 移动端统计项 */
    .stat-item {
        padding: 6px !important;
        height: 100% !important;
        display: flex !important;
        flex-direction: column !important;
        align-items: center !important;
        justify-content: center !important;
        text-align: center !important;
    }

    /* 移动端统计图标 */
    .stat-icon {
        margin-bottom: 4px !important;
        width: 30px !important;
        height: 30px !important;
    }

    /* 移动端统计图标内的图标 */
    .stat-icon :deep(.el-icon) {
        font-size: 14px !important;
    }

    /* 移动端统计数字 */
    .stat-number {
        font-size: 18px !important;
        margin-bottom: 2px !important;
        line-height: 1.2 !important;
    }

    /* 移动端统计标签 */
    .stat-label {
        font-size: 10px !important;
        line-height: 1.3 !important;
        height: 30px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
    }

    /* 移动端行间距 */
    :deep(.el-row) {
        margin-bottom: 0 !important;
    }

    /* 移动端列间距 */
    :deep(.el-col) {
        margin-bottom: 8px !important;
    }

    /* 移动端菜单项 */
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
        height: 45px !important;
        line-height: 45px !important;
        padding: 0 12px !important;
        font-size: 13px !important;
    }

    /* 移动端图标 */
    :deep(.el-icon) {
        font-size: 16px !important;
        margin-right: 6px !important;
    }

    /* 移动端内容容器 */
    .n1 {
        padding: 8px !important;
        margin-bottom: 8px !important;
        gap: 6px !important;
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
}
</style>
