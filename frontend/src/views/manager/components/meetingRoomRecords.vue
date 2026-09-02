<template>
  <div class="dashboard-container">
    <!-- 数据卡片区域 -->
    <div class="stats-container mb-4">
      <div class="stat-item">
        <div class="card-icon bg-primary">
          <el-icon class="icon-sm"><House /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ roomCount }}</div>
          <div class="card-label">会议室总数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="card-icon bg-success">
          <el-icon class="icon-sm"><Calendar /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ meetingCount }}</div>
          <div class="card-label">今日会议</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="card-icon bg-warning">
          <el-icon class="icon-sm"><Clock /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ approvedCount }}</div>
          <div class="card-label">已审核申请</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="card-icon bg-danger">
          <el-icon class="icon-sm"><Clock /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ pendingCount }}</div>
          <div class="card-label">待审核申请</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="24">
        <el-card class="chart-card" shadow="hover">
          <div class="card-header">
            <h3>会议室使用频率统计</h3>
            <el-select
              v-model="timeRange"
              size="small"
              @change="handleTimeRangeChange"
            >
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
              <el-option label="本季度" value="quarter" />
            </el-select>
          </div>
          <div class="chart-container">
            <canvas id="roomUsageChart"></canvas>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-4">
      <el-col :xs="24">
        <el-card class="chart-card" shadow="hover">
          <div class="card-header">
            <h3>会议申请趋势</h3>
            <el-select
              v-model="chartType"
              size="small"
              @change="updateTrendChart"
            >
              <el-option label="按周统计" value="week" />
              <el-option label="按月统计" value="month" />
            </el-select>
          </div>
          <div class="chart-container">
            <canvas id="applicationTrendChart"></canvas>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { House, Calendar, Clock } from '@element-plus/icons-vue';
import Chart from 'chart.js/auto';
// 导入接口
import { getAdminStatsAPI, getRoomFrequencyChartAPI, getReservationListAPI } from '@/apis/adminAPI.js';

// 统计数据
const roomCount = ref(0);
const meetingCount = ref(0);
const approvedCount = ref(0);
const pendingCount = ref(0);

// 图表相关
const timeRange = ref('week');
const chartType = ref('week'); // 初始按周统计
let roomUsageChart = null;
let applicationTrendChart = null;
// 会议室使用频率数据
const roomFrequencyData = ref({
  roomNames: [],
  usageCounts: []
});
// 会议申请趋势真实数据
const trendData = ref({
  labels: [],
  applyCounts: [],
  passCounts: []
});

// 获取管理员统计信息
const fetchAdminStats = async () => {
  try {
    const res = await getAdminStatsAPI();
    if (res.data.code === 1) {
      const data = res.data.data;
      roomCount.value = data.totalMeetingRooms || 0;
      meetingCount.value = data.todayMeetings || 0;
      approvedCount.value = data.reviewedApplications || 0;
      pendingCount.value = data.pendingApplications || 0;
    }
  } catch (err) {
    console.error('获取统计信息失败：', err);
    // 接口失败时使用默认值
    roomCount.value = 0;
    meetingCount.value = 0;
    approvedCount.value = 0;
    pendingCount.value = 0;
  }
};

// 获取会议室使用频率数据
const fetchRoomFrequency = async (period) => {
  try {
    const res = await getRoomFrequencyChartAPI({ period });
    console.log("aaa",res)
    if (res.data.code === 1) {
      const data = res.data.data;
      roomFrequencyData.value = {
        roomNames: data.roomNames || [],
        usageCounts: data.usageCounts || []
      };
    } else {
      roomFrequencyData.value = {
        roomNames: [],
        usageCounts: []
      };
    }
    // 更新图表数据
    updateRoomUsageChart();
  } catch (err) {
    console.error('获取会议室使用频率失败：', err);
    // 接口失败时使用空数据
    roomFrequencyData.value = {
      roomNames: [],
      usageCounts: []
    };
    updateRoomUsageChart();
  }
};

// 获取日期范围（用于筛选当前周期的记录）
const getDateRange = (type) => {
  const today = new Date();
  let startDate, endDate;
  if (type === 'week') {
    // 本周一到周日
    const weekDay = today.getDay() || 7; // 周日转7
    startDate = new Date(today.setDate(today.getDate() - weekDay + 1));
    endDate = new Date(today.setDate(today.getDate() + 6));
  } else if (type === 'month') {
    // 本月1日到月末
    startDate = new Date(today.getFullYear(), today.getMonth(), 1);
    endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  }
  return {
    start: startDate.toISOString().split('T')[0],
    end: endDate.toISOString().split('T')[0]
  };
};

// 获取会议申请趋势真实数据（获取所有预约记录，筛选当前周期内的记录统计）
const fetchApplicationTrend = async () => {
  try {
    // 1. 获取所有预约记录（不传递日期参数）
    const res = await getReservationListAPI();
    console.log("所有预约记录：", res.data)

    if (res.data.code === 1) {
      const allReservations = res.data.data;
      // 2. 获取当前选择周期的日期范围（本周/本月）
      const { start, end } = getDateRange(chartType.value);
      const startDate = new Date(start);
      const endDate = new Date(end);

      // 3. 筛选出预约日期在当前周期内的记录
      const filteredReservations = allReservations.filter(item => {
        if (!item.reserveDate) return false;
        const itemReserveDate = new Date(item.reserveDate);
        return itemReserveDate >= startDate && itemReserveDate <= endDate;
      });

      // 4. 生成对应周期的标签
      const labels = [];
      if (chartType.value === 'week') {
        // 本周7天的“月-日”格式标签
        for (let i = 0; i < 7; i++) {
          const date = new Date(start);
          date.setDate(date.getDate() + i);
          labels.push(date.toISOString().split('T')[0].slice(5));
        }
      } else if (chartType.value === 'month') {
        // 本月4周的“第X周”标签
        labels.push('第1周', '第2周', '第3周', '第4周');
      }

      // 5. 统计申请数、通过数
      const applyCounts = new Array(labels.length).fill(0);
      const passCounts = new Array(labels.length).fill(0);

      filteredReservations.forEach(item => {
        const itemDate = item.reserveDate ? item.reserveDate.split('T')[0] : '';
        if (!itemDate) return;

        let idx = -1;
        if (chartType.value === 'week') {
          // 匹配本周7天的“月-日”标签
          idx = labels.indexOf(itemDate.slice(5));
        } else if (chartType.value === 'month') {
          // 匹配本月4周的标签（按日期分周）
          const itemDateObj = new Date(itemDate);
          const week = Math.ceil(itemDateObj.getDate() / 7);
          idx = week - 1;
        }

        if (idx !== -1 && idx < labels.length) {
          applyCounts[idx]++;
          // 状态规则：不是待确认（0）和已拒绝（2）都算已通过
          if (item.status !== 0 && item.status !== 2) {
            passCounts[idx]++;
          }
        }
      });

      trendData.value = { labels, applyCounts, passCounts };
    }
  } catch (err) {
    console.error('获取申请趋势数据失败：', err);
  }
};

// 时间范围变更（使用频率）
const handleTimeRangeChange = async () => {
  await fetchRoomFrequency(timeRange.value);
  updateRoomUsageChart();
};

// 更新申请趋势图表
const updateTrendChart = async () => {
  await fetchApplicationTrend();
  destroyChart(applicationTrendChart);
  initApplicationTrendChart();
};

// 销毁图表实例
const destroyChart = (chart) => {
  if (chart) {
    chart.destroy();
  }
};

// 初始化会议室使用频率图表
const initRoomUsageChart = () => {
  const ctx = document.getElementById('roomUsageChart').getContext('2d');
  destroyChart(roomUsageChart);

  roomUsageChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: roomFrequencyData.value.roomNames,
      datasets: [{
        label: '使用次数',
        data: roomFrequencyData.value.usageCounts,
        backgroundColor: 'rgba(64, 158, 255, 0.7)',
        borderColor: 'rgba(64, 158, 255, 1)',
        borderWidth: 1,
        borderRadius: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top',
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            precision: 0
          }
        }
      }
    }
  });
};

// 初始化申请趋势图表（真实数据）
const initApplicationTrendChart = () => {
  const ctx = document.getElementById('applicationTrendChart').getContext('2d');
  destroyChart(applicationTrendChart);

  applicationTrendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: trendData.value.labels,
      datasets: [
        {
          label: '申请数量',
          data: trendData.value.applyCounts,
          borderColor: 'rgba(64, 158, 255, 1)',
          backgroundColor: 'rgba(64, 158, 255, 0.1)',
          tension: 0.4,
          fill: true
        },
        {
          label: '通过数量',
          data: trendData.value.passCounts,
          borderColor: 'rgba(103, 194, 58, 1)',
          backgroundColor: 'rgba(103, 194, 58, 0.1)',
          tension: 0.4,
          fill: true
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top',
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            precision: 0
          }
        }
      }
    }
  });
};

// 更新会议室使用频率图表
const updateRoomUsageChart = () => {
  initRoomUsageChart();
};

// 页面挂载时请求数据
onMounted(async () => {
  await fetchAdminStats();
  await fetchRoomFrequency(timeRange.value);
  await fetchApplicationTrend();
  initRoomUsageChart();
  initApplicationTrendChart();
});

// 组件卸载时销毁图表
watch(() => {}, () => {
  destroyChart(roomUsageChart);
  destroyChart(applicationTrendChart);
}, { once: true });
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  box-sizing: border-box;
}

.mb-4 {
  margin-bottom: 20px;
}

.mt-4 {
  margin-top: 20px;
}

.stats-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #ebeef5;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  background-color: #fafafa;
  flex: 1;
  min-width: calc(50% - 5px);
  max-width: calc(50% - 5px);
  min-height: 60px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-color: #f5f7fa;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  transition: all 0.3s ease;
  height: 100%;
  min-height: 60px;
  max-height: 80px;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.card-icon {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  margin-bottom: 0;
  color: white;
  flex-shrink: 0;
}

.bg-primary {
  background-color: rgba(64, 158, 255, 0.9);
}

.bg-success {
  background-color: rgba(103, 194, 58, 0.9);
}

.bg-warning {
  background-color: rgba(247, 186, 42, 0.9);
}

.bg-danger {
  background-color: rgba(237, 100, 166, 0.9);
}

.icon-lg {
  font-size: 24px;
}

.icon-sm {
  font-size: 14px;
}

.card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: left;
  width: 100%;
}

.card-value {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 2px;
  color: #333;
  line-height: 1.2;
}

.card-label {
  font-size: 11px;
  color: #666;
  opacity: 0.9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
  height: auto;
  max-width: 100%;
}

.chart-card {
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: 100%;
}

.chart-card:hover {
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  padding: 15px 20px;
  border-bottom: 1px solid #f2f3f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
  white-space: nowrap; /* 文字不换行 */
}

.text-sm {
  font-size: 13px;
}

.text-gray-500 {
  color: #909399;
}

.chart-container {
  padding: 20px;
  height: 320px;
  box-sizing: border-box;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .chart-container {
    height: 280px;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 15px 10px;
  }

  .chart-container {
    height: 250px;
    padding: 15px 10px;
  }

  .card-header {
    padding: 12px 15px;
  }

  .card-header h3 {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .chart-container {
    height: 200px;
  }

  .stats-container {
    padding: 10px;
    gap: 8px;
  }

  .stat-item {
    padding: 8px;
    min-height: 50px;
    min-width: calc(50% - 4px);
    max-width: calc(50% - 4px);
  }

  .card-icon {
    width: 25px;
    height: 25px;
    margin-right: 6px;
  }

  .icon-sm {
    font-size: 12px;
  }

  .card-value {
    font-size: 14px;
    margin-bottom: 2px;
  }

  .card-label {
    font-size: 10px;
  }

  .mb-4 {
    margin-bottom: 10px;
  }

  .mt-4 {
    margin-top: 10px;
  }
}
</style>
