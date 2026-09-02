<template>
  <div class="approved-meeting-list">
    <div class="main-content-wrapper">
      <!-- 左侧卡片区域 -->
      <div class="card-section">
        <!-- 卡片列表区域 -->
        <div class="card-list-container">
          <div
            v-for="item in filteredTableData"
            :key="item.id"
            class="approved-card"
          >
            <!-- 左侧信息区域 -->
            <div class="card-left">
              <div class="card-header">
                <!-- 修复：显示会议名称 -->
                <div class="meeting-title">预约缘由：{{ item.purpose }}</div>
                <el-tag
                  :type="getStatusType(item.status)"
                  effect="light"
                  class="status-tag"
                >
                  {{ item.status }}
                </el-tag>
              </div>

              <div class="card-body">
                <div class="info-container">
                  <div class="info-item">
                    <el-icon class="info-icon"><Location /></el-icon>
                    <span class="info-label">会议室：</span>
                    <span class="info-value">{{ item.roomName }}</span>
                  </div>
                  <div class="info-item">
                    <el-icon class="info-icon"><Calendar /></el-icon>
                    <span class="info-label">会议日期：</span>
                    <span class="info-value">{{ item.reserveDate }}</span>
                  </div>
                  <div class="info-item">
                    <el-icon class="info-icon"><Clock /></el-icon>
                    <span class="info-label">时间：</span>
                    <span class="info-value">{{ item.startTime }} - {{ item.endTime }}</span>
                  </div>
                  <div class="info-item">
                    <el-icon class="info-icon"><User /></el-icon>
                    <span class="info-label">发起人：</span>
                    <span class="info-value">{{ item.username }}</span>
                  </div>
                  <div class="info-item full-width">
                    <el-icon class="info-icon"><Clock /></el-icon>
                    <span class="info-label">通过时间：</span>
                    <span class="info-value">{{ item.approveTime}}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧操作区域 -->

          </div>
        </div>
      </div>

      <!-- 右侧功能区域 -->
      <div class="right-sidebar">
        <!-- 统一操作盒子 -->
        <el-card class="action-box">
          <div class="unified-actions">
            <!-- 查询区域 -->
            <div class="search-section">
              <div class="form-item">
                <el-input
                  v-model="searchForm.roomName"
                  placeholder="请输入会议名称查询"
                  class="w-full"
                  clearable
                  prefix-icon="Search"
                  @keyup.enter="handleQuery"
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

            <!-- 分页控制区域 -->
            <div class="pagination-section">
              <div class="pagination-info">
                <span>第 {{ pagination.currentPage }} / {{ Math.ceil(pagination.total / pagination.pageSize) }} 页</span>
                <span>共 {{ pagination.total }} 条记录</span>
              </div>
              <div class="page-size-control">
                <el-select
                  v-model="pagination.pageSize"
                  class="w-full"
                  @change="handleSizeChange"
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
                >
                  <el-icon><ArrowLeft /></el-icon>
                  上一页
                </el-button>
                <el-button
                  type="primary"
                  @click="handleCurrentChange(pagination.currentPage + 1)"
                  :disabled="pagination.currentPage === Math.ceil(pagination.total / pagination.pageSize)"
                >
                  下一页
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import {
  Search,
  Delete,
  Location,
  Calendar,
  Clock,
  User,
  UserFilled,
  Refresh,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
// 导入对接后端的API函数
import { getApprovedListAPI, deleteApprovedRecordAPI } from '@/apis/managerappoint.js';

// 搜索表单
const searchForm = reactive({
  title: '',
  roomName:''
});

// 原始表格数据
const tableData = ref([]);
// 加载状态
const isLoading = ref(false);
// 分页控制
const pagination = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0
});

// 过滤后的表格数据（实现前端搜索）
const filteredTableData = computed(() => {
  // 修复：使用roomName进行过滤，匹配会议室名称或会议标题
  if (!searchForm.roomName) {
    return tableData.value;
  }
  const keyword = searchForm.roomName.toLowerCase();
  return tableData.value.filter(item =>
    (item.roomName && item.roomName.toLowerCase().includes(keyword)) ||
    (item.title && item.title.toLowerCase().includes(keyword))
  );
});

// 根据状态获取标签类型（增加后端返回的“已过期”状态）
const getStatusType = (status) => {
  switch(status) {
    case '进行中':
    case '已通过':
      return 'success';
    case '已取消':
    case '已拒绝':
    case '已过期':
      return 'danger';
    case '待开始':
      return 'warning';
    case '已结束':
      return 'info';
    default:
      return 'info';
  }
};

/**
 * 修复核心：加载已审批列表
 */
const loadApprovedList = async () => {
  isLoading.value = true;
  try {
    const res = await getApprovedListAPI();
    console.log("dsadas",res.data)
    if (res.data.code === 1) {
      let allData = res.data.data || [];

      // 修复：应用roomName搜索过滤
      if (searchForm.roomName) {
        const keyword = searchForm.roomName.toLowerCase();
        allData = allData.filter(item =>
          (item.roomName && item.roomName.toLowerCase().includes(keyword)) ||
          (item.title && item.title.toLowerCase().includes(keyword))
        );
      }

      // 模拟分页
      pagination.total = allData.length;
      const startIndex = (pagination.currentPage - 1) * pagination.pageSize;
      tableData.value = allData.slice(startIndex, startIndex + pagination.pageSize);
    } else {
      tableData.value = [];
      pagination.total = 0;
      ElMessage.error(res.data.message || '加载已审批列表失败');
    }
  } catch (error) {
    console.error('加载已审批列表失败：', error);
    ElMessage.error('加载已审批列表失败');
    tableData.value = [];
    pagination.total = 0;
  } finally {
    isLoading.value = false;
  }
};

// 页面加载时初始化数据
onMounted(() => {
  loadApprovedList();
});

// 查询
const handleQuery = () => {
  pagination.currentPage = 1; // 重置页码
  loadApprovedList();
};

// 修复：重置时清空roomName
const handleReset = () => {
  searchForm.title = '';
  searchForm.roomName = '';
  pagination.currentPage = 1;
  loadApprovedList();
};

// 分页-每页条数变化
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  loadApprovedList();
};

// 分页-页码变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  loadApprovedList();
};

/**
 * 单个删除
 */
const handleDelete = async (id) => {
  console.log("id",id)
  try {
    await ElMessageBox.confirm(
      '确认删除该已审批会议记录吗？删除后无法恢复！',
      '删除确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
        center: true
      }
    );

    const res = await deleteApprovedRecordAPI(id);
    console.log("res",res)
    // 接口文档中成功返回code为1
    if (res.data.code === 1) {
      ElMessage.success('删除成功');
      loadApprovedList();
    } else {
      ElMessage.error(res.data.message || '删除失败');
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      ElMessage.error('删除操作异常，请重试');
      console.error('删除异常:', error);
    }
  }
};
</script>
<style scoped>
.approved-meeting-list {
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

.action-card {
  margin-bottom: 20px;
}

/* 卡片列表容器 */
.card-list-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

/* 审批记录卡片样式 - 纵向布局的横屏卡片 */
.approved-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  min-height: 180px;
}

.approved-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.16);
  border-color: rgba(64, 158, 255, 0.3);
}

/* 卡片左侧内容区域 */
.card-left {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(103, 194, 58, 0.1) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  border-bottom: none;
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
  white-space: nowrap;
}

.status-tag {
  font-weight: 600;
  border-radius: 16px;
  padding: 6px 16px;
  font-size: 14px;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.status-tag:hover {
  transform: scale(1.05);
}

/* 卡片主体 */
.card-body {
  padding: 15px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
}

/* 信息项容器 */
.info-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #ebeef5;
  margin-bottom: 15px;
}

/* 信息项 */
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(240, 244, 255, 0.6);
  border-radius: 6px;
  transition: all 0.3s ease;
  font-size: 14px;
  min-height: 40px;
  flex: 1;
  min-width: calc(50% - 5px);
  max-width: calc(50% - 5px);
}

.info-item:hover {
  background: rgba(240, 244, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

/* 全宽信息项 */
.info-item.full-width {
  min-width: 100%;
  max-width: 100%;
}

/* 信息图标 */
.info-icon {
  font-size: 18px;
  color: #67c23a;
  margin-right: 4px;
  flex-shrink: 0;
}

/* 信息标签 */
.info-label {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  min-width: 90px;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 信息值 */
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

/* 会议信息区域 */
.meeting-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 0;
  flex: 1;
  justify-content: center;
}

/* 移动端信息项容器调整 */
@media (max-width: 768px) {
  .info-container {
    gap: 8px !important;
    padding: 12px !important;
  }

  .info-item {
    min-width: calc(50% - 4px) !important;
    max-width: calc(50% - 4px) !important;
    padding: 6px 10px !important;
    font-size: 13px !important;
    min-height: 36px !important;
  }

  .info-icon {
    font-size: 16px !important;
  }

  .info-label {
    font-size: 13px !important;
    min-width: 80px !important;
  }

  .info-value {
    font-size: 13px !important;
  }
}

/* 小屏幕移动端调整 */
@media (max-width: 480px) {
  .info-container {
    gap: 6px !important;
    padding: 10px !important;
  }

  .info-item {
    min-width: calc(50% - 3px) !important;
    max-width: calc(50% - 3px) !important;
    padding: 5px 8px !important;
    font-size: 12px !important;
    min-height: 32px !important;
  }

  .info-icon {
    font-size: 14px !important;
  }

  .info-label {
    font-size: 12px !important;
    min-width: 70px !important;
  }

  .info-value {
    font-size: 12px !important;
  }
}



/* 卡片右侧操作区域 */
.card-right {
  width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(240, 244, 255, 0.3);
}

/* 卡片操作按钮 */
.card-actions {
  display: flex;
  gap: 0;
  justify-content: center;
  padding-top: 0;
  border-top: none;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.card-actions .el-button {
  min-width: 100%;
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

.delete-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: #fff;
  width: 100%;
}

.delete-btn:hover {
  transform: translateY(-2px) scale(1.03);
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

.custom-pagination .el-pagination__sizes {
  margin-right: 10px;
}

.custom-pagination .el-pagination__total {
  margin-right: 10px;
  color: #666;
}

.custom-pagination .el-pager li {
  margin: 0 5px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.custom-pagination .el-pager li:hover {
  color: #409eff;
  border-color: #409eff;
}

.custom-pagination .el-pager li.active {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
}

/* 修复删除确认弹窗样式 */
:deep(.custom-confirm-box) {
  width: 400px !important;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
}

:deep(.custom-confirm-box .el-message-box__header) {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.1) 0%, rgba(245, 108, 108, 0.1) 100%);
}

:deep(.custom-confirm-box .el-message-box__title) {
  font-size: 16px;
  font-weight: 500;
}

:deep(.custom-confirm-box .el-message-box__content) {
  padding: 20px;
  font-size: 14px;
  color: #606266;
}

:deep(.custom-confirm-box .el-message-box__btns) {
  padding: 10px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.custom-confirm-box .el-button) {
  min-width: 80px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

:deep(.custom-confirm-box .el-button--default) {
  background-color: #fff;
  border-color: #dcdcdc;
  color: #606266;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.custom-confirm-box .el-button--default:hover) {
  background-color: #f5f7fa;
  border-color: #bfcbd9;
  color: #606266;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

:deep(.custom-confirm-box .el-button--warning) {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border-color: #f56c6c;
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
}

:deep(.custom-confirm-box .el-button--warning:hover) {
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
  border-color: #f78989;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
}

/* 响应式调整 */
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
  /* 移动端主内容布局 */
  .main-content-wrapper {
    flex-direction: column !important;
    gap: 10px !important;
    padding: 5px !important;
  }

  /* 移动端右侧边栏 */
  .right-sidebar {
    width: 100% !important;
    min-width: auto !important;
    position: static !important;
    order: -1 !important;
    margin-bottom: 8px !important;
    padding: 0 !important;
  }

  /* 移动端统一操作盒子 */
  .action-box {
    padding: 6px !important;
    margin: 0 !important;
    width: 100% !important;
  }

  /* 移动端统一操作区域 */
  .unified-actions {
    gap: 6px !important;
    padding: 8px !important;
  }

  /* 移动端查询区域 */
  .search-section {
    gap: 6px !important;
  }

  /* 移动端分页控制区域 */
  .pagination-section {
    gap: 6px !important;
    padding: 8px !important;
  }

  /* 移动端表单项 */
  .form-item {
    gap: 4px !important;
  }

  /* 移动端输入框 */
  .el-input {
    font-size: 12px !important;
    height: 32px !important;
  }

  /* 移动端选择器 */
  .el-select {
    font-size: 12px !important;
    height: 32px !important;
  }

  /* 移动端查询按钮 */
  .query-btn {
    height: 36px !important;
    font-size: 13px !important;
    padding: 0 12px !important;
  }

  /* 移动端重置按钮 */
  .reset-btn {
    height: 36px !important;
    font-size: 13px !important;
    padding: 0 12px !important;
  }

  /* 移动端分页按钮容器 */
  .page-nav-buttons {
    display: flex !important;
    gap: 12px !important;
    justify-content: space-between !important;
    width: 100% !important;
    flex-direction: row !important;
  }

  /* 移动端分页按钮 */
  .page-nav-buttons .el-button {
    height: 40px !important;
    font-size: 14px !important;
    padding: 0 16px !important;
    flex: 1 !important;
    max-width: calc(50% - 6px) !important;
    text-align: center !important;
    min-width: 0 !important;
    white-space: nowrap !important;
  }

  /* 移动端分页信息 */
  .pagination-info {
    padding: 6px 10px !important;
    font-size: 12px !important;
    gap: 4px !important;
  }

  /* 移动端每页显示控制 */
  .page-size-control {
    gap: 4px !important;
  }

  .approved-meeting-list {
    padding: 15px;
  }

  .card-list-container {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .approved-card {
    margin: 0;
    flex-direction: column;
    min-height: auto;
  }

  .card-left {
    width: 100%;
  }

  .card-header {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  }

  .card-body {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  }

  .card-right {
    width: 100%;
    padding: 15px;
  }

  .meeting-info {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .card-actions {
    justify-content: center;
    flex-direction: column;
    width: 100%;
  }

  .custom-confirm-box, .el-dialog {
    width: 90% !important;
    margin: 20px 0;
  }

  .pagination-container {
    padding: 15px;
  }

  .custom-pagination {
    text-align: center;
  }
}

@media (max-width: 480px) {
  .approved-meeting-list {
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
    gap: 8px;
    padding-top: 15px;
  }

  /* 搜索按钮和重置按钮在移动端调整 */
  .query-btn,
  .reset-btn {
    padding: 12px;
  }

  /* 分页按钮在移动端调整 */
  .page-nav-buttons {
    flex-direction: row !important;
    gap: 6px !important;
  }

  .page-nav-buttons .el-button {
    max-width: calc(50% - 3px) !important;
    font-size: 12px !important;
    padding: 0 10px !important;
  }
}

/* 确保分页按钮容器在所有设备上都能正确显示 */
.pagination-section .page-nav-buttons {
  display: flex !important;
  gap: 12px !important;
  justify-content: space-between !important;
  width: 100% !important;
  flex-direction: row !important;
}

/* 确保分页按钮在所有设备上都能正确显示 */
.pagination-section .page-nav-buttons .el-button {
  flex: 1 !important;
  max-width: calc(50% - 6px) !important;
  text-align: center !important;
  height: 40px !important;
  font-size: 14px !important;
  padding: 0 16px !important;
  min-width: 0 !important;
  white-space: nowrap !important;
}
</style>
