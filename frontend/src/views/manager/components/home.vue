<template>
  <div class="calendar-container">
    <!-- 日历组件，添加过渡动画和自定义单元格 -->
    <transition name="calendar-fade">
      <el-calendar v-model="value" class="custom-calendar" :first-day-of-week="1">
        <!-- 自定义月份导航 -->
        <template #header>
          <div class="calendar-header">
            <div class="welcome-message">欢迎您，{{ adminName }}管理员</div>
            <div class="header-controls">
              <button class="month-btn" @click="changeMonth(-1)">上月</button>
              <h3 class="current-month">{{ currentMonthText }}</h3>
              <button class="month-btn" @click="changeMonth(1)">下月</button>
              <button class="today-btn" @click="value = new Date()">今天</button>
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

        <!-- 自定义日期单元格内容（移除指示器相关） -->
        <template #date-cell="{ data }">
          <div
            :class="['calendar-cell', {
              'is-current': data.type === 'current-month' && Number(data.day.split('-')[2]) === new Date().getDate(),
              'is-other-month': data.type !== 'current-month'
            }]"
            @click="handleDateClick(data.date)"
          >
            <span class="cell-day">{{ data.day.split('-')[2] }}</span>
          </div>
        </template>
      </el-calendar>
    </transition>
    <!-- 预约详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${selectedDateFormatted} 会议室预约`"
      :width="dialogWidth"
      :before-close="handleClose"
      custom-class="reservation-dialog"
      :modal-append-to-body="false"
      :append-to-body="true"
    >
 <div
  class="loading-container"
  v-loading="loading"
  element-loading-text="加载中..."
  element-loading-spinner-size="40"
>
  <!-- 加载状态下，内容会被遮罩覆盖，无需额外写“加载中”文本 -->
</div>

      <div v-if="selectedReservations.length" class="reservations-container">
        <el-collapse class="reservation-collapse">
          <el-collapse-item title="待审批" name="pending" class="collapse-item">
            <div v-if="pendingReservations.length" class="reservation-list">
              <el-card
                class="reservation-card pending-card"
                v-for="item in pendingReservations"
                :key="item.reservationId"
                hover
              >
                <div class="card-header">
                  <span class="room-name">{{ getRoomName(item.roomId) }}</span>
                  <span class="status-badge pending-badge">待审批</span>
                </div>
                <div class="card-body">
                  <p><i class="el-icon-time"></i> {{ item.startTime }} - {{ item.endTime }}</p>
                  <p><i class="el-icon-user"></i> 申请人: 用户{{ item.username }}</p>
                  <p><i class="el-icon-info"></i> 用途: {{ item.purpose }}</p>
                </div>
              </el-card>
            </div>
            <p v-else class="empty-text">无待审批预约</p>
          </el-collapse-item>
          <el-collapse-item title="已审批" name="approved" class="collapse-item">
            <div v-if="approvedReservations.length" class="reservation-list">
              <el-card
                class="reservation-card approved-card"
                v-for="item in approvedReservations"
                :key="item.reservationId"
                hover
              >
                <div class="card-header">
                  <span class="room-name">{{ getRoomName(item.roomId) }}</span>
                  <span class="status-badge approved-badge">已审批</span>
                </div>
                <div class="card-body">
                  <p><i class="el-icon-time"></i> {{ item.startTime }} - {{ item.endTime }}</p>
                  <p><i class="el-icon-user"></i> 申请人: 用户{{ item.username }}</p>
                  <p><i class="el-icon-info"></i> 用途: {{ item.purpose }}</p>
                </div>
              </el-card>
            </div>
            <p v-else class="empty-text">无已审批预约</p>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div v-else class="empty-state">
        <el-empty description="该日期暂无预约记录" />
      </div>
    </el-dialog>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElCalendar, ElDialog, ElCard, ElCollapse, ElCollapseItem, ElLoading, ElEmpty, ElMessage } from 'element-plus';
// 引入简化后的管理员接口（与registerAPI风格一致）
import { getReservationListAPI, getAllMeetroomsAPI, getUserInfoAPI } from '@/apis/managerhome.js';

// 类型定义
interface Reservation {
  reservationId: number;
  roomId: number;
  username: string;
  date: string;
  startTime: string;
  endTime: string;
  purpose: string;
  status: string; // 0-待审批，2-已审批（接口文档返回示例）
}
interface Meetroom {
  roomId: number;
  roomName: string;
}

// 状态管理（移除指示器相关状态）
const value = ref(new Date());
const dialogVisible = ref(false);
const selectedDate = ref<Date | null>(null);
const selectedReservations = ref<Reservation[]>([]);
const loading = ref(false);
const adminName = ref(''); // 管理员名称
const meetrooms = ref<Meetroom[]>([]); // 会议室列表（用于匹配会议室名称）

// 月份导航相关
const currentMonthText = computed(() => {
  const year = value.value.getFullYear();
  const month = value.value.getMonth() + 1;
  return `${year}年${month}月`;
});

// 切换月份
const changeMonth = (step: number) => {
  const newDate = new Date(value.value);
  newDate.setMonth(newDate.getMonth() + step);
  value.value = newDate;
};

// 获取会议室名称（通过roomId匹配）
const getRoomName = (roomId: number) => {
  const room = meetrooms.value.find(item => item.roomId === roomId);
  return room ? room.roomName : `会议室${roomId}`;
};

// 加载指定日期的预约数据
const fetchReservations = async (date: Date) => {
  const dateStr = formatDate(date);
  loading.value = true;
  try {
    // 调用接口获取预约数据
    const res = await getReservationListAPI({ date: dateStr });
    selectedReservations.value = res.data.data || [];
    console.log("selected",selectedReservations.value);
  } catch (error) {
    // 接口失败时使用空数组
    selectedReservations.value = [];
    console.error('加载预约数据失败:', error);
    ElMessage.error('加载预约数据失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 获取管理员名称（调用简化后的接口）
const fetchAdminInfo = async () => {
  try {
    const res = await getUserInfoAPI();
    console.log("12332",res.data.data)
    // 按实际接口返回字段调整（示例：若返回data.username则用res.data.username）
    adminName.value = res.data.data.username || '刘昊洋';
  } catch (error) {
    adminName.value = '刘昊洋'; // 默认名称兜底
  }
};

// 初始化会议室列表
const initMeetrooms = async () => {
  try {
    // 调用接口获取会议室列表
    const res = await getAllMeetroomsAPI({ page: 1, size: 100 });
    meetrooms.value = res.data.data || [];
  } catch (error) {
    // 接口失败时使用空数组
    meetrooms.value = [];
    console.error('初始化会议室列表失败:', error);
    ElMessage.error('初始化会议室列表失败，请重试');
  }
};

// 工具函数：格式化日期为 yyyy-MM-dd
const formatDate = (date: Date): string => {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

// 处理日期点击
const handleDateClick = (date: Date) => {
  selectedDate.value = date;
  dialogVisible.value = true;
  fetchReservations(date);
};

// 关闭弹窗
const handleClose = () => {
  dialogVisible.value = false;
  selectedReservations.value = [];
};

// 计算属性：格式化选中的日期
const selectedDateFormatted = computed(() => {
  if (!selectedDate.value) return '';
  const date = selectedDate.value;
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
});

// 计算属性：筛选待审批预约
const pendingReservations = computed(() => {
  return selectedReservations.value.filter(item => item.status == "待确认");
});

// 计算属性：筛选已审批预约
const approvedReservations = computed(() => {
  return selectedReservations.value.filter(item => item.status == "已通过"||item.status=="已过期"||item.status=="已拒绝"||item.status=="使用中"||item.status=="已取消");
});

// 计算属性：弹窗宽度
const dialogWidth = computed(() => {
  return window.innerWidth > 768 ? '600px' : '90%';
});

// 初始化
onMounted(() => {
  fetchAdminInfo();
  initMeetrooms(); // 初始化会议室列表
});
</script>
<style scoped lang="scss">

/* 外层容器样式 - 玻璃质感 */
.calendar-container {
  padding: 24px;
  width: 80%;
  min-width: 900px;
  margin: 20px auto;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 28px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
  perspective: 1000px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    inset 0 -2px 4px rgba(0, 0, 0, 0.05);
  &:hover {
    transform: translateY(-5px) translateZ(10px);
    box-shadow:
      0 15px 40px rgba(31, 38, 135, 0.15),
      0 0 0 1px rgba(255, 255, 255, 0.4),
      inset 0 2px 4px rgba(255, 255, 255, 0.9),
      inset 0 -2px 4px rgba(0, 0, 0, 0.08);
  }
}
/* 日历整体样式 */
.custom-calendar {
  border-radius: 20px;
  overflow: hidden;
  border: none;
  width: 100%;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
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
  height: 180px;
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
/* 日期单元格样式 */
.calendar-cell {
  position: relative;
  height: 100%;
  padding: 20px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.6);
  margin: 8px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform-style: preserve-3d;
  &:hover {
    background: linear-gradient(145deg, rgba($primaryColor, 0.15), rgba($primaryColor, 0.05));
    transform: translateY(-5px) rotateX(5deg);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
    border-color: rgba(179, 219, 225, 0.4);
  }
}
.calendar-cell.is-current {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.25), rgba(179, 219, 225, 0.15));
  border: 2px solid rgb(179, 219, 225);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  transform: scale(1.05);
}
.calendar-cell.is-other-month {
  opacity: 0.6;
  background: rgba(255, 255, 255, 0.3);
}
.cell-day {
  font-size: 17px;
  font-weight: 700;
  color: rgb(179, 219, 225);
  text-shadow: 2px 2px 4px rgba(255, 255, 255, 0.8);
}
/* 弹窗样式 */
.reservation-dialog {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 28px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    inset 0 -2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  &:hover {
    transform: translateY(-5px) translateZ(10px);
    box-shadow:
      0 15px 40px rgba(31, 38, 135, 0.15),
      0 0 0 1px rgba(255, 255, 255, 0.4),
      inset 0 2px 4px rgba(255, 255, 255, 0.9),
      inset 0 -2px 4px rgba(0, 0, 0, 0.08);
  }
}
/* 预约卡片样式 */
:deep(.reservation-card) {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    inset 0 -2px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 12px;
  border: none;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  &:hover {
    transform: translateY(-5px) translateZ(10px);
    box-shadow: 0 25px 60px rgba(0, 0, 0, 0.18);
  }
}
:deep(.pending-card) {
  border-left: 5px solid rgb(252, 207, 134);
}
:deep(.approved-card) {
  border-left: 5px solid $sucColor;
}
/* 折叠面板样式 */
:deep(.el-collapse) {
  border: none;
}
:deep(.el-collapse-item) {
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    inset 0 -2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  &:hover {
    transform: translateY(-5px) translateZ(10px);
    box-shadow:
      0 15px 40px rgba(31, 38, 135, 0.15),
      0 0 0 1px rgba(255, 255, 255, 0.4),
      inset 0 2px 4px rgba(255, 255, 255, 0.9),
      inset 0 -2px 4px rgba(0, 0, 0, 0.08);
  }
}
:deep(.el-collapse-item__header) {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.1), rgba(179, 219, 225, 0.05));
  color: rgb(179, 219, 225);
  font-weight: 600;
  font-size: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  &:hover {
    background: linear-gradient(145deg, rgba(179, 219, 225, 0.15), rgba(179, 219, 225, 0.1));
    transform: translateY(-2px);
  }
}
/* 状态徽章样式 */
:deep(.status-badge) {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}
:deep(.pending-badge) {
  background: linear-gradient(145deg, $warnColor, rgb(255, 221, 161));
  color: #fff;
}
:deep(.approved-badge) {
  background: linear-gradient(145deg, $sucColor, rgb(177, 241, 229));
  color: #fff;
}
/* 响应式调整 */
  @media (max-width: 768px) {
    .calendar-container {
      padding: 12px;
      min-width: auto;
      width: 100%;
      margin: 0;
    }
    .calendar-header {
      flex-direction: column;
      gap: 10px;
      align-items: flex-start;
      padding: 12px;
    }
    .header-controls {
      width: 100%;
      justify-content: space-between;
      gap: 8px;
      flex-wrap: wrap;
    }
    .month-btn {
      padding: 6px 12px;
      font-size: 14px;
      flex: 1;
      min-width: 60px;
    }
    .current-month {
      font-size: 16px;
      padding: 8px 12px;
      flex: 1;
      min-width: 120px;
      text-align: center;
    }
    .today-btn {
      padding: 6px 12px;
      font-size: 14px;
      flex: 1;
      min-width: 60px;
    }
    
    /* 确保日历头部在移动端正确布局 */
    .calendar-header {
      flex-direction: column;
      gap: 10px;
      align-items: flex-start;
      padding: 12px;
      width: 100%;
      box-sizing: border-box;
    }
    
    /* 确保头部控制区域在移动端有足够空间 */
    .header-controls {
      width: 100%;
      display: flex;
      justify-content: space-between;
      gap: 8px;
      box-sizing: border-box;
    }
    :deep(.el-calendar-table) {
      width: 100%;
      table-layout: fixed;
    }
    :deep(.el-calendar-table__row) {
      height: 45px;
    }
    :deep(.el-calendar-table td) {
      width: calc(100% / 7);
      padding: 0;
      height: 45px;
    }
    .calendar-cell {
      padding: 6px;
      margin: 2px;
      height: 36px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .cell-day {
      font-size: 14px;
      text-align: center;
    }
    .welcome-message {
      font-size: 16px;
      text-align: left;
    }
  }
</style>
