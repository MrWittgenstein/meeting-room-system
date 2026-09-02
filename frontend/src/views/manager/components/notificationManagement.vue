<template>
  <div class="notification-management">
    <div class="page-header">
      <h2>消息通知管理</h2>
      <el-button type="primary" @click="handleAddNotification">
        <el-icon><Plus /></el-icon>发布新通知
      </el-button>
    </div>

    <div class="header">
      <div class="header-right">
        <el-radio-group v-model="mode" @change="handleSearch">
          <el-radio-button label="未过期">未过期</el-radio-button>
          <el-radio-button label="已过期">已过期</el-radio-button>
          <el-radio-button label="全部">全部</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="searchForm.type" placeholder="通知类型" clearable @change="handleSearch">
        <el-option label="维修" value="维修" />
        <el-option label="暂停使用" value="暂停使用" />
        <el-option label="恢复使用" value="恢复使用" />
        <el-option label="其他重要通知" value="其他重要通知" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="标题/内容关键字" clearable @input="handleSearch" />
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-card class="list-card" v-loading="loading">
      <el-empty v-if="notificationList.length === 0" description="暂无通知" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in notificationList" :key="item.id" :timestamp="formatRange(item.publishTime, item.expireTime)">
          <div class="item">
            <div class="title">
              <el-tag :type="getTagType(item.type)">{{ item.type }}</el-tag>
              <span class="text">{{ item.title }}</span>
              <el-tag :type="getStatusType(item.status)" class="status" size="small">{{ item.status }}</el-tag>
            </div>
            <div class="content">{{ item.content }}</div>
            <div class="meta">
              <span>发布人：{{ item.publisher }}</span>
              <div class="item-actions">

                <el-button type="warning" @click="handleEdit(item)" class="custom-button edit-btn">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="danger" @click="handleDelete(item)" class="custom-button delete-btn">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 发布通知弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="60%"
      :close-on-click-modal="false"
    >
      <el-form :model="notificationForm" :rules="rules" ref="notificationFormRef" label-width="100px">
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="notificationForm.title" placeholder="请输入通知标题"></el-input>
        </el-form-item>
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="notificationForm.type" placeholder="请选择通知类型">
            <el-option label="维修" value="维修"></el-option>
            <el-option label="暂停使用" value="暂停使用"></el-option>
            <el-option label="恢复使用" value="恢复使用"></el-option>
            <el-option label="其他重要通知" value="其他重要通知"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="notificationForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入通知内容"
          ></el-input>
        </el-form-item>
        <el-form-item label="通知状态" prop="status">
          <el-select v-model="notificationForm.status" placeholder="请选择通知状态">
            <el-option label="未过期" value="未过期"></el-option>
            <el-option label="已过期" value="已过期"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="notificationForm.publishTime"
            type="datetime"
            placeholder="请选择生效时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="notificationForm.expireTime"
            type="datetime"
            placeholder="请选择过期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Delete } from '@element-plus/icons-vue'
import { getImportantNewsAll, addImportantNews, updateImportantNews, deleteImportantNews } from '@/apis/importantnews'

// 页面数据
const loading = ref(false)
const notificationList = ref([])
const mode = ref('全部')
const searchForm = reactive({
  type: '',
  status: '',
  dateRange: [],
  keyword: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 时间范围格式化
const formatRange = (start, end) => {
  const s = start || ''
  const e = end || ''
  return e ? `${s} ~ ${e}` : s
}

// 弹窗数据
const dialogVisible = ref(false)
const dialogTitle = ref('发布新通知')
const notificationFormRef = ref()
const notificationForm = reactive({
  id: '',
  title: '',
  type: 'system',
  content: '',
  status: '未过期',
  publishTime: '',
  expireTime: '',
  publisher: '管理员'
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择通知类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择通知状态', trigger: 'change' }
  ]
}

// 获取通知类型标签样式
const getTagType = (type) => {
  const typeMap = {
    '维修': 'danger',
    '暂停使用': 'warning',
    '恢复使用': 'success',
    '其他重要通知': 'info'
  }
  return typeMap[type] || 'info'
}

// 获取状态标签样式
const getStatusType = (status) => {
  const statusMap = {
    '未过期': 'success',
    '已过期': 'danger'
  }
  return statusMap[status] || 'info'
}

// 搜索通知
const handleSearch = async () => {
  loading.value = true
  try {
    const res = await getImportantNewsAll()
    console.log("aaa",res)
    if (res.status == 200) {
      const allNotifications = res.data || []

      // 转换数据格式
      const formattedNotifications = allNotifications.map(item => ({
        id: item.id,
        title: item.title,
        type: item.newsType || '其他重要通知',
        content: item.content,
        status: item.status || '未过期',
        publishTime: item.startTime,
        expireTime: item.endTime,
        publisher: item.publisherName || '管理员'
      }))

      // 应用搜索过滤
      let filteredNotifications = [...formattedNotifications]

      // 类型过滤
      if (searchForm.type) {
        filteredNotifications = filteredNotifications.filter(item => item.type === searchForm.type)
      }

      // 状态过滤（根据mode）
      if (mode.value !== '全部') {
        filteredNotifications = filteredNotifications.filter(item => item.status === mode.value)
      }

      // 关键字过滤
      if (searchForm.keyword) {
        const keyword = searchForm.keyword.toLowerCase()
        filteredNotifications = filteredNotifications.filter(item =>
          item.title.toLowerCase().includes(keyword) ||
          item.content.toLowerCase().includes(keyword)
        )
      }

      // 日期范围过滤
      if (searchForm.dateRange && searchForm.dateRange.length === 2) {
        const startDate = new Date(searchForm.dateRange[0])
        const endDate = new Date(searchForm.dateRange[1])
        filteredNotifications = filteredNotifications.filter(item => {
          const publishDate = new Date(item.publishTime)
          return publishDate >= startDate && publishDate <= endDate
        })
      }

      notificationList.value = filteredNotifications
      pagination.total = notificationList.value.length
    } else {
      notificationList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('获取通知列表失败：', error)
    notificationList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    type: '',
    status: '',
    dateRange: [],
    keyword: ''
  })
  mode.value = '全部'
  handleSearch()
}

// 分页相关函数已移除，改为直接展示所有通知

// 添加通知
const handleAddNotification = () => {
  dialogTitle.value = '发布新通知'
  // 重置表单
  Object.assign(notificationForm, {
    id: '',
    title: '',
    type: '',
    content: '',
    status: '未过期',
    publishTime: '',
    expireTime: '',
    publisher: '管理员'
  })
  dialogVisible.value = true
}

// 编辑通知
const handleEdit = (row) => {
  dialogTitle.value = '编辑通知'
  // 复制行数据到表单
  Object.assign(notificationForm, row)
  dialogVisible.value = true
}

// 查看通知
const handleView = (row) => {
  // 这里可以打开查看详情弹窗，或者跳转到详情页面
  ElMessage.info(`查看通知ID：${row.id}`)
}

// 删除通知
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调用删除API
    await deleteImportantNews(row.id)
    ElMessage.success('删除成功')

    // 重新加载数据
    await handleSearch()
  } catch (error) {
    // 取消删除或删除失败
    if (error.message !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  }
}

// 提交表单
const handleSubmit = () => {
  notificationFormRef.value.validate(async (valid) => {
    if (!valid) return false
    loading.value = true
    try {
      const toIso = (v) => {
        if (!v) return ''
        return String(v).includes('T') ? v : String(v).replace(' ', 'T')
      }
      const now = new Date()
      const pad = (n) => (n < 10 ? '0' + n : '' + n)
      const nowIso = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
      const payload = {
        id: notificationForm.id || undefined,
        title: notificationForm.title,
        content: notificationForm.content,
        newsType: notificationForm.type,
        startTime: toIso(notificationForm.publishTime) || nowIso,
        endTime: toIso(notificationForm.expireTime),
        status: notificationForm.status,
        publisherName: notificationForm.publisher
      }
      if (notificationForm.id) {
        await updateImportantNews(payload)
        ElMessage.success('通知更新成功')
      } else {
        await addImportantNews(payload)
        ElMessage.success('通知发布成功')
      }
      dialogVisible.value = false
      await handleSearch()
    } catch (error) {
      ElMessage.error(notificationForm.id ? '编辑失败' : '发布失败')
      console.error('提交失败:', error)
    } finally {
      loading.value = false
    }
  })
}

// 组件挂载后加载数据
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
/* 消息通知管理页面样式 */
.notification-management {
  width: 100%;
  min-height: 100%;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 15px;
  box-sizing: border-box;
  overflow: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.3);
}

.page-header h2 {
  font-size: 20px;
  color: #666;
  font-weight: 700;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
  margin: 0;
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.3);
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  flex-wrap: wrap;
  padding: 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.list-card {
  min-height: 200px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(255, 255, 255, 0.3);
}

.item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 10px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.2);
  background: rgba(255, 255, 255, 0.9);
  margin: 0 8px 10px 8px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-weight: 600;
  line-height: 1.4;
}

.title .text {
  font-weight: 700;
  font-size: 14px;
  flex: 1;
  color: #666;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

.content {
  color: #606266;
  line-height: 1.5;
  padding: 8px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  border-left: 3px solid rgba(179, 219, 225, 0.3);
  font-size: 13px;
}

.meta {
  display: flex;
  gap: 15px;
  color: #909399;
  font-size: 12px;
  flex-wrap: wrap;
  padding: 8px 0 0 0;
  border-top: 1px dashed rgba(179, 219, 225, 0.2);
  justify-content: space-between;
  align-items: center;
}

.item-actions {
  display: flex;
  gap: 6px;
}

.status {
  margin-left: auto;
}

/* 时间轴样式 */
:deep(.el-timeline) {
  padding: 0 15px;
  margin: 0;
}

:deep(.el-timeline-item) {
  margin-bottom: 12px;
  padding-bottom: 0;
  border-bottom: none;
}

:deep(.el-timeline-item:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 11px;
  margin-bottom: 6px;
  line-height: 1.2;
}

:deep(.el-timeline-item__tail) {
  background-color: rgba(179, 219, 225, 0.3);
  width: 2px;
}

:deep(.el-timeline-item__node) {
  background-color: rgba(179, 219, 225, 1);
  border-color: rgba(179, 219, 225, 1);
  box-shadow: 0 2px 6px rgba(179, 219, 225, 0.3);
  width: 10px;
  height: 10px;
}


/* 表单元素样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 自定义按钮样式 */
.custom-button {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 80px;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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

.edit-btn {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
  color: #fff;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(230, 162, 60, 0.4);
  background: linear-gradient(135deg, #ebb563 0%, #e6a23c 100%);
}

.delete-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: #fff;
}

.delete-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(245, 108, 108, 0.4);
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
}

/* 适配小屏幕 */
@media (max-width: 768px) {
  .notification-management {
    padding: 10px;
  }

  .page-header {
    margin-bottom: 12px;
    padding-bottom: 6px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .header {
    margin-bottom: 12px;
    padding-bottom: 6px;
  }

  .filters {
    flex-direction: column;
    gap: 8px;
    margin-bottom: 12px;
    padding: 10px;
  }

  .filter-actions {
    width: 100%;
    justify-content: space-between;
    gap: 6px;
  }

  .filters .el-select,
  .filters .el-input {
    width: 100%;
  }

  .list-card {
    border-radius: 10px;
  }

  .item {
    padding: 10px 8px;
    margin: 0 6px 8px 6px;
    gap: 4px;
  }

  .title {
    gap: 6px;
  }

  .title .text {
    font-size: 13px;
  }

  .content {
    padding: 6px;
    font-size: 12px;
    line-height: 1.4;
  }

  .meta {
    flex-direction: column;
    gap: 6px;
    align-items: flex-start;
    padding: 6px 0 0 0;
    font-size: 11px;
  }

  .item-actions {
    margin-top: 6px;
    width: 100%;
    display: flex;
    justify-content: space-between;
    gap: 4px;
  }

  .custom-button {
    flex: 1;
    margin: 0 3px;
    padding: 6px 12px;
    font-size: 12px;
    min-width: 60px;
  }

  .custom-button:first-child {
    margin-left: 0;
  }

  .custom-button:last-child {
    margin-right: 0;
  }

  .status {
    margin-left: 0;
    align-self: flex-start;
  }

  /* 时间轴样式 - 移动端 */
  :deep(.el-timeline) {
    padding: 0 10px;
  }

  :deep(.el-timeline-item) {
    margin-bottom: 10px;
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: 10px;
    margin-bottom: 4px;
  }

  :deep(.el-timeline-item__node) {
    width: 8px;
    height: 8px;
  }
}
</style>
