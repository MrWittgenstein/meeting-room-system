<template>
  <div class="pending-review-page">
    <div class="main-content-wrapper">
      <!-- 左侧卡片区域 -->
      <div class="card-section">
        <!-- 卡片列表区域 -->
        <div class="card-list-container">

          <!-- 修复：统一状态字段为state，确保v-for循环能拿到数据 -->
          <div
            v-for="item in filteredTableData"
            :key="item.reservationId"
            class="approval-card"
          >
            <div class="card-header">
              <!-- 会议标题：接口返回的purpose字段 -->
              <div class="meeting-title">申请缘由：{{ item.purpose }}</div>
              <!-- 修复：状态字段统一为state -->
              <el-tag
                :type="getStatusType(item.status)"
                effect="light"
                class="status-tag"
              >
                {{ formatStatus(item.status) }}
              </el-tag>
            </div>

            <div class="card-body">
              <div class="meeting-info">
                <div class="info-item">
                  <el-icon class="info-icon"><Location /></el-icon>
                  <span class="info-label">会议室：</span>
                  <!-- 会议室名称：接口返回的roomName字段 -->
                  <span class="info-value">{{ item.roomName }}</span>
                </div>
                <div class="info-item">
                  <el-icon class="info-icon"><Calendar /></el-icon>
                  <span class="info-label">会议日期：</span>
                  <!-- 会议日期：接口返回的reserveDate字段 -->
                  <span class="info-value">{{ item.reserveDate }}</span>
                </div>
                <div class="info-item">
                  <el-icon class="info-icon"><Clock /></el-icon>
                  <span class="info-label">时间：</span>
                  <span class="info-value">{{ item.startTime }} - {{ item.endTime }}</span>
                </div>
                <div class="info-item">
                  <el-icon class="info-icon"><User /></el-icon>
                  <span class="info-label">申请人：</span>
                  <!-- 申请人：接口返回的userName字段 -->
                  <span class="info-value">{{ item.username }}</span>
                </div>

              </div>

              <div class="card-actions">
                <el-button
                  type="primary"
                  @click="handleViewDetail(item)"
                  class="view-btn"
                  :disabled="item.status !== '待确认'"
                >
                  <el-icon><View /></el-icon>
                  查看详情
                </el-button>
                <el-button
                  type="success"
                  @click="handleApprove(item)"
                  class="approve-btn"
                  :disabled="item.status !== '待确认'"
                >
                  <el-icon><Check /></el-icon>
                  通过
                </el-button>
                <el-button
                  type="danger"
                  @click="handleReject(item)"
                  class="reject-btn"
                  :disabled="item.status !== '待确认'"
                >
                  <el-icon><CircleClose /></el-icon>
                  拒绝
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧功能区域 -->
      <div class="right-sidebar">
        <!-- 查询区域 - 移除会议日期字段 -->
        <el-card class="search-card">
          <h3 class="sidebar-title">会议查询</h3>
          <div class="search-form">
            <div class="form-item">
              <label class="form-label">审核状态</label>
              <el-select
                v-model="searchForm.status"
                placeholder="请选择审核状态"
                class="w-full"
                :teleported="false"
              >
                <el-option label="全部" value="" />
                <el-option label="待审核" value="待确认" />
              </el-select>
            </div>
            <div class="form-item">
              <label class="form-label">会议名称</label>
              <el-input
                v-model="searchForm.roomName"
                placeholder="请输入会议名称查询"
                class="w-full"
                clearable
                prefix-icon="Search"
              />
            </div>
            <div class="form-item">
              <el-button type="primary" @click="handleQuery" class="w-full query-btn">
                <el-icon><Search /></el-icon>
                <span>查询</span>
              </el-button>
            </div>
            <div class="form-item">
              <el-button type="warning" @click="handleReset" class="w-full reset-btn">
                <el-icon><Refresh /></el-icon>
                <span>重置</span>
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 分页控制区域 -->
        <el-card class="pagination-card">
          <h3 class="sidebar-title">分页控制</h3>
          <div class="pagination-controls">
            <div class="pagination-info">
              <span>第 {{ pagination.currentPage }} / {{ Math.ceil(pagination.total / pagination.pageSize) }} 页</span>
              <span>共 {{ pagination.total }} 条记录</span>
            </div>
            <div class="page-size-control">
              <label class="form-label">每页显示：</label>
              <el-select
                v-model="pagination.pageSize"
                class="w-full"
                @change="handleSizeChange"
                size="small"
                :teleported="false"
              >
                <el-option label="5 条" :value="5" />
                <el-option label="10 条" :value="10" />
                <el-option label="20 条" :value="20" />
                <el-option label="50 条" :value="50" />
                <el-option label="100 条" :value="100" />
              </el-select>
            </div>
            <div class="page-nav-buttons">
              <el-button
                type="primary"
                @click="handleCurrentChange(pagination.currentPage - 1)"
                :disabled="pagination.currentPage === 1"
                size="small"
              >
                <el-icon><ArrowLeft /></el-icon>
                上一页
              </el-button>
              <el-button
                type="primary"
                @click="handleCurrentChange(pagination.currentPage + 1)"
                :disabled="pagination.currentPage === Math.ceil(pagination.total / pagination.pageSize)"
                size="small"
              >
                下一页
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 详情弹窗 - 字段匹配接口返回值 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="会议申请详情"
      width="600px"
      class="detail-dialog"
    >
      <el-descriptions column="1" border>
        <el-descriptions-item label="会议室类型">{{ currentItem.roomType }}</el-descriptions-item>
        <el-descriptions-item label="会议室地点">{{ currentItem.roomLocation }}</el-descriptions-item>
        <el-descriptions-item label="会议标题">{{ currentItem.title ||'无'}}</el-descriptions-item>
        <el-descriptions-item label="会议描述">{{ currentItem.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="时间范围">{{ currentItem.startTime }} - {{ currentItem.endTime }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentItem.initiator }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentItem.phone || '无' }}</el-descriptions-item>

        <el-descriptions-item label="备注信息">{{ currentItem.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝原因"
      width="400px"
      class="reject-dialog"
    >
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReject">确认拒绝</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import {
  Search,
  Location,
  Calendar,
  Clock,
  User,
  UserFilled,
  View,
  Check,
  CircleClose,
  Refresh,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, ElForm } from 'element-plus';
// 导入审批相关API
import {
  getPendingApprovalList,
  approveReservation,
  getReservationDetail
} from '@/apis/approveReservationAPI.js';

// 搜索表单 - 移除date字段
const searchForm = reactive({
  status: '待确认',
  roomName:''
});
// 表格数据
const tableData = ref([]);
// 加载状态
const isLoading = ref(false);
// 分页控制
const pagination = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0
});
// 弹窗相关
const detailDialogVisible = ref(false);
const rejectDialogVisible = ref(false);
const currentItem = ref({});
const rejectFormRef = ref(null);
const rejectForm = reactive({
  reason: ''
});
// 拒绝原因验证规则
const rejectRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { min: 5, message: '原因至少5个字符', trigger: 'blur' }
  ]
};

// 格式化状态显示（适配接口返回的state字符串）
const formatStatus = (state) => {
  switch (state) {
    case '待确认': return '待审核';
    case '已通过': return '已通过';
    case '已拒绝': return '已拒绝';
    default: return state;
  }
};

// 根据状态获取标签类型（适配接口返回的state字符串）
const getStatusType = (state) => {
  switch (state) {
    case '待确认': return 'warning';
    case '已通过': return 'success';
    case '已拒绝': return 'danger';
    default: return 'info';
  }
};

// 筛选后的数据 - 移除日期筛选逻辑（修复：统一状态字段为state）
const filteredTableData = computed(() => {
  let result = [...tableData.value];

  // 状态筛选（适配接口的state字符串）
  if (searchForm.status !== '') {
    result = result.filter(item => item.status === searchForm.status);
  }

  // 标题筛选（匹配接口的purpose字段）
  if (searchForm.roomName) {
    const keyword = searchForm.roomName.toLowerCase();
    result = result.filter(item =>
      item.roomName.toLowerCase().includes(keyword)
    );
  }

  // 分页处理
  const startIndex = (pagination.currentPage - 1) * pagination.pageSize;
  return result.slice(startIndex, startIndex + pagination.pageSize);
});

/**
 * 加载待审批预约列表（修复：筛选条件的状态字段为state）
 */
const loadPendingList = async () => {
  isLoading.value = true;
  try {
    const res = await getPendingApprovalList();
    console.log("接口返回数据：", res.data)
    if (res.data.code == 1) {
      // 直接赋值接口返回的列表
      tableData.value = res.data.data || [];
      console.log("表格数据：", tableData.value)
    } else {
      tableData.value = [];
      ElMessage.error(res.data.message || '加载待审批列表失败');
    }
    // 更新分页总数（移除日期筛选 ）
    const filtered = [...tableData.value].filter(item => {
      if (searchForm.status !== '' && item.status !== searchForm.status) return false;
      if (searchForm.roomName && !item.roomName.toLowerCase().includes(searchForm.roomName.toLowerCase())) return false;
      return true;
    });
    console.log("筛选后数据：", filtered)
    pagination.total = filtered.length;
  } catch (error) {
    console.error('加载待审批列表失败：', error);
    ElMessage.error('加载待审批列表失败');
    tableData.value = [];
    pagination.total = 0;
  } finally {
    isLoading.value = false;
  }
};

// 页面加载时加载数据
onMounted(() => {
  loadPendingList();
});

// 查询
const handleQuery = () => {
  pagination.currentPage = 1;
  loadPendingList();
  ElMessage.success('查询完成');
};

// 重置 - 移除date重置
const handleReset = () => {
  searchForm.status = '待确认';
  searchForm.roomName = '';

  pagination.currentPage = 1;
  loadPendingList();
};

// 分页-每页条数变化
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  loadPendingList();
};

// 分页-页码变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  loadPendingList();
};

/**
 * 查看详情 - 适配接口字段
 */
const handleViewDetail = async (row) => {
  isLoading.value = true;
  try {
    const res = await getReservationDetail({
      reservationId: row.reservationId,
      roomId: row.roomId
    });
    console.log("ressss",res)
    // 适配接口返回结构
    currentItem.value = res
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error('获取详情失败');
    console.error('详情加载失败:', error);
  } finally {
    isLoading.value = false;
  }
};

/**
 * 审批通过 - 适配接口的reservationId
 */
const handleApprove = async (row) => {
  ElMessageBox.confirm(
    `确认通过【${row.purpose}】的会议申请吗？`,
    '审核确认',
    {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'success',
      center: true
    }
  ).then(async () => {
    try {
      const res = await approveReservation({
        reservationId: row.reservationId,
        approveStatus: 4,
        rejectReason: ''
      });
      if (res.data.code === 1) {
        ElMessage.success('审核已通过');
        loadPendingList();
      } else {
        ElMessage.error(res.data.message || '审批失败');
      }
    } catch (error) {
      ElMessage.error('审批操作失败');
      console.error('审批失败:', error);
    }
  }).catch(() => {});
};

/**
 * 拒绝申请 - 适配接口的reservationId
 */
const handleReject = (row) => {
  currentItem.value = { ...row };
  rejectForm.reason = '';
  rejectDialogVisible.value = true;
};

/**
 * 确认拒绝 - 适配接口的reservationId
 */
const confirmReject = async () => {
  if (!rejectFormRef.value) return;

  try {
    await rejectFormRef.value.validate();
  } catch {
    ElMessage.warning('请完善拒绝原因');
    return;
  }

  try {
    const res = await approveReservation({
      reservationId: currentItem.value.reservationId,
      approveStatus: 1,
      rejectReason: rejectForm.reason
    });
    if (res.data.code === 1) {
      ElMessage.success('已拒绝该申请');
      rejectDialogVisible.value = false;
      loadPendingList();
    } else {
      ElMessage.error(res.data.message || '拒绝失败');
    }
  } catch (error) {
    ElMessage.error('拒绝操作失败');
    console.error('拒绝失败:', error);
  }
};
</script>
<style scoped>
.pending-review-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 主内容布局 - 左侧卡片列表，右侧功能区 */
.main-content-wrapper {
  display: flex;
  gap: 20px;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

/* 左侧卡片区域 */
.card-section {
  flex: 1;
  min-width: 0;
}

/* 右侧功能边栏 - 固定定位 */
.right-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 30px;
  min-width: 320px;
  position: sticky;
  top: 20px;
  align-self: flex-start;
  height: fit-content;
}

/* 搜索卡片样式 */
.search-card, .action-card, .pagination-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.search-card:hover, .action-card:hover, .pagination-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
}

/* 边栏标题 */
.sidebar-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  text-align: center;
  letter-spacing: 0.5px;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(64, 158, 255, 0.1) 100%);
  padding-top: 15px;
}

/* 搜索表单样式 */
.search-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 0 20px 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

/* 分页卡片样式 */
.pagination-card {
  margin-bottom: 0;
}

.pagination-controls {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 0 20px 20px;
}

.pagination-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 15px;
  background: rgba(240, 244, 255, 0.6);
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  text-align: center;
}

.page-size-control {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-nav-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.page-nav-buttons .el-button {
  flex: 1;
  max-width: 120px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  background: linear-gradient(135deg,
    #67c23a 0%,
    #85ce61 50%,
    #409eff 100%);
  border: none;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #fff;
  position: relative;
  overflow: hidden;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  letter-spacing: 0.5px;
  height: 42px;
}

.page-nav-buttons .el-button:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow:
    0 12px 28px rgba(103, 194, 58, 0.45),
    0 4px 16px rgba(103, 194, 58, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg,
    #85ce61 0%,
    #67c23a 50%,
    #667eea 100%);
}

.page-nav-buttons .el-button:active {
  transform: translateY(-2px) scale(1.02);
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-nav-buttons .el-button:disabled {
  background: rgba(204, 204, 204, 0.8);
  border: none;
  box-shadow: none;
  transform: none;
  cursor: not-allowed;
  text-shadow: none;
}

.search-card {
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.flex-wrap {
  flex-wrap: wrap;
}

.mt-3 {
  margin-top: 15px;
}

/* 卡片列表容器 */
.card-list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

/* 审批卡片样式 */
.approval-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.approval-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.16);
  border-color: rgba(103, 194, 58, 0.3);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(64, 158, 255, 0.1) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.meeting-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  margin-right: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.status-tag {
  font-weight: 600;
  border-radius: 16px;
  padding: 6px 16px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.status-tag:hover {
  transform: scale(1.05);
}

/* 卡片主体 */
.card-body {
  padding: 20px;
}

/* 会议信息区域 */
.meeting-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(240, 244, 255, 0.6);
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 15px;
  min-height: 48px;
}

.info-item:hover {
  background: rgba(240, 244, 255, 0.9);
  transform: translateX(3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.info-icon {
  font-size: 18px;
  color: #67c23a;
  margin-right: 4px;
  flex-shrink: 0;
}

.info-label {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  min-width: 90px;
  flex-shrink: 0;
  white-space: nowrap;
}

.info-value {
  color: #606266;
  font-size: 14px;
  flex: 1;
  word-break: break-all;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 卡片操作按钮 */
.card-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.3);
}

.card-actions .el-button {
  flex: 1;
  min-width: 110px;
  border-radius: 8px;
  font-weight: 600;
  height: 44px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
}

.view-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
}

.view-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.4);
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
}

.approve-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
}

.approve-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.4);
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
}

.reject-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: #fff;
}

.reject-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(245, 108, 108, 0.4);
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
}

/* 查询和重置按钮样式 */
.query-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
  border-radius: 8px;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.query-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.4);
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
}

.reset-btn {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
  color: #fff;
  border-radius: 8px;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.reset-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(144, 147, 153, 0.4);
  background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 20px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.custom-pagination {
  text-align: right;
}

/* 对话框样式 */
.detail-dialog, .reject-dialog {
  animation: dialogFadeIn 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .card-list-container {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 1024px) {
  /* 中等屏幕下调整布局 */
  .main-content-wrapper {
    flex-direction: column;
  }

  .right-sidebar {
    width: 100%;
    min-width: auto;
    position: relative;
    top: 0;
  }
}

@media (max-width: 768px) {
  .pending-review-page {
    padding: 15px;
  }

  .main-content-wrapper {
    flex-direction: column;
    gap: 15px;
  }

  .card-list-container {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .approval-card {
    margin: 0;
  }

  .meeting-info {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .card-actions {
    flex-direction: row;
    gap: 8px;
  }

  .card-actions .el-button {
    flex: 1;
    width: auto;
    min-width: 0;
  }

  .custom-pagination {
    text-align: center;
  }

  /* 右侧边栏在移动端调整为上下布局 */
  .right-sidebar {
    width: 100%;
    min-width: auto;
    position: relative;
    top: 0;
  }

  /* 搜索表单和分页控制在移动端调整 */
  .search-form {
    padding: 0 15px 15px;
  }

  .pagination-controls {
    padding: 0 15px 15px;
  }

  /* 弹窗在移动端调整大小 */
  .detail-dialog {
    width: 90% !important;
    margin: 20px 0;
  }

  .reject-dialog {
    width: 90% !important;
    margin: 20px 0;
  }
}

@media (max-width: 480px) {
  .pending-review-page {
    padding: 10px;
  }

  .card-list-container {
    gap: 12px;
  }

  .card-header {
    padding: 15px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .meeting-title {
    font-size: 16px;
    margin-right: 0;
  }

  .card-body {
    padding: 15px;
  }

  .info-item {
    padding: 8px 12px;
    font-size: 13px;
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }

  .info-label {
    min-width: auto;
  }

  .card-actions {
    flex-direction: row;
    gap: 8px;
    padding-top: 15px;
  }

  .card-actions .el-button {
    flex: 1;
    width: auto;
    min-width: 0;
    font-size: 12px;
    padding: 6px;
  }

  /* 搜索按钮和重置按钮在移动端调整 */
  .query-btn,
  .reset-btn {
    padding: 12px;
  }

  /* 分页按钮在移动端调整 */
  .page-nav-buttons {
    flex-direction: column;
  }

  .page-nav-buttons .el-button {
    max-width: none;
  }
}
</style>
