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
          <div class="user-info" @click.stop @click="tpinfo">
            <el-avatar class="user-avatar" :src="img" />
            <span class="user-name">{{ name }}</span>
          </div>

          <el-button class="logout-btn" @click="handleExit">退出登录</el-button>
        </div>
      </div>
    </div>
    <div class="main-container">
        <el-menu  :default-active="selectedIndex" @select="handleMenuSelect" :collapse="false" mode="vertical" background-color="#f5f5f5" text-color="black" active-text-color="#87CEEB" width="130px" class="no1">
          <el-menu-item index="1"><el-icon><House /></el-icon><span>首页</span></el-menu-item>
          <el-menu-item index="2"><el-icon><EditPen /></el-icon><span>预约会议</span></el-menu-item>
          <el-menu-item index="3"><el-icon><ChatLineSquare /></el-icon><span>查询预约</span></el-menu-item>
          <el-menu-item index="5"><el-icon><PieChart /></el-icon><span>会议记录</span></el-menu-item>
          <el-menu-item index="4"><el-icon><User /></el-icon><span>个人信息</span></el-menu-item>
          <el-menu-item index="6"><el-icon><Bell /></el-icon><span>消息通知</span></el-menu-item>
          <el-menu-item index="7"><el-icon><Setting /></el-icon><span>会议控制</span></el-menu-item>
        </el-menu>

      <div class="no2">
        <div class="container">
          <!-- 卡片式预约列表 -->
          <div class="booking-list">
            <div
              v-for="(booking, index) in data1.arr"
              :key="booking.reservationId"
              class="booking-card"
            >
              <!-- 卡片头部 -->
              <div class="card-header">
                <h3 class="meeting-title">{{ booking.title }}</h3>
                <span :class="statusClass(booking.status)">{{ booking.status }}</span>
              </div>

              <!-- 预约信息 -->
              <div class="booking-info">
                <!-- 会议室基本信息 -->
                <div class="room-info">
                  <div class="room-icon">
                    <el-icon class="building-icon"><OfficeBuilding /></el-icon>
                  </div>
                  <div class="room-details">
                    <div class="room-name">{{ booking.roomName || '未知位置' }}</div>
                    <div class="room-number">位置：{{ booking.location || 'N/A' }}</div>
                    <div class="room-type">预约用途：<el-icon><BottomRight /></el-icon></div>
                  </div>
                </div>

                <!-- 会议时间信息 -->
                <div class="time-info">
                  <div class="time-item">
                    <el-icon class="time-icon"><Calendar /></el-icon>
                    <div class="time-label">日期</div>
                    <div class="time-value">{{ booking.reserveDate }}</div>
                  </div>
                  <div class="time-item">
                    <el-icon class="time-icon"><Clock /></el-icon>
                    <div class="time-label">时间</div>
                    <div class="time-value">{{ booking.startTime }} - {{ booking.endTime }}</div>
                  </div>

                </div>
              </div>

              <!-- 会议用途 -->
              <div class="meeting-purpose">
                <el-icon class="purpose-icon"><Document /></el-icon>
                <div class="purpose-content">{{ booking.purpose }}</div>
              </div>

              <!-- 操作按钮 -->
              <div class="booking-actions">
                <el-button
                  :type="getButtonType(booking)"
                  @click="handleBookingOperate(index, booking)"
                  class="action-button"
                  :icon="getButtonText(booking) === '取消申请' ? 'Close' : 'Delete'"
                >
                  {{ getButtonText(booking) }}
                </el-button>
              </div>
            </div>

            <!-- 空数据提示 -->
            <div v-if="data1.arr.length === 0" class="empty-state">
              <el-empty description="暂无预约记录"></el-empty>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElEmpty } from 'element-plus'
// 引入接口
import { getUserInfo, getBookingRecords, cancelBooking } from '@/apis/usercheck.js'

const router = useRouter()

// 菜单选择
  const handleMenuSelect = (menuIndex) => {
    switch (menuIndex) {
      case "1": router.push('/user/home'); break
      case "2": router.push('/user/appoint'); break
      case "3": router.push('/user/check'); break
      case "4": router.push('/user/info'); break
      case "5": router.push('/user/meetingMinu'); break
      case "6": router.push('/user/notifications'); break
      case "7": router.push('/user/control'); break
      default: router.push('/user/home')
    }
  }

const selectedIndex = ref("3")

// 右上角用户信息
const img = ref('/lsj.jpg') // 默认头像
const name = ref('临时测试用户') // 默认名称

// 预约记录数据（从接口获取）
const data1 = ref({ arr: [] })

// 格式化时间显示
const formatTime = (date, startTime, endTime) => {
  if (!date || !startTime || !endTime) return '时间信息不全'
  return `${date} ${startTime} - ${endTime}`
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString()
}

// 根据状态返回样式类
const statusClass = (status) => {
  switch(status) {
    case 0: // 待审批
      return 'status-pending';
    case 2: // 已通过
      return 'status-success';
    case 1: // 已拒绝
      return 'status-rejected';
    case 3: // 已取消
      return 'status-canceled';
    default:
      return 'status-unknown';
  }
}

// 新增：判断按钮显示文本
const getButtonText = (booking) => {
  // 拼接预约开始时间（兼容接口返回的日期+时间格式）
  const meetingStartTime = new Date(`${booking.reserveDate}T${booking.startTime}`);
  const now = new Date();
  // 当前时间在开始时间前 → 取消申请；之后 → 删除记录
  return now < meetingStartTime ? '取消申请' : '删除记录';
}

// 新增：判断按钮样式类型
const getButtonType = (booking) => {
  return getButtonText(booking) === '取消申请' ? 'warning' : 'danger';
}

// 新增：按钮点击统一处理
const handleBookingOperate = async (index, booking) => {
  const buttonText = getButtonText(booking);
  // 二次确认
  if (!confirm(`确定要${buttonText}预约 #${booking.reservationId} 吗？`)) {
    return;
  }

  try {
    // 这里可根据按钮类型区分接口（当前先复用原取消接口，后续可扩展删除接口）
    const res = await cancelBooking(booking.reservationId);
    if (res.data.code === 1 && res.data.message === 'success') {
      data1.value.arr.splice(index, 1);
      ElMessage.success(`已${buttonText}预约 #${booking.reservationId}`);
    } else {
      ElMessage.error(res.data.message || `${buttonText}失败`);
    }
  } catch (err) {
    ElMessage.error(`网络异常，${buttonText}失败`);
    console.error(err);
  }
}
const tpinfo=()=>{
  router.push('./info')
}
// 退出登录
const handleExit = () => {
  localStorage.removeItem('sessionId');
  localStorage.removeItem('token');
  router.push('/');
}

// 移动端菜单切换
const toggleMenu = () => {
  const menuElement = document.querySelector('.no1');
  const overlayElement = document.querySelector('.menu-overlay') || createOverlay();

  if (menuElement) {
    menuElement.classList.toggle('menu-expanded');
    overlayElement.classList.toggle('menu-overlay-visible');
  }
};

// 创建菜单遮罩
const createOverlay = () => {
  const overlay = document.createElement('div');
  overlay.className = 'menu-overlay';
  overlay.addEventListener('click', toggleMenu);
  document.body.appendChild(overlay);
  return overlay;
};

// 页面挂载时加载数据
onMounted(async () => {
  try {
    // 1. 获取用户信息（头像、名称）
    try {
      const userRes = await getUserInfo();
      if (userRes.data.code === 1 && userRes.data.message === 'success') {
        const user = userRes.data.data;
        name.value = user.username || '临时测试用户';
        img.value = user.thumbnailUrl || '/lsj.jpg';
      }
    } catch  {
      // 使用默认用户信息
      name.value = '临时测试用户';
      img.value = '/lsj.jpg';
    }

    // 2. 获取预约记录
    try {
      // 调用接口获取预约记录
      const res = await getBookingRecords();
      const records = res.data.data || [];
      console.log("...",records)

      // 适配接口返回的数据结构
      data1.value.arr = records.map(item => ({
        ...item,
        // 补充显示需要的默认值
        location: item.location || '未知位置',
        type: item.type || '普通会议室',
        title: item.title || '会议'
      }));
    } catch (recordsErr) {
      // 接口失败时使用空数组
      data1.value.arr = [];
      console.error('获取预约记录失败:', recordsErr);
      ElMessage.error('获取预约记录失败，请重试');
    }

    ElMessage.closeAll();
  } catch (err) {
    ElMessage.error('加载数据失败，请重试');
    console.error(err);
  }
})
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
  overflow: auto;
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* 容器 */
.container{
  width: 100%;
  max-width: 1350px;
  margin: 0 auto;
}

/* 预约列表样式 */
.booking-list {
  width: 100%;
}

/* 预约卡片 - 现代化设计 */
.booking-card {
  margin-bottom: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.95) 100%);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(210, 230, 235, 0.6);
  border-radius: 20px;
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transform-style: preserve-3d;
  transition: all 0.3s ease;
  transform: translateZ(0);

  &:hover {
    transform: translateY(-4px) translateZ(0);
    box-shadow:
      0 20px 40px rgba(179, 219, 225, 0.2),
      0 0 0 1px rgba(255, 255, 255, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
  }
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 0;
}

.card-header .meeting-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2d3748;
  background: linear-gradient(135deg, #4a5568, #2d3748);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.card-header span {
  font-size: 14px;
  font-weight: 600;
  padding: 6px 16px;
  border-radius: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 预约信息 */
.booking-info {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
  padding: 20px;
  gap: 20px;
  background: linear-gradient(135deg, rgba(179, 219, 225, 0.05), rgba(244, 162, 175, 0.05));
  border-radius: 16px;
  margin: 10px 20px;
  border: 1px solid rgba(210, 230, 235, 0.4);
}

/* 预约详情 */
.booking-details {
  flex: 1;
}

/* 预约基本信息 */
.booking-basic-info {
  margin: 10px 0;
}

.booking-basic-info p {
  margin: 8px 0;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.booking-basic-info p:nth-child(5) {
  color: #666; /* 描述文字颜色稍浅 */
}

.booking-basic-info i {
  margin-right: 8px;
  color: rgb(179, 219, 225);
  font-size: 16px;
}

/* 状态样式 */
.status-pending {
  color: #d69e2e;
  background: linear-gradient(135deg, rgba(250, 204, 21, 0.15), rgba(250, 204, 21, 0.05));
  border: 1px solid rgba(250, 204, 21, 0.3);
  box-shadow: 0 2px 8px rgba(250, 204, 21, 0.15);
}

.status-success {
  color: #38a169;
  background: linear-gradient(135deg, rgba(48, 209, 88, 0.15), rgba(48, 209, 88, 0.05));
  border: 1px solid rgba(48, 209, 88, 0.3);
  box-shadow: 0 2px 8px rgba(48, 209, 88, 0.15);
}

.status-rejected {
  color: #e53e3e;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(239, 68, 68, 0.05));
  border: 1px solid rgba(239, 68, 68, 0.3);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.status-canceled {
  color: #718096;
  background: linear-gradient(135deg, rgba(147, 197, 253, 0.15), rgba(147, 197, 253, 0.05));
  border: 1px solid rgba(147, 197, 253, 0.3);
  box-shadow: 0 2px 8px rgba(147, 197, 253, 0.15);
}

.status-unknown {
  color: #a0aec0;
  background: linear-gradient(135deg, rgba(203, 213, 225, 0.15), rgba(203, 213, 225, 0.05));
  border: 1px solid rgba(203, 213, 225, 0.3);
  box-shadow: 0 2px 8px rgba(203, 213, 225, 0.15);
}

/* 会议室基本信息 */
.room-info {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  flex: 1;
}

.room-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(179, 219, 225, 0.3), rgba(179, 219, 225, 0.1));
  border: 2px solid rgba(179, 219, 225, 0.4);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(179, 219, 225, 0.2);
}

.room-icon:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
}

.building-icon {
  font-size: 28px;
  color: rgba(179, 219, 225, 0.8);
}

.room-details {
  flex: 1;
}

.room-name {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 5px;
}

.room-number, .room-type {
  font-size: 14px;
  color: #718096;
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 会议时间信息 */
.time-info {
  display: flex;
  gap: 25px;
  flex: 1;
  flex-wrap: wrap;
}

.time-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(210, 230, 235, 0.4);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.time-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(179, 219, 225, 0.2);
  border-color: rgba(179, 219, 225, 0.6);
}

.time-item .time-icon {
  font-size: 18px;
  color: rgba(179, 219, 225, 0.8);
  margin-right: 0;
}

.time-label {
  font-size: 12px;
  color: #a0aec0;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.time-value {
  font-size: 15px;
  font-weight: 700;
  color: #2d3748;
}

/* 会议用途 */
.meeting-purpose {
  padding: 0 20px 20px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: -10px;
}

.meeting-purpose .purpose-icon {
  font-size: 20px;
  color: rgba(179, 219, 225, 0.8);
  margin-top: 2px;
  flex-shrink: 0;
}

.purpose-content {
  flex: 1;
  font-size: 15px;
  color: #4a5568;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.8);
  padding: 12px 16px;
  border: 1px solid rgba(210, 230, 235, 0.4);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 操作按钮样式 */
.booking-actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  padding: 0 20px 20px;
}

/* 按钮样式 */
:deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.8));
  border: 2px solid rgba(179, 219, 225, 0.3);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  font-weight: 600;
  padding: 8px 20px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 20px rgba(179, 219, 225, 0.4);
    transform: translateY(-2px);
    border-color: rgba(179, 219, 225, 0.6);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 8px rgba(179, 219, 225, 0.3);
  }
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, rgba(250, 204, 21, 0.1), rgba(250, 204, 21, 0.05));
  border-color: rgba(250, 204, 21, 0.3);
  color: #ffffff;

  &:hover {
    background: linear-gradient(135deg, rgba(250, 204, 21, 0.2), rgba(250, 204, 21, 0.1));
    box-shadow: 0 8px 20px rgba(250, 204, 21, 0.3);
    border-color: rgba(250, 204, 21, 0.6);
  }
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.05));
  border-color: rgba(239, 68, 68, 0.3);
  color: #ffffff;

  &:hover {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(239, 68, 68, 0.1));
    box-shadow: 0 8px 20px rgba(239, 68, 68, 0.3);
    border-color: rgba(239, 68, 68, 0.6);
  }
}

/* 空状态样式 */
.empty-state {
  padding: 60px 40px;
  text-align: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(210, 230, 235, 0.6);
  border-radius: 20px;
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transform-style: preserve-3d;
  transition: all 0.3s ease;
  margin: 20px;

  :deep(.el-empty__description) {
    color: #718096;
    font-size: 16px;
    margin-top: 10px;
  }
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

/* 图标样式 */
:deep(.el-icon) {
  font-size: 22px;
  margin-right: 12px;
  color: rgb(179, 219, 225);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

/* 响应式调整 */
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

  /* 移动端预约列表 */
  .booking-list {
    gap: 8px !important;
  }

  /* 移动端预约卡片 */
  .booking-card {
    padding: 8px !important;
    margin: 0 !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    border: none !important;
  }

  /* 移动端卡片头部 */
  .card-header {
    flex-direction: row !important;
    align-items: center !important;
    justify-content: space-between !important;
    gap: 8px !important;
    padding: 8px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }

  /* 移动端会议标题 */
  .meeting-title {
    font-size: 16px !important;
    margin: 0 !important;
    flex: 1 !important;
    min-width: 0 !important;
    white-space: nowrap !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
  }

  /* 移动端状态标签 */
  .card-header span {
    font-size: 12px !important;
    padding: 4px 8px !important;
    border-radius: 12px !important;
    flex-shrink: 0 !important;
  }

  /* 移动端预约信息 */
  .booking-info {
    flex-direction: column !important;
    gap: 8px !important;
    padding: 8px !important;
    margin: 0 !important;
  }

  /* 移动端时间信息 */
  .time-info {
    flex-direction: row !important;
    gap: 8px !important;
    width: 100% !important;
  }

  /* 移动端时间项 */
  .time-item {
    flex: 1 !important;
    min-width: 0 !important;
    padding: 6px 8px !important;
    margin: 0 !important;
  }

  /* 移动端时间标签 */
  .time-label {
    font-size: 10px !important;
  }

  /* 移动端时间值 */
  .time-value {
    font-size: 12px !important;
  }

  /* 移动端操作按钮 */
  .booking-actions {
    flex-direction: row !important;
    gap: 8px !important;
    padding: 6px 8px !important;
    margin: 0 !important;
  }

  /* 移动端时间项 */
  .time-item {
    padding: 6px 8px !important;
    margin: 0 !important;
  }

  /* 移动端会议用途 */
  .meeting-purpose {
    padding: 6px 8px !important;
    margin: 0 !important;
    display: flex !important;
    align-items: center !important;
    gap: 8px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }

  /* 移动端会议用途内容 */
  .purpose-content {
    padding: 6px 8px !important;
    margin: 0 !important;
    flex: 1 !important;
    min-width: 0 !important;
    font-size: 12px !important;
    line-height: 1.4 !important;
  }

  /* 移动端操作按钮 */
  .action-button {
    flex: 1 !important;
    min-width: 0 !important;
    padding: 6px 8px !important;
    font-size: 12px !important;
    height: 32px !important;
  }

  /* 移动端容器 */
  .container {
    padding: 0 !important;
    margin: 0 !important;
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

  /* 移动端会议室信息 */
  .room-info {
    flex-direction: row !important;
    align-items: center !important;
    gap: 8px !important;
    padding: 6px 8px !important;
    margin: 0 !important;
    flex-wrap: nowrap !important;
  }

  /* 移动端会议室详情 */
  .room-details {
    margin-left: 0 !important;
    padding: 0 !important;
    margin: 0 !important;
    flex: 1 !important;
    min-width: 0 !important;
  }

  /* 移动端会议室图标 */
  .room-icon {
    width: 36px !important;
    height: 36px !important;
    flex-shrink: 0 !important;
  }

  /* 移动端建筑图标 */
  .building-icon {
    font-size: 18px !important;
  }

  /* 移动端会议室名称 */
  .room-name {
    font-size: 14px !important;
    margin-bottom: 2px !important;
    font-weight: 600 !important;
  }

  /* 移动端会议室位置和类型 */
  .room-number, .room-type {
    font-size: 12px !important;
    margin-bottom: 0 !important;
    line-height: 1.2 !important;
  }

  /* 移动端时间标签 */
  .time-label {
    font-size: 10px !important;
  }

  /* 移动端时间值 */
  .time-value {
    font-size: 12px !important;
  }

  /* 移动端会议标题 */
  .meeting-title {
    font-size: 16px !important;
    margin: 0 !important;
  }

  /* 移动端会议用途内容 */
  .purpose-content {
    font-size: 12px !important;
    line-height: 1.4 !important;
  }

  /* 移动端预约图片 */
  .booking-image {
    margin-bottom: 8px !important;
    margin-right: 0 !important;
    align-self: center !important;
    width: 60px !important;
    height: 60px !important;
  }
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
</style>
