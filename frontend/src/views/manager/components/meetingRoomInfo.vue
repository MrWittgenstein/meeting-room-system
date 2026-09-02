<template>
  <div class="conference-room-page">
    <!-- 主要内容区域：左侧卡片列表，右侧功能区 -->
    <div class="main-content-wrapper">
      <!-- 左侧卡片区域 -->
      <div class="card-section">
        <!-- 精美的卡片区域 - 纵向分布 -->
        <div class="card-container">
          <div
            v-for="row in filteredTableData"
            :key="row.roomId"
            class="room-card"
          >
            <!-- 卡片内容 -->
            <div class="card-content">
              <div class="card-header">
                <div class="room-type">{{ row.categoryName }}</div>
                <el-tag
                  :type="row.status === 1 ? 'info' : 'success'"
                  effect="light"
                  class="status-tag"
                >
                  {{ row.status === 1 ? '维修' : '可用' }}
                </el-tag>
              </div>
              <div class="room-main-content">
                <div class="room-image">
                  <el-image
                    v-if="row.originImage"
                    :src="row.originImage"
                    fit="cover"
                    @click.stop="handleImagePreview(row.originImage)"
                  />
                  <div v-else class="no-image">
                    <el-icon class="no-image-icon"><Camera /></el-icon>
                    <span>无图片</span>
                  </div>
                </div>
                <div class="room-main-info">
                  <h3 class="room-name">{{ row.roomName }}</h3>
                  <div class="basic-info">
                    <div class="info-grid">
                      <div class="info-item">
                        <el-icon class="info-icon"><Location /></el-icon>
                        <span class="info-label">门牌号：</span>
                        <span class="info-value">{{ row.roomNumber }}</span>
                      </div>
                      <div class="info-item">
                        <el-icon class="info-icon"><User /></el-icon>
                        <span class="info-label">可容纳：</span>
                        <span class="info-value">{{ row.capacity }}人</span>
                      </div>
                      <div class="info-item">
                        <el-icon class="info-icon"><Location /></el-icon>
                        <span class="info-label">位置：</span>
                        <span class="info-value">{{ row.location }}</span>
                      </div>
                      <div class="info-item">
                        <el-icon class="info-icon"><Clock /></el-icon>
                        <span class="info-label">开放时间：</span>
                        <span class="info-value">{{ row.openTime }} - {{ row.closeTime }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="equipment-info">
                    <div class="info-item full-width">
                      <el-icon class="info-icon"><Monitor /></el-icon>
                      <span class="info-label">类型：</span>
                      <span class="info-value equipment-list">{{ row.type || '基础设备' }}</span>
                    </div>
                    <div class="info-item full-width">
                      <el-icon class="info-icon"><Document /></el-icon>
                      <span class="info-label">描述：</span>
                      <span class="info-value description-text">{{ row.description || '暂无描述' }}</span>
                    </div>
                  </div>
                  <div class="card-actions">
                    <el-button
                      size="small"
                      type="primary"
                      @click.stop="handleEdit(row)"
                      class="edit-btn"
                    >
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button
                      size="small"
                      type="danger"
                      @click.stop="handleDelete(row.roomId)"
                      class="del-btn"
                    >
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
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
                  v-model="searchForm.name"
                  placeholder="请输入会议室名称或门牌号查询"
                  class="w-full"
                  clearable
                />
              </div>
              <div class="form-item">
                <el-button type="primary" @click="handleQuery" class="w-full query-btn">
                  <el-icon><Search /></el-icon>
                  <span>查询</span>
                </el-button>
              </div>
            </div>

            <!-- 操作按钮区域 -->
            <div class="action-section">
              <el-button
                type="primary"
                @click="handleAdd"
                class="add-btn w-full"
              >
                <el-icon><Plus /></el-icon>
                <span>新增会议室</span>
              </el-button>
            </div>

            <!-- 分页控制区域 -->
            <div class="pagination-section">
              <div class="page-size-control">
                <el-select
                  v-model="pagination.pageSize"
                  class="w-full"
                  @change="handleSizeChange"
                  size="small"
                  :teleported="false"
                  placement="top"
                >
                  <el-option label="5 条" :value="5" />
                  <el-option label="10 条" :value="10" />
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
          </div>
        </el-card>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑会议室' : '新增会议室'"
      width="600px"
      :before-close="handleDialogClose"
      class="custom-dialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="会议室类型" prop="categoryName">
          <el-select v-model="formData.categoryName" placeholder="请选择分类" class="w-full" :teleported="false">
            <el-option label="多媒体会议室" value="多媒体会议室" />
            <el-option label="普通会议室" value="普通会议室" />
            <el-option label="主席台" value="主席台" />
          </el-select>
        </el-form-item>
        <el-form-item label="会议室名称" prop="roomName">
          <el-input v-model="formData.roomName" />
        </el-form-item>
        <el-form-item label="门牌号" prop="roomNumber">
          <el-input v-model="formData.roomNumber" />
        </el-form-item>
        <el-form-item label="会议室描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-upload
            class="upload-demo w-full"
            :http-request="handleCustomUpload"
            :on-preview="handleUploadPreview"
            :on-remove="handleRemove"
            :on-success="handleUploadSuccess"
            :limit="1"
            :file-list="fileList"
            list-type="picture-card"
            :auto-upload="true"
          >
            <el-icon><Plus /></el-icon>
            <div class="el-upload__text">点击上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="可容纳人数" prop="capacity">
          <el-input v-model.number="formData.capacity" type="number" min="1" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="formData.location" clearable />
        </el-form-item>
        <el-form-item label="开放时间" prop="openTime">
          <el-time-picker
            v-model="formData.openTime"
            placeholder="选择开放时间"
            class="w-full"
            value-format="HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="关闭时间" prop="closeTime">
          <el-time-picker
            v-model="formData.closeTime"
            placeholder="选择关闭时间"
            class="w-full"
            value-format="HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" class="w-full" :teleported="false">
            <el-option label="维修" :value="1" />
            <el-option label="可用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 图片预览组件 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="[previewImage]"
      @close="handlePreviewClose"
      teleport="body"
      class="image-viewer"
    />
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { Plus, Delete, Edit, Camera, Location, User, Clock, Monitor, Document, ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, ElForm, ElImageViewer } from 'element-plus';
import {
  getMeetingRoomList,
  addMeetingRoom,
  updateMeetingRoom,
  deleteMeetingRoom,
  uploadMeetingRoomImage
} from '@/apis/managermeetingroom.js';
// 搜索表单
const searchForm = reactive({
  type: '',
  name: ''
});
// 会议室数据
const tableData = ref([]);
// 分页控制（修复：基于接口返回的总条数，而非前端切片）
const pagination = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0 // 由接口返回的总条数赋值
});
// 修复：筛选逻辑保留，移除前端分页切片（分页由接口处理）
const filteredTableData = computed(() => {
  let result = [...tableData.value];
  // 类型筛选
  if (searchForm.type) {
    result = result.filter(item => item.categoryName === searchForm.type);
  }
  // 名称筛选：roomNumber转字符串后匹配
  if (searchForm.name) {
    const keyword = searchForm.name.toLowerCase();
    result = result.filter(item =>
      item.roomName.toLowerCase().includes(keyword) ||
      String(item.roomNumber).toLowerCase().includes(keyword)
    );
  }
  return result;
});

// 弹窗相关
const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);

// 统计信息计算
const availableRoomsCount = computed(() => {
  return tableData.value.filter(item => item.status === 1).length;
});

const maintenanceRoomsCount = computed(() => {
  return tableData.value.filter(item => item.status === 0).length;
});
const formData = reactive({
  roomId: 0,
  categoryName: '',
  roomName: '',
  roomNumber: '',
  description: '',
  image: '',
  capacity: 1,
  location: '',
  status: 0 ,
  openTime: '',
  closeTime: ''
});
// 图片上传相关
const fileList = ref([]);
const previewImage = ref('');
const previewVisible = ref(false);
const uploadFile = ref(null);
// 表单验证规则
const formRules = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请选择会议室类型', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
  roomNumber: [{ required: true, message: '请输入门牌号', trigger: 'blur' }],
  description: [{ required: true, message: '请输入会议室描述', trigger: 'blur' }],
  capacity: [
    { required: true, message: '请输入可容纳人数', trigger: 'blur' },
    { type: 'number', min: 1, message: '人数必须大于0', trigger: 'blur' }
  ],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'blur' }],
  openTime: [{ required: true, message: '请选择开放时间', trigger: 'blur' }],
  closeTime: [
    { required: true, message: '请选择关闭时间', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (formData.openTime && value && new Date(`2000-01-01 ${value}`) <= new Date(`2000-01-01 ${formData.openTime}`)) {
          callback(new Error('关闭时间必须晚于开放时间'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};
// 页面加载时获取会议室列表
onMounted(() => {
  fetchMeetingRooms();
});
/**
 * 获取会议室列表：调用接口获取数据
 */
const fetchMeetingRooms = async () => {
  try {
    // 调用接口获取会议室列表，传递分页和搜索参数
    const res = await getMeetingRoomList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      type: searchForm.type,
      name: searchForm.name
    });
    console.log("aaaaa",res)

    // 更新表格数据和分页信息
    tableData.value = res.data.data || [];
    pagination.total = res.data.total || 0;

  } catch (error) {
    console.error('获取会议室列表失败：', error);
    ElMessage.error('获取会议室列表失败，请重试');
    // 接口失败时使用空数组
    tableData.value = [];
    pagination.total = 0;
  }
};
// 格式化时间显示
const formatDate = (timeString) => {
  if (!timeString) return '';
  return timeString;
};
/**
 * 自定义图片上传
 */
const handleCustomUpload = (params) => {
  const file = params.file;
  uploadFile.value = file;
  const reader = new FileReader();
  reader.onload = (e) => {
    formData.image = e.target?.result;
    params.onSuccess({ status: 'success' }, file);
  };
  reader.readAsDataURL(file);
};
// 上传成功处理
const handleUploadSuccess = (response, file) => {
  fileList.value = [{
    name: file.name,
    url: formData.image,
    uid: file.uid
  }];
  ElMessage.success('图片预览成功，提交时同步上传');
};
// 表格图片预览
const handleImagePreview = (url) => {
  previewImage.value = url;
  previewVisible.value = true;
};
// 上传区域图片预览
const handleUploadPreview = (file) => {
  previewImage.value = file.url;
  previewVisible.value = true;
};
// 图片预览关闭
const handlePreviewClose = () => {
  previewVisible.value = false;
};
// 移除图片
const handleRemove = (file) => {
  formData.image = '';
  uploadFile.value = null;
  const index = fileList.value.findIndex(item => item.uid === file.uid);
  if (index !== -1) {
    fileList.value.splice(index, 1);
  }
};
// 修复：查询功能（携带搜索条件重新获取数据）
const handleQuery = () => {
  pagination.currentPage = 1; // 重置为第一页
  fetchMeetingRooms(); // 重新获取筛选后的分页数据
  ElMessage.success('查询完成');
};
// 修复：分页-每页条数变化（重新获取对应条数的第一页数据）
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  fetchMeetingRooms();
};
// 分页-页码变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  fetchMeetingRooms();
};



// 卡片点击事件
const handleCardClick = (row) => {
  // 可以在这里添加点击卡片的额外逻辑，比如查看详情等
  console.log('点击了卡片:', row);
};
// 新增
const handleAdd = () => {
  isEdit.value = false;
  formData.roomId = 0;
  formData.categoryName = '';
  formData.roomName = '';
  formData.roomNumber = '';
  formData.description = '';
  formData.image = '';
  formData.capacity = 1;
  formData.location = '';
  formData.status = 1;
  formData.openTime = '';
  formData.closeTime = '';
  fileList.value = [];
  uploadFile.value = null;
  dialogVisible.value = true;
};
// 编辑
const handleEdit = (row) => {
  isEdit.value = true;
  formData.roomId = row.roomId;
  formData.categoryName = row.categoryName;
  formData.roomName = row.roomName;
  formData.roomNumber = row.roomNumber;
  formData.description = row.description;
  formData.image = row.image;
  formData.capacity = row.capacity;
  formData.location = row.location;
  formData.status = row.status;
  formData.openTime = row.openTime;
  formData.closeTime = row.closeTime;
  // 初始化文件列表
  fileList.value = formData.image ? [{
    name: '会议室图片',
    url: formData.image,
    uid: Date.now()
  }] : [];
  uploadFile.value = null;
  dialogVisible.value = true;
};
/**
 * 单个删除：修复接口返回值判断，确保参数类型正确
 */
const handleDelete = async (roomId) => {
  ElMessageBox.confirm(
    '确认删除该会议室吗？',
    '删除确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
      center: true
    }
  ).then(async () => {
    try {
      const res = await deleteMeetingRoom(roomId);
      if (res.data.code == 1) {
        tableData.value = tableData.value.filter(item => item.roomId !== roomId);
        pagination.total = tableData.value.length;
        ElMessage.success('删除成功');
      } else {
        ElMessage.error(res.data.message || '删除失败');
      }
    } catch (error) {
      ElMessage.error('删除失败');
      console.error(error);
    }
  }).catch(() => {});
};

/**
 * 提交表单：修复新增/编辑逻辑，确保图片持久化，参数完整
 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch (error) {
    ElMessage.warning('表单验证失败，请检查必填项');
    return;
  }
  try {
    // 组装完整参数，确保接口所需字段不缺失
    const submitData = {
      roomId: formData.roomId,
      type: formData.categoryName,
      roomName: formData.roomName,
      roomNumber: Number(formData.roomNumber),
      description: formData.description,
      capacity: formData.capacity,
      location: formData.location,
      status: formData.status,
      openTime: formData.openTime,
      closeTime: formData.closeTime,
    };
    if (isEdit.value) {
      // 编辑操作
      const res = await updateMeetingRoom(submitData);
      if (res.data.code === 1) {
        // 图片上传：确保编辑时图片同步到后端
        if (uploadFile.value && formData.roomId) {
          await uploadMeetingRoomImage({
            roomId: formData.roomId,
            file: uploadFile.value
          });
        }
        // 更新本地数据，避免刷新丢失
        const index = tableData.value.findIndex(item => item.roomId === formData.roomId);
        if (index !== -1) {
          tableData.value[index] = { ...formData };
        }
        ElMessage.success('编辑成功');
      } else {
        ElMessage.error(res.data.message || '编辑失败');
      }
    } else {
      const fd = new FormData();
      fd.append('meetingroom', new Blob([JSON.stringify(submitData)], { type: 'application/json' }));
      if (uploadFile.value) {
        fd.append('image', uploadFile.value);
      }
      const res = await addMeetingRoom(fd);
      if (res.data.code === 1 && res.data.message === 'success') {
        ElMessage.success('新增会议室成功');
        fetchMeetingRooms();
        dialogVisible.value = false;
        fileList.value = [];
        uploadFile.value = null;
      } else {
        ElMessage.error(res.data.message || '新增会议室失败');
      }
    }
    dialogVisible.value = false;
  } catch (error) {
    ElMessage.error(isEdit.value ? '编辑失败' : '新增失败');
    console.error(error);
  }
};
// 弹窗关闭回调
const handleDialogClose = (done) => {
  formRef.value?.resetFields();
  done();
};
</script>
<style scoped>
/* 页面内边距 */
.conference-room-page {
  padding: 10px;
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

/* 统一操作盒子样式 */
.action-box {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.action-box:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
}

/* 统一操作区域 */
.unified-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px;
}

/* 查询区域 */
.search-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 操作按钮区域 */
.action-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 分页控制区域 */
.pagination-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
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

/* 操作按钮区域 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 0 20px 20px;
}

/* 按钮样式 */
.add-btn, .delete-btn {
  height: 54px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  letter-spacing: 0.5px;
}

.add-btn {
  background: linear-gradient(135deg,
    #67c23a 0%,
    #85ce61 50%,
    #409eff 100%);
  color: #fff;
}

.delete-btn {
  background: linear-gradient(135deg,
    #f56c6c 0%,
    #f78989 50%,
    #ef4444 100%);
  color: #fff;
}

.add-btn:hover {
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

.delete-btn:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow:
    0 12px 28px rgba(245, 108, 108, 0.45),
    0 4px 16px rgba(245, 108, 108, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg,
    #f78989 0%,
    #f56c6c 50%,
    #ef4444 100%);
}

.add-btn:active,
.delete-btn:active {
  transform: translateY(-2px) scale(1.02);
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.add-btn:hover .el-icon,
.delete-btn:hover .el-icon {
  transform: rotate(5deg) scale(1.2);
  transition: all 0.3s ease;
}

.add-btn::before,
.delete-btn::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%
  );
  transform: rotate(45deg);
  transition: all 0.6s ease;
}

.add-btn:hover::before,
.delete-btn:hover::before {
  animation: shimmer 1.2s ease-in-out;
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
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;

  border-radius: 12px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
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

.page-nav-buttons .el-button:hover .el-icon {
  transform: rotate(5deg) scale(1.2);
  transition: all 0.3s ease;
}

.page-nav-buttons .el-button::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%
  );
  transform: rotate(45deg);
  transition: all 0.6s ease;
}

.page-nav-buttons .el-button:hover::before {
  animation: shimmer 1.2s ease-in-out;
}

.page-nav-buttons .el-button:disabled {
  background: rgba(204, 204, 204, 0.8);
  border: none;
  box-shadow: none;
  transform: none;
  cursor: not-allowed;
  text-shadow: none;
}

.page-nav-buttons .el-button:disabled:hover::before {
  animation: none;
}

.page-nav-buttons .el-button:disabled:hover .el-icon {
  transform: none;
}

/* 查询按钮样式 */
.query-btn {
  height: 48px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 15px;
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
}

.query-btn:hover {
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

.query-btn:active {
  transform: translateY(-2px) scale(1.02);
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.query-btn:hover .el-icon {
  transform: rotate(5deg) scale(1.2);
  transition: all 0.3s ease;
}

.query-btn::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%
  );
  transform: rotate(45deg);
  transition: all 0.6s ease;
}

.query-btn:hover::before {
  animation: shimmer 1.2s ease-in-out;
}

/* 精美的卡片容器 - 纵向分布 */
.card-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

/* 卡片基础样式 - 纵向分布，宽度宽，高度适中 */
.room-card {
  width: 100%;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
  transition: all 0.3s ease;
  min-height: 200px;
}

.room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

/* 卡片内容 */
.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 24px;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(64, 158, 255, 0.1) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

/* 会议室类型 */
.room-type {
  font-size: 15px;
  font-weight: 600;
  color: #67c23a;
  background: rgba(103, 194, 58, 0.15);
  padding: 6px 16px;
  border-radius: 16px;
  letter-spacing: 0.5px;
}

/* 卡片主内容区域 - 横向分布 */
.room-main-content {
  display: flex;
  gap: 24px;
  padding: 24px;
  align-items: center;
}

/* 会议室图片样式 - 宽度固定，高度适中 */
.room-image {
  width: 200px;
  height: 150px;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 12px;
  position: relative;
  flex-shrink: 0;
}

.room-image :deep(.el-image) {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease;
  cursor: pointer;
  border-radius: 12px;
}

.room-image :deep(.el-image:hover) {
  transform: scale(1.05);
}

/* 无图片样式 */
.no-image {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 100%);
  border-radius: 12px;
}

.no-image-icon {
  font-size: 36px;
  margin-bottom: 8px;
  color: #c0c4cc;
  transition: all 0.3s ease;
}

.no-image:hover .no-image-icon {
  transform: scale(1.1);
  color: #909399;
}

/* 主要信息区域 */
.room-main-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 会议室名称 */
.room-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 0.5px;
  line-height: 1.3;
}

/* 基础信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 15px;
}

/* 基础信息区域 */
.basic-info {
  width: 100%;
}

/* 设备和描述信息 */
.equipment-info {
  margin-bottom: 15px;
  width: 100%;
}

/* 信息项 */
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  background: rgba(240, 244, 255, 0.6);
  border-radius: 10px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.info-item:hover {
  background: rgba(240, 244, 255, 0.9);
  transform: translateX(3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 全宽信息项 */
.info-item.full-width {
  grid-column: span 2;
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  width: 100%;
}

/* 信息图标 */
.info-icon {
  font-size: 16px;
  color: #67c23a;
  margin-right: 6px;
  flex-shrink: 0;
}

/* 信息标签 */
.info-label {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
  min-width: 70px;
  flex-shrink: 0;
}

/* 信息值 */
.info-value {
  color: #606266;
  font-size: 13px;
  flex: 1;
  word-break: break-all;
  line-height: 1.4;
}

/* 设备列表 */
.equipment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

/* 描述文本 */
.description-text {
  margin: 4px 0 0 0;
  line-height: 1.5;
  color: #606266;
}

/* 卡片操作按钮 */
.card-actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  padding: 0;
  margin-top: auto;
}

/* 响应式设计 */
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

  /* 移动端主内容布局 */
  .main-content-wrapper {
    flex-direction: column !important;
    gap: 8px !important;
    padding: 0 !important;
  }

  /* 移动端查询区域 */
  .search-section {
    gap: 6px !important;
  }

  /* 移动端操作按钮区域 */
  .action-section {
    gap: 6px !important;
  }

  /* 移动端分页控制区域 */
  .pagination-section {
    gap: 6px !important;
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

  /* 移动端按钮 */
  .add-btn, .delete-btn {
    height: 36px !important;
    font-size: 13px !important;
    padding: 0 12px !important;
  }

  /* 移动端查询按钮 */
  .query-btn {
    height: 36px !important;
    font-size: 13px !important;
    padding: 0 12px !important;
  }

  /* 移动端分页按钮 */
  .page-nav-buttons .el-button {
    height: 32px !important;
    font-size: 12px !important;
    padding: 0 10px !important;
  }

  /* 移动端卡片主内容区域 */
  .room-main-content {
    flex-direction: column !important;
    gap: 8px !important;
    padding: 10px !important;
    align-items: flex-start !important;
  }

  /* 移动端主要信息区域 */
  .room-main-info {
    gap: 8px !important;
  }

  /* 移动端基础信息区域 */
  .basic-info {
    margin-bottom: 0 !important;
  }

  /* 移动端设备和描述信息 */
  .equipment-info {
    margin-bottom: 0 !important;
  }

  /* 移动端会议室图片 */
  .room-image {
    width: 100% !important;
    height: auto !important;
    aspect-ratio: 16/9 !important;
  }

  /* 移动端基础信息网格 */
  .info-grid {
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 8px !important;
  }

  /* 移动端全宽信息项 */
  .info-item.full-width {
    grid-column: span 1 !important;
    white-space: normal !important;
    overflow: visible !important;
    text-overflow: clip !important;
  }

  /* 移动端设备信息区域 */
  .equipment-info {
    display: grid !important;
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 8px !important;
    width: 100% !important;
  }

  /* 移动端卡片头部 */
  .card-header {
    padding: 12px 15px !important;
  }

  /* 移动端会议室名称 */
  .room-name {
    font-size: 18px !important;
  }

  /* 移动端信息项 */
  .info-item {
    padding: 6px 10px !important;
    font-size: 12px !important;
  }

  /* 移动端信息标签 */
  .info-label {
    font-size: 11px !important;
    min-width: 60px !important;
    white-space: nowrap !important;
  }

  /* 移动端信息值 */
  .info-value {
    font-size: 11px !important;
    white-space: normal !important;
    overflow: visible !important;
    text-overflow: clip !important;
    word-break: break-all !important;
  }

  /* 移动端操作按钮 */
  .card-actions {
    flex-direction: row !important;
    justify-content: space-between !important;
    gap: 8px !important;
    width: 100% !important;
    margin-top: 10px !important;
  }

  /* 移动端操作按钮 */
  .card-actions .el-button {
    width: calc(50% - 4px) !important;
  }

  /* 移动端搜索表单 */
  .search-form {
    padding: 0 15px 15px !important;
  }

  /* 移动端操作按钮区域 */
  .action-buttons {
    padding: 0 15px 15px !important;
  }

  /* 移动端分页控件 */
  .pagination-controls {
    padding: 0 15px 15px !important;
  }

  /* 移动端会议类型 */
  .room-type {
    font-size: 13px !important;
    padding: 4px 12px !important;
  }

  /* 移动端页面内边距 */
  .conference-room-page {
    padding: 5px !important;
  }
}

/* 按钮样式 - 精美设计 */
.edit-btn,
.del-btn {
  border-radius: 8px;
  padding: 14px 28px;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 110px;
  justify-content: center;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  letter-spacing: 0.5px;
}

/* 按钮渐变和光泽效果 */
.edit-btn {
  background: linear-gradient(135deg,
    #67c23a 0%,
    #85ce61 50%,
    #409eff 100%);
  color: #fff;
}

.del-btn {
  background: linear-gradient(135deg,
    #f56c6c 0%,
    #f78989 50%,
    #ef4444 100%);
  color: #fff;
}

/* 按钮悬停效果 */
.edit-btn:hover {
  transform: translateY(-4px) scale(1.08);
  box-shadow:
    0 12px 28px rgba(103, 194, 58, 0.45),
    0 4px 16px rgba(103, 194, 58, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg,
    #85ce61 0%,
    #67c23a 50%,
    #667eea 100%);
}

.del-btn:hover {
  transform: translateY(-4px) scale(1.08);
  box-shadow:
    0 12px 28px rgba(245, 108, 108, 0.45),
    0 4px 16px rgba(245, 108, 108, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg,
    #f78989 0%,
    #f56c6c 50%,
    #ef4444 100%);
}

/* 按钮点击效果 */
.edit-btn:active,
.del-btn:active {
  transform: translateY(-2px) scale(1.04);
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* 按钮图标动画 */
.edit-btn:hover .el-icon,
.del-btn:hover .el-icon {
  transform: rotate(5deg) scale(1.2);
  transition: all 0.3s ease;
}

/* 按钮高光效果 */
.edit-btn::before,
.del-btn::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%
  );
  transform: rotate(45deg);
  transition: all 0.6s ease;
}

.edit-btn:hover::before,
.del-btn:hover::before {
  animation: shimmer 1.2s ease-in-out;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%) translateY(-100%) rotate(45deg);
  }
  100% {
    transform: translateX(100%) translateY(100%) rotate(45deg);
  }
}

/* 分页容器样式 */
.pagination-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-top: 20px;
}

/* 自定义分页样式 */
.custom-pagination {
  text-align: center;
}

.custom-pagination .el-pagination__sizes {
  margin-right: 15px;
}

.custom-pagination .el-pagination__total {
  margin-right: 15px;
  color: #666;
  font-weight: 500;
}

.custom-pagination .el-pager li {
  margin: 0 5px;
  border-radius: 8px;
  transition: all 0.2s ease;
  min-width: 32px;
  height: 32px;
  line-height: 32px;
}

.custom-pagination .el-pager li:hover {
  color: #67c23a;
  border-color: #67c23a;
}

.custom-pagination .el-pager li.active {
  background-color: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

/* 自定义对话框样式 */
.custom-dialog {
  animation: dialogFadeIn 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
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

/* 对话框底部按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 图片上传组件样式 */
.upload-demo {
  margin-top: 10px;
}

.el-upload--picture-card {
  width: 120px;
  height: 120px;
  transition: all 0.3s ease;
  border-radius: 12px;
}

.el-upload--picture-card:hover {
  transform: scale(1.03);
}

/* 图片查看器样式 */
:deep(.image-viewer .el-image-viewer__close) {
  width: 40px;
  height: 40px;
  line-height: 40px;
  font-size: 20px;
  transition: all 0.3s ease;
  border-radius: 50%;
}

:deep(.image-viewer .el-image-viewer__close:hover) {
  background-color: rgba(0, 0, 0, 0.5);
  transform: scale(1.1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content-wrapper {
    flex-direction: column;
  }

  .right-sidebar {
    width: 100%;
    min-width: auto;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }

  .search-card, .action-card, .stats-card {
    width: calc(33.333% - 14px);
    min-width: 250px;
  }

  .card-container {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .conference-room-page {
    padding: 10px;
  }

  .main-content-wrapper {
    gap: 15px;
  }

  .right-sidebar {
    gap: 15px;
  }

  .search-card, .action-card, .stats-card {
    width: 100%;
    min-width: auto;
  }

  .room-main-content {
    flex-direction: column;
    gap: 15px;
    padding: 15px;
  }

  .room-image {
    width: 100%;
    height: 200px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.full-width {
    grid-column: span 1;
  }

  .card-actions {
    justify-content: center;
  }

  .custom-pagination {
    text-align: center;
  }

  .custom-pagination .el-pagination__sizes,
  .custom-pagination .el-pagination__total {
    margin-right: 10px;
  }
}

@media (max-width: 480px) {
  .conference-room-page {
    padding: 8px;
  }

  .room-name {
    font-size: 18px;
  }

  .room-image {
    height: 180px;
  }

  .info-item {
    padding: 8px 12px;
    font-size: 13px;
  }

  .edit-btn,
  .del-btn {
    padding: 6px 12px;
    font-size: 12px;
  }

  .add-btn, .delete-btn {
    height: 45px;
    font-size: 14px;
  }
}
</style>
