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
            <span class="user-name" >{{ name }}</span>
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
          <div class="n1">
          <div style="display: flex; gap: 10px; margin-bottom: 10px;">
            <!-- 日期选择：选择后加载对应日期的会议室 -->
            <el-date-picker
              v-model="date"
              type="date"
              placeholder="请选择日期"
              @change="fetchMeetingRooms"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="flex: 1;"
            ></el-date-picker>
            <el-select v-model="selected2" placeholder="请选择类型" style="flex: 1;" @change="filterBytype" clearable>
              <el-option value="" label="全部"></el-option>
              <el-option value="普通会议室" label="普通会议室"></el-option>
              <el-option value="多媒体会议室" label="多媒体会议室"></el-option>
              <el-option value="主席台" label="主席台"></el-option>
            </el-select>
          </div>
          <div style="display: flex; gap: 10px;">
            <!-- 修改：规模筛选改为输入数字（显示容纳人数大于该值的会议室） -->
            <el-input
              v-model.number="inputCapacity"
              type="number"
              placeholder="请输入最小容纳人数"
              style="flex: 1;"
              clearable
              @input="filterByCapacity"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
            <el-input
              v-model="searchName"
              placeholder="请输入会议室名称"
              style="flex: 1;"
              clearable
              @input="filterByName"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

          <!-- 会议室列表 -->
          <div class="room-list">
            <el-card
              v-for="(room, index) in filteredData"
              :key="room.roomId || index"
              class="room-card"
            >
              <!-- 会议室信息 -->
            <div class="room-info">
              <!-- 会议室名称和状态 -->
              <div class="room-header">
                <h3>{{ room.roomName }}</h3>
                <span class="room-status">{{ room.isAvailable ? '可预约' : '不可用' }}</span>
              </div>

              <div class="room-content">
                <!-- 会议室图片区域 -->
                <div class="room-image-section">
                  <el-image
                    :src="room.originImage"
                    class="room-image"
                    fit="cover"
                    :preview-src-list="[room.originImage]"
                    v-if="room.originImage"
                  ></el-image>
                  <div class="no-image" v-else>
                    <i class="el-icon-picture-outline"></i>
                    <span>无图片</span>
                  </div>
                </div>

                <div class="room-details">
                  <!-- 会议室基本信息 -->
                  <div class="room-basic-info">
                    <div class="info-row">
                      <p><i class="el-icon-location-outline"></i> 地址：{{ room.location }}</p>
                      <p><i class="el-icon-office-building"></i> 类型：{{ room.type || '普通会议室' }}</p>
                    </div>
                    <div class="info-row">
                      <p><i class="el-icon-user"></i> 容纳人数：{{ room.capacity }}人</p>
                      <p><i class="el-icon-clock"></i> 开放时间：{{ room.openTime }}-{{ room.closeTime }}</p>
                    </div>
                    <div class="info-row">
                      <p><i class="el-icon-office-building"></i> 编号：{{ room.roomNumber }}</p>
                      <p><i class="el-icon-s-promotion"></i> 设备：{{ room.equipment || '基础设备' }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

              <!-- 可预约时段 -->
              <div class="time-slots">
                <div class="time-legend">
                  <span class="legend-item"><span class="dot unavailable"></span> 不可预约时段</span>
                  <span class="legend-item"><span class="dot available"></span> 可预约时段</span>
                  <span class="legend-item"><span class="dot selected"></span> 已选时段</span>
                </div>

                <!-- 已选时间段 -->
                <div v-if="room.selectedHours.length > 0" class="selected-time-info">
                  <span class="info-label">已选时段：</span>
                  <span class="time-ranges">{{ formatSelectedTimeRanges(room.selectedHours) }}</span>
                  <span class="time-duration">(时长：{{ room.selectedHours.length-1 }}小时)</span>
                </div>

                <div class="slots-container">
                  <!-- 时段范围：0-23小时 -->
                  <div
                    v-for="hour in Array.from({length:24}, (_, index) => index)"
                    :key="hour"
                    class="time-slot"
                    :class="{
                      'unavailable': !room.availableHours.includes(hour) || !room.isAvailable,
                      'available': room.availableHours.includes(hour) && !room.selectedHours.includes(hour) && room.isAvailable,
                      'selectable': room.isBookingMode && room.availableHours.includes(hour) && room.isAvailable,
                      'selected': room.isBookingMode && room.selectedHours.includes(hour) && room.isAvailable,
                      'invalid': room.isBookingMode && room.selectedHours.length > 0 && !isValidSelection(room, hour)
                    }"
                    @click="handleTimeSlotClick(room, hour)"
                  >
                    {{ hour }}:00
                  </div>
                </div>

                <div class="time-tip">
                  <i class="el-icon-info"></i> 请选择<strong>连续</strong>的时间段
                </div>
              </div>

              <!-- 预约按钮 -->
              <el-button
                class="book-button"
                type="primary"
                size="medium"
                @click="toggleBookingMode(room)"
                :disabled="!room.isAvailable"
              >
                {{ room.isBookingMode ? '取消预约' : '预约' }}
              </el-button>

              <!-- 预约信息表单 -->
              <el-form
                v-if="room.isBookingMode"
                :model="room.bookingForm"
                class="booking-form"
                label-width="80px"
              >
                <el-form-item label="会议主题" prop="title" :rules="[{ required: true, message: '请输入会议主题', trigger: 'blur' }]">
                  <el-input v-model="room.bookingForm.title"></el-input>
                </el-form-item>
                <el-form-item label="会议人数" prop="people" :rules="[{ required: true, message: '请输入会议人数', trigger: 'blur' }]">
                  <el-input v-model.number="room.bookingForm.people"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="success" @click="submitBooking(room)" :disabled="!isValidBooking(room)">提交预约</el-button>
                  <el-button type="default" @click="resetSelection(room)">重置选择</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 导入API
import { getUserInfo, getMeetingRooms, submitAppointment } from '@/apis/userappointAPI.js'


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

const selectedIndex = ref("2") // 默认选中“预约会议”
const date = ref('') // 选择的日期（已通过value-format确保为YYYY-MM-DD字符串）
const selected2 = ref('') // 类型筛选
const inputCapacity = ref('') // 修改：规模筛选（用户输入的最小容纳人数）
const searchName = ref('') // 编号搜索
// 会议室列表（从接口获取）
const tableData = ref([])


// 筛选条件
const filters = ref({ type: '', capacity: '', name: '' })
// 按类型筛选
const filterBytype = (type) => { filters.value.type = type }
// 修改：按规模筛选（容纳人数大于输入值）
const filterByCapacity = () => {
  filters.value.capacity = inputCapacity.value ? Number(inputCapacity.value) : '';
}
// 按编号筛选
const filterByName = () => { filters.value.name = searchName.value }

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(item => {
    const typeMatch = !filters.value.type || item.type === filters.value.type
    const capacityMatch = !filters.value.capacity || item.capacity > Number(filters.value.capacity)
    const nameMatch = !filters.value.name || item.roomName.toString().includes(filters.value.name)
    return typeMatch && capacityMatch && nameMatch
  })
})


// 切换预约模式
const toggleBookingMode = (room) => {
  room.isBookingMode = !room.isBookingMode;
  if (!room.isBookingMode) room.selectedHours = [];
};

// 验证时间段连续性
const isValidSelection = (room, hour) => {
  if (room.selectedHours.length === 0) return true;
  const sorted = [...room.selectedHours].sort((a, b) => a - b);
  return hour >= sorted[0] - 1 && hour <= sorted[sorted.length - 1] + 1;
};

// 选择时段
const handleTimeSlotClick = (room, hour) => {
  if (!room.isBookingMode || !room.availableHours.includes(hour) || !room.isAvailable) return;

  if (room.selectedHours.length > 0 && !isValidSelection(room, hour)) {
    ElMessage.warning("请选择连续的时间段！");
    return;
  }

  const index = room.selectedHours.indexOf(hour);
  if (index > -1) {
    const isMiddle = room.selectedHours.includes(hour - 1) && room.selectedHours.includes(hour + 1);
    isMiddle ? (ElMessage.warning("不能取消中间时段，请重新选择！"), room.selectedHours = []) : room.selectedHours.splice(index, 1);
  } else {
    room.selectedHours.push(hour);
    room.selectedHours.sort((a, b) => a - b);
  }
};

// 验证预约有效性
const isValidBooking = (room) => { return room.selectedHours.length >= 2 };
// 重置选择
const resetSelection = (room) => { room.selectedHours = []; };

// 提交预约（对接接口）
const submitBooking = async (room) => {
  console.log(room)

  if (!isValidBooking(room)) {
    ElMessage.warning("请选择至少2小时的连续时间段");
    return;
  }

  if (!room.bookingForm.title || !room.bookingForm.people) {
    ElMessage.warning("请填写会议主题和人数");
    return;
  }

  // 构造符合接口要求的时间格式（YYYY-MM-DD HH:mm:ss）
  const formatTime = (hour) => {
    return `${hour.toString().padStart(2, '0')}:00:00`;
  };

  // 构造后端需要的预约数据（严格贴合接口文档的ReservationDto）
  const appointData = {
    roomId: room.roomId,          // 接口必传：会议室ID
    date: date.value,             // 接口必传：预约日期（YYYY-MM-DD）
    startTime: formatTime(room.selectedHours[0]), // 接口必传：开始时间
    endTime: formatTime(room.selectedHours[room.selectedHours.length - 1]), // 接口必传：结束时间
    purpose: room.bookingForm.reason, // 接口必传：预约事由,
    location:room.location,
    roomName:room.roomName,
    type:room.type,
  };

  try {
    const res = await submitAppointment(appointData);
    console.log("Res",res)
    if (res.data.code === 1 && res.data.message === 'success') {
      console.log("预约成功",res)
      ElMessage.success(`预约成功！您已预约${room.roomNumber}号会议室的${formatSelectedTimeRanges(room.selectedHours)}时段`);
      // 重置当前会议室的预约状态
      room.isBookingMode = false;
      room.selectedHours = [];
      room.bookingForm = { title: "", people: "", reason: "" };
      // 重新加载会议室列表（更新可预约时段）
      await fetchMeetingRooms();
    }else if(res.data.message=='预约时间与已有预约冲突，请选择其他时间段'){
      ElMessage.error('预约时间与已有预约冲突，请选择其他时间段');
      setTimeout(() => {
      router.go(0);
    }, 2000);
      console.log("111",res.data)
    }
     else {
      ElMessage.error(res.data.message || '预约失败，请重试');
    }
  } catch (err) {
    ElMessage.error('网络异常，预约失败');
    console.error('预约错误详情:', err);
  }
};


// 格式化已选时段
const formatSelectedTimeRanges = (hours) => {
  if (hours.length === 0) return '';
  const sorted = [...hours].sort((a, b) => a - b);
  return sorted.length === 1 ? `${sorted[0]}:00` : `${sorted[0]}:00-${sorted[sorted.length - 1]}:00`;
};


// 时段范围样式判断
const isFirstInRange = (room, hour) => room.selectedHours.includes(hour) && !room.selectedHours.includes(hour - 1);
const isLastInRange = (room, hour) => room.selectedHours.includes(hour) && !room.selectedHours.includes(hour + 1);
const isMiddleInRange = (room, hour) => room.selectedHours.includes(hour) && room.selectedHours.includes(hour - 1) && room.selectedHours.includes(hour + 1);


// 右上角用户信息
const img = ref('/lsj.jpg') // 默认头像
const name = ref('临时测试用户') // 默认名称
// 退出登录
const handleExit = () => {
  localStorage.removeItem('sessionId');
  localStorage.removeItem('token');
  router.push('/');
};

// 移动端菜单切换
const toggleMenu = () => {
  const menuElement = document.querySelector('.no1');
  const overlayElement = document.querySelector('.menu-overlay') || createOverlay();

  if (menuElement) {
    menuElement.classList.toggle('menu-expanded');
    overlayElement.classList.toggle('menu-overlay-visible');
  }
}

// 创建菜单遮罩
const createOverlay = () => {
  const overlay = document.createElement('div');
  overlay.className = 'menu-overlay';
  overlay.addEventListener('click', toggleMenu);
  document.body.appendChild(overlay);
  return overlay;
}

const tpinfo=()=>{
  router.push('./info')
}
// 页面加载时执行
onMounted(async () => {
  // 1. 获取用户信息（头像、名称）
  try {
    const userRes = await getUserInfo();
    if (userRes.data.code === 1 && userRes.data.message === 'success') {
      const user = userRes.data.data;
      name.value =  user.username || '临时测试用户';
      img.value = user.thumbnailUrl || user.originUrl || '/lsj.jpg';
    }
  } catch (err) {
    ElMessage.error('获取用户信息失败');
    console.error(err);
  }
  const now = new Date();
  const pad = (n) => (n < 10 ? '0' + n : '' + n);
  date.value = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}`;
  await fetchMeetingRooms();
});

// 加载指定日期的会议室列表
const fetchMeetingRooms = async () => {
  try {
    const roomRes = await getMeetingRooms(date.value);
    console.log("reswww",roomRes)

    // 验证接口返回有效性
    if (roomRes.data.code !== 1 || roomRes.data.message !== 'success') {
      ElMessage.error(roomRes.data.message || '获取会议室失败');
      return;
    }
    const roomData = roomRes.data.data || [];
    if (!Array.isArray(roomData)) {
      ElMessage.error('会议室数据格式错误');
      return;
    }

    // 处理会议室数据：优先使用 availableSlots；若为空则基于 openTime/closeTime 计算可用时段
    tableData.value = roomData.map(room => {
      const availableHours = [];
      if (Array.isArray(room.availableSlots)) {
        room.availableSlots.forEach(slot => {
          const slotStartHour = parseInt(String(slot.startTime || '').split(':')[0]) || 8;
          const slotEndHour = parseInt(String(slot.endTime || '').split(':')[0]) || 18;
          for (let i = slotStartHour; i < slotEndHour; i++) availableHours.push(i);
        });
      } else {
        const startHour = parseInt(String(room.openTime || '').split(':')[0]) || 8;
        const endHour = parseInt(String(room.closeTime || '').split(':')[0]) || 18;
        for (let i = startHour; i < endHour; i++) availableHours.push(i);
      }

      return {
        ...room,
        availableHours: availableHours,
        isAvailable: room.isAvailable === true,
        isBookingMode: false,
        selectedHours: [],
        bookingForm: { title: "", people: "", reason: "" }
      };
    });

  } catch (err) {
    console.error('获取会议室列表失败:', err);
    ElMessage.error('获取会议室列表失败，请重试');
    // 接口失败时使用空数组
    tableData.value = [];
  }
};
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

/* 主容器 */
.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 0;
  gap: 0;
}

/* 右侧内容区域 */
.no2 {
  flex: 1;
  padding: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 0;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
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
  transform-style: preserve-3d;
  transition: all 0.3s ease;
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
  display: flex;
  flex-direction: column;
}

/* 容器 */
.container{
  width: 100%;
  max-width: 100%;
  padding: 20px 0;
}

/* 筛选栏 */
.n1{
  width: 100%;
  min-height: 100px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  margin-bottom: 20px;
  gap: 10px;
  padding: 15px 20px;
  transform-style: preserve-3d;
  box-sizing: border-box;
}

/* 筛选行 */
.filter-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

/* 会议室列表 */
.room-list {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: 24px;
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

  /* 移动端菜单切换按钮 */
  .mobile-menu-toggle {
    display: flex !important;
    align-items: center;
    justify-content: center;
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
    border: none !important;
    border-radius: 0 !important;
    padding: 0 !important;
  }

  /* 移动端筛选栏 */
  .n1 {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    padding: 15px;
    min-height: auto;
  }

  /* 确保移动端筛选行保持为flex布局 */
  .filter-row {
    display: flex !important;
    gap: 8px !important;
    flex-wrap: nowrap !important;
  }

  /* 移动端筛选控件 */
  .filter-row .el-date-picker,
  .filter-row .el-select,
  .filter-row .el-input {
    flex: 1 !important;
    width: calc(50% - 4px) !important;
    min-width: calc(50% - 4px) !important;
    max-width: calc(50% - 4px) !important;
  }

  /* 确保Element Plus组件在移动端正确显示 */
  .filter-row :deep(.el-input__wrapper),
  .filter-row :deep(.el-select__wrapper),
  .filter-row :deep(.el-date-editor) {
    width: 100% !important;
  }

  /* 移动端会议室列表 */
  .room-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  /* 移动端容器内边距 */
  .container {
    padding: 0 !important;
    margin: 0 !important;
  }

  /* 移动端会议室信息 */
  .room-info {
    padding: 15px;
    flex-direction: column;
    align-items: center;
  }

  /* 移动端会议室图片区域 */
  .room-image-section {
    width: 120px;
    height: 120px;
    margin-bottom: 15px;
  }

  /* 移动端无图片区域 */
  .no-image {
    width: 120px;
    height: 120px;
  }

  /* 移动端会议室详情 */
  .room-details {
    width: 100%;
  }

  /* 移动端时间段容器 */
  .slots-container {
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
  }

  /* 移动端时间段 */
  .time-slot {
    height: 35px;
    font-size: 11px;
  }

  /* 移动端筛选栏 */
  .filter-row {
    flex-direction: column;
    gap: 10px;
  }

  /* 移动端筛选控件 */
  .filter-row .el-select,
  .filter-row .el-input {
    width: 100% !important;
    margin-right: 0 !important;
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

  /* 移动端时间段容器 */
  .slots-container {
    display: flex !important;
    flex-wrap: wrap !important;
    gap: 5px !important;
    margin-bottom: 10px !important;
    padding: 10px !important;
    background: rgba(240, 244, 255, 0.5) !important;
    border-radius: 8px !important;
  }

  /* 移动端时间段 */
  .time-slot {
    width: calc(25% - 5px) !important;
    height: 40px !important;
    font-size: 12px !important;
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

  /* 移动端会议室卡片 */
  .room-card {
    margin: 0 !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    border: none !important;
    background: rgba(255, 255, 255, 1) !important;
  }

  /* 移动端会议室信息 */
  .room-info {
    padding: 10px !important;
    gap: 10px !important;
    margin-bottom: 10px !important;
    flex-direction: column !important;
    align-items: flex-start !important;
  }

  /* 移动端会议室内容区域 */
  .room-content {
    flex-direction: row !important;
    gap: 15px !important;
    align-items: center !important;
  }

  /* 移动端会议室图片区域 */
  .room-image-section {
    width: 80px !important;
    height: 80px !important;
    margin-bottom: 0 !important;
  }

  /* 移动端无图片区域 */
  .no-image {
    width: 80px !important;
    height: 80px !important;
  }

  /* 移动端会议室详情 */
  .room-details {
    flex: 1;
    gap: 8px !important;
  }

  /* 移动端会议室名称和状态 */
  .room-header {
    margin-bottom: 5px !important;
    width: 100% !important;
  }

  .room-header h3 {
    font-size: 16px !important;
  }

  .room-status {
    font-size: 11px !important;
    padding: 3px 10px !important;
  }

  /* 移动端会议室基本信息 */
  .room-basic-info p {
    padding: 6px 10px !important;
    margin: 0 !important;
    min-width: calc(50% - 5px) !important;
    max-width: calc(50% - 5px) !important;
    font-size: 11px !important;
  }

  /* 移动端信息行 */
  .info-row {
    gap: 8px !important;
  }

  /* 移动端时间段容器 */
  .time-slots {
    margin: 0 10px 10px !important;
    padding: 10px !important;
    border-radius: 8px !important;
  }

  .slots-container {
    gap: 5px !important;
    margin-bottom: 10px !important;
    padding: 10px !important;
    border-radius: 8px !important;
  }

  /* 移动端预约按钮 */
  .book-button {
    margin: 0 10px 10px !important;
    width: calc(100% - 20px) !important;
  }

  /* 移动端预约表单 */
  .booking-form {
    margin: 0 10px 10px !important;
    padding: 10px !important;
    border-radius: 8px !important;
  }
}

/* 会议室卡片 - 精美静态卡片 */
.room-card {
  overflow: hidden;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.room-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 会议室信息 */
.room-info {
  display: flex;
  flex-direction: column;
  margin-bottom: 15px;
  padding: 20px;
  gap: 15px;
  align-items: flex-start;
}

/* 会议室内容区域 */
.room-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  width: 100%;
}

/* 会议室详情 */
.room-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 会议室图片区域 */
.room-image-section {
  width: 100px;
  height: 100px;
  overflow: hidden;
  border-radius: 12px;
  background: #f5f7fa;
  position: relative;
  flex-shrink: 0;
}

/* 会议室图片 */
.room-image {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.room-image:hover {
  transform: scale(1.05);
}

/* 无图片 */
.no-image {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  border: 1px solid rgba(179, 219, 225, 0.3);
}

.no-image i {
  font-size: 32px;
  margin-bottom: 8px;
  color: #c0c4cc;
  transition: all 0.3s ease;
}

.no-image:hover i {
  transform: scale(1.1);
  color: #909399;
}

/* 会议室详情 */
.room-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 会议室名称和状态 */
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.room-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 0.5px;
}

.room-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(179, 219, 225, 0.15);
  color: rgb(179, 219, 225);
}

/* 会议室基本信息 */
.room-basic-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 5px 0;
}

.info-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  width: 100%;
}

.room-basic-info p {
  margin: 0;
  padding: 6px 10px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  background: rgba(240, 244, 255, 0.6);
  border-radius: 8px;
  transition: all 0.3s ease;
  flex: 1;
  min-width: calc(50% - 5px);
  max-width: calc(50% - 5px);
  box-sizing: border-box;
}

.room-basic-info p:hover {
  background: rgba(240, 244, 255, 0.9);
  transform: translateX(3px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
}

.room-basic-info p:last-child {
  color: #666;
}

.room-basic-info i {
  margin-right: 6px;
  color: rgb(179, 219, 225);
  font-size: 14px;
  flex-shrink: 0;
}

/* 时间段 */
.time-slots {
  margin: 0 20px 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-top: 1px dashed rgba(179, 219, 225, 0.3);
}

/* 时间图例 */
.time-legend {
  display: flex;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  gap: 15px;
  flex-wrap: wrap;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 图例点 */
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.dot.unavailable {
  background-color: rgba(217, 217, 217, 0.9);
}

.dot.available {
  background-color: rgba(179, 219, 225, 0.9);
}

.dot.selected {
  background-color: rgba(179, 219, 225, 1);
}

/* 已选时间段信息 */
.selected-time-info {
  margin: 10px 0;
  padding: 10px 12px;
  background: rgba(179, 219, 225, 0.1);
  border-radius: 10px;
  border-left: 3px solid rgba(179, 219, 225, 0.8);
  font-size: 13px;
  color: #606266;
}

/* 时间段容器 */
.slots-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 10px;
  padding: 12px;
  background: rgba(240, 244, 255, 0.5);
  border-radius: 12px;
}

/* 时间段 */
.time-slot {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: not-allowed;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

/* 不可用时间段 */
.time-slot.unavailable {
  background: rgba(245, 245, 245, 0.9);
  color: #8c8c8c;
  border-color: rgba(217, 217, 217, 0.9);
}

/* 可用时间段 */
.time-slot.available {
  background: rgba(230, 247, 255, 0.9);
  color: rgb(179, 219, 225);
  border-color: rgba(145, 213, 255, 0.9);
  cursor: pointer;
}

.time-slot.available:hover {
  background: rgba(186, 231, 255, 0.9);
  box-shadow: 0 4px 10px rgba(179, 219, 225, 0.3);
  transform: translateY(-1px);
}

/* 可选时间段 */
.time-slot.selectable {
  cursor: pointer;
}

.time-slot.selectable:hover {
  background: rgba(186, 231, 255, 0.9);
  box-shadow: 0 4px 10px rgba(179, 219, 225, 0.3);
  transform: translateY(-1px);
}

/* 已选时间段 */
.time-slot.selected {
  background: rgba(179, 219, 225, 1);
  color: white;
  border-color: rgba(179, 219, 225, 1);
  box-shadow: 0 4px 10px rgba(179, 219, 225, 0.4);
  transform: translateY(-1px);
}

/* 无效时间段 */
.time-slot.invalid {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 时间提示 */
.time-tip {
  font-size: 12px;
  color: #606266;
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: rgba(240, 244, 255, 0.5);
  border-radius: 8px;
  border-left: 3px solid rgba(179, 219, 225, 0.6);
}

.time-tip i {
  margin-right: 6px;
  color: #909399;
  font-size: 12px;
}

.time-tip strong {
  color: rgb(179, 219, 225);
  font-weight: 600;
}

/* 预约按钮 */
.book-button {
  margin: 0 20px 20px;
  width: calc(100% - 40px);
  height: 50px;
  background: linear-gradient(135deg, rgba(179, 219, 225, 0.9) 0%, rgba(159, 199, 205, 0.9) 100%);
  border: 1px solid rgba(179, 219, 225, 0.6);
  border-radius: 16px;
  color: white;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 6px 20px rgba(179, 219, 225, 0.3);
  transition: all 0.3s ease;
}

.book-button:hover {
  background: linear-gradient(135deg, rgba(179, 219, 225, 1) 0%, rgba(159, 199, 205, 1) 100%);
  box-shadow: 0 10px 30px rgba(179, 219, 225, 0.4);
  transform: translateY(-3px);
}

/* 预约表单 */
.booking-form {
  margin: 0 20px 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-top: 1px dashed rgba(179, 219, 225, 0.3);
}

/* 表单标题 */
.booking-form .el-form-item {
  margin-bottom: 15px;
}

.booking-form .el-form-item__label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

/* 表单输入框样式 */
.booking-form :deep(.el-input__wrapper),
.booking-form :deep(.el-textarea__wrapper) {
  background: rgba(240, 244, 255, 0.8);
  border-radius: 10px;
  border-color: rgba(179, 219, 225, 0.3);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
}

.booking-form :deep(.el-input__wrapper:hover),
.booking-form :deep(.el-textarea__wrapper:hover) {
  border-color: rgba(179, 219, 225, 0.6);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.08);
}

.booking-form :deep(.el-input__wrapper.is-focus),
.booking-form :deep(.el-textarea__wrapper.is-focus) {
  border-color: rgba(179, 219, 225, 0.9);
  box-shadow: 0 0 0 2px rgba(179, 219, 225, 0.2);
}

/* 表单按钮 */
.booking-form .el-form-item:last-child {
  margin-bottom: 0;
  display: flex;
  justify-content: center;
  gap: 15px;
}

.booking-form .el-button {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.booking-form .el-button--success {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.9) 0%, rgba(83, 174, 38, 0.9) 100%);
  border-color: rgba(103, 194, 58, 0.6);
}

.booking-form .el-button--success:hover {
  background: linear-gradient(135deg, rgba(103, 194, 58, 1) 0%, rgba(83, 174, 38, 1) 100%);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.3);
  transform: translateY(-2px);
}

.booking-form .el-button--default {
  background: rgba(245, 245, 245, 0.9);
  border-color: rgba(217, 217, 217, 0.9);
  color: #606266;
}

.booking-form .el-button--default:hover {
  background: rgba(245, 245, 245, 1);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 已选时间信息 */
.selected-time-info {
  margin: 10px 0;
  padding: 10px;
  background: rgba(179, 219, 225, 0.1);
  border-radius: 8px;
  border-left: 4px solid rgba(179, 219, 225, 0.8);
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

/* 表单元素样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 日历样式 - 与管理员页面一致 */
:deep(.el-date-picker) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 28px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  transform-style: preserve-3d;
  perspective: 1000px;
  transition: all 0.3s ease;
}

/* 日历头部样式 */
:deep(.el-picker-panel__header) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.1), rgba(179, 219, 225, 0.05));
  border-radius: 20px 20px 0 0;
  padding: 15px 20px;
  text-align: center;
}

/* 日历标题样式 */
:deep(.el-picker-panel__title) {
  color: rgb(179, 219, 225);
  font-weight: 700;
  font-size: 16px;
  margin: 0;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

/* 日历导航按钮样式 */
:deep(.el-picker-panel__icon-btn) {
  color: rgb(179, 219, 225);
  font-size: 18px;
  transition: all 0.3s ease;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover {
    background: rgba(179, 219, 225, 0.1);
    color: rgb(159, 199, 205);
    box-shadow: 0 4px 12px rgba(179, 219, 225, 0.3);
  }
}

/* 日期表格样式 */
:deep(.el-calendar-table) {
  width: 100%;
  border-collapse: separate;
  border-spacing: 8px;
}

/* 日期单元格样式 */
:deep(.el-calendar__body tr td) {
  padding: 0;
  border: none;
}

/* 日期单元格内容样式 */
:deep(.el-calendar-table__cell) {
  position: relative;
  height: 50px;
  padding: 10px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: rgba(255, 255, 255, 0.6);
  margin: 8px;
  text-align: center;
  &:hover {
    background: rgba(179, 219, 225, 0.15);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
    transform: translateY(-3px);
  }
}

/* 当前日期样式 */
:deep(.el-calendar-table__today) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.25), rgba(179, 219, 225, 0.15));
  border: 2px solid rgb(179, 219, 225);
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
}

/* 选中日期样式 */
:deep(.el-calendar-table__row td.is-selected) {
  background: rgba(179, 219, 225, 0.8);
  color: white;
  border-radius: 20px;
  box-shadow: 0 6px 16px rgba(179, 219, 225, 0.4);
}

/* 月份切换器样式 */
:deep(.el-month-table td) {
  padding: 10px 0;
}

/* 月份单元格样式 */
:deep(.el-month-table__cell) {
  height: 50px;
  padding: 10px;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: rgba(255, 255, 255, 0.6);
  margin: 8px;
  text-align: center;
  &:hover {
    background: rgba(179, 219, 225, 0.15);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
    transform: translateY(-3px);
  }
}

/* 年份切换器样式 */
:deep(.el-year-table td) {
  padding: 15px 0;
}

/* 年份单元格样式 */
:deep(.el-year-table__cell) {
  height: 60px;
  padding: 15px;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: rgba(255, 255, 255, 0.6);
  margin: 8px;
  text-align: center;
  &:hover {
    background: rgba(179, 219, 225, 0.15);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
    transform: translateY(-3px);
  }
}

/* 周标题样式 */
:deep(.el-calendar-table__row th) {
  color: rgb(179, 219, 225);
  font-weight: 600;
  padding: 10px 0;
  text-align: center;
}

/* 日期文本样式 */
:deep(.el-calendar-table__cell span) {
  color: #666;
  font-weight: 500;
}

/* 当前日期文本样式 */
:deep(.el-calendar-table__today span) {
  color: rgb(179, 219, 225);
  font-weight: 700;
}

/* 选中日期文本样式 */
:deep(.el-calendar-table__row td.is-selected span) {
  color: white;
  font-weight: 700;
}

/* 其他月份日期文本样式 */
:deep(.el-calendar-table__cell.next-month span),
:deep(.el-calendar-table__cell.prev-month span) {
  color: #c0c4cc;
  opacity: 0.7;
}

/* 时间选择器样式 */
:deep(.el-time-panel) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 28px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
}

/* 时间选择器头部样式 */
:deep(.el-time-panel__content::before) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.1), rgba(179, 219, 225, 0.05));
  border-radius: 20px 20px 0 0;
}

/* 时间选择器项目样式 */
:deep(.el-time-spinner__item) {
  height: 36px;
  line-height: 36px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  &:hover {
    background: rgba(179, 219, 225, 0.15);
    border-radius: 12px;
  }
}

/* 选中时间样式 */
:deep(.el-time-spinner__item.active) {
  background: rgba(179, 219, 225, 0.8);
  color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(179, 219, 225, 0.3);
}

/* 底部操作按钮样式 */
:deep(.el-picker-panel__footer) {
  padding: 15px 20px;
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.1), rgba(179, 219, 225, 0.05));
  border-radius: 0 0 28px 28px;
}

/* 确认按钮样式 */
:deep(.el-picker-panel__footer-btn) {
  height: 40px;
  padding: 0 20px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  background: rgba(179, 219, 225, 0.8);
  border: 1px solid rgba(179, 219, 225, 0.6);
  color: white;
  &:hover {
    background: rgba(179, 219, 225, 1);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.4);
    transform: translateY(-2px);
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
