<template>
  <div class="body">
    <!-- 个人信息卡片 -->
    <div class="profile-card">
      <div class="card-header with-actions">
        <div class="header-info">
          <h2>个人信息</h2>
          <p>管理您的个人资料</p>
        </div>
        <div class="header-actions">
          <el-button @click="toggleEditMode" type="primary" :loading="submitLoading">
            {{ isEditing ? '保存修改' : '编辑资料' }}
          </el-button>
          <el-button
            v-if="!isEditing"
            @click="openPasswordDialog"
            type="primary"
          >
            修改密码
          </el-button>
          <el-button
            v-if="isEditing"
            @click="cancelEdit"
          >
            取消
          </el-button>
        </div>
      </div>

      <div class="profile-content">
        <!-- 头像部分 -->
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            :action="null"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <div class="avatar-wrapper">
              <img
                :src="avatarUrl || defaultAvatar"
                alt="用户头像"
                class="user-avatar-lg"
              >
              <div class="avatar-upload-overlay">
                <el-icon class="upload-icon"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
          </el-upload>
          <p class="avatar-hint">点击头像更换</p>
        </div>

        <!-- 基本信息表单 -->
        <el-form label-width="120px" class="info-form">
          <div class="form-row">
            <el-form-item label="昵称">
              <el-input
                v-model="formData.username"
                :disabled="!isEditing"
                placeholder="请输入昵称"
                :maxlength="20"
              ></el-input>
            </el-form-item>

            <el-form-item label="ID">
              <el-input v-model="formData.userId" :disabled=true placeholder="用户ID"></el-input>
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="真实姓名">
              <el-input
                v-model="formData.realName"
                :disabled="!isEditing"
                placeholder="请输入真实姓名"
              ></el-input>
            </el-form-item>

            <el-form-item label="手机号码">
              <el-input
                v-model="formData.phone"
                :disabled="!isEditing"
                placeholder="请输入手机号码"
              ></el-input>
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="邮箱">
              <el-input
                v-model="formData.email"
                :disabled="true"
                placeholder="请输入邮箱"
                type="email"
              ></el-input>
            </el-form-item>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog
      title="修改密码"
      v-model="passwordDialogVisible"
      width="400px"
      :close-on-click-modal="false"
      class="password-dialog"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordFormData"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="电子邮箱" prop="email">
          <el-input
            v-model="passwordFormData.email"
            type="email"
            placeholder="请输入绑定的电子邮箱"
            :disabled="codeSent"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="verification-row">
            <el-input
              v-model="passwordFormData.code"
              placeholder="请输入验证码"
            />
            <el-button
              type="text"
              @click="sendCode"
              :disabled="codeSent || !passwordFormData.email"
              class="verification-btn"
            >
              {{ codeSent ? `${countDown}秒后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordFormData.newPassword"
            type="password"
            placeholder="请输入新密码（至少8位）"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordFormData.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePasswordChange" :loading="pwdLoading">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script lang="ts" setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Edit, Check, Close, Lock, User, Avatar, Document, Phone, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { ElForm } from 'element-plus'
// 导入封装的API
import { getUserInfo, updateUserInfo, changePassword, uploadAvatar, sendVerificationCode } from '@/apis/managerinfo.js'

// 类型定义
interface UserInfo {
  userId: string
  username: string
  realName: string
  phone: string
  email: string
  thumbnailUrl?: string // 头像地址（接口返回字段）
}
interface PasswordForm {
  email: string
  code: string
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// 默认头像（占位用）
const defaultAvatar = 'https://via.placeholder.com/180/409EFF/FFFFFF?text=头像';

// 用户信息状态
const userInfo = ref<UserInfo>({
  userId: '',
  username: '',
  realName: '',
  phone: '',
  email: ''
});
// 头像相关
const avatarUrl = ref<string>('');
const originalAvatar = ref<string>('');
// 表单数据
const formData = ref<UserInfo>({ ...userInfo.value });
const originalData = ref<UserInfo>({ ...userInfo.value });
// 状态控制
const isEditing = ref(false);
const submitLoading = ref(false);
const pwdLoading = ref(false);
// 密码弹窗控制
const passwordDialogVisible = ref(false);
const passwordFormData = ref<PasswordForm>({
  email: '',
  code: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});
// 验证码相关
const codeSent = ref(false);
const countDown = ref(60);
let countDownTimer: NodeJS.Timeout | null = null;

// 表单引用
const profileFormRef = ref<InstanceType<typeof ElForm>>();
const passwordFormRef = ref<InstanceType<typeof ElForm>>();

// 表单验证规则
const formRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的电子邮箱格式', trigger: 'blur' }
  ]
});

// 密码验证规则
const passwordRules = reactive({
  email: [
    { required: true, message: '请输入绑定的电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的电子邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== passwordFormData.value.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
});

// 初始化用户信息
const initUserInfo = async () => {
  try {
    const res = await getUserInfo();
    console.log("res1",res.data)
    if ( res.data.message=='success') {
      userInfo.value = res.data.data;
      console.log("12132333",userInfo.value)
      formData.value = { ...res.data.data };
      originalData.value = { ...res.data.data };
      // 赋值头像地址，确保有默认值
      avatarUrl.value = res.data.data.thumbnailUrl || defaultAvatar;
      originalAvatar.value = avatarUrl.value;
      // 初始化密码弹窗的邮箱
      passwordFormData.value.email = res.data.data.email;
    }
  } catch (error) {
    ElMessage.error('初始化用户信息失败');
    console.error('初始化失败:', error);
  }
};

// 头像上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error('请上传图片文件!');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 2MB!');
    return false;
  }
  return true;
};

// 处理头像上传
const handleAvatarUpload = async (params) => {
  const file = params.file;

  // 预览图片
  const reader = new FileReader();
  reader.onload = (event) => {
    avatarUrl.value = event.target?.result as string;
  };
  reader.readAsDataURL(file);

  // 上传到服务器
  const uploadFormData = new FormData();
  uploadFormData.append('file', file);

  try {
    const sendimg = await uploadAvatar(uploadFormData);
    console.log("sendimg", sendimg.data);
    if (sendimg.data.message == 'success') {
      ElMessage.success('头像上传成功');
      // 更新原始头像地址，确保保存时使用最新的URL
      originalAvatar.value = sendimg.data.data.originalFileUrl;
      // 更新用户信息中的头像地址
      userInfo.value.thumbnailUrl = sendimg.data.data.originalFileUrl;
      formData.value.thumbnailUrl = sendimg.data.data.originalFileUrl;
    } else {
      ElMessage.error(sendimg.data.message || '头像上传失败');
      avatarUrl.value = originalAvatar.value;
    }
  } catch (error) {
    ElMessage.error('头像上传失败，请重试');
    avatarUrl.value = originalAvatar.value;
    console.error('头像上传失败:', error);
  }
};

// 切换编辑模式
const toggleEditMode = async () => {
  if (isEditing.value) {
    // 保存修改
    const isValid = await profileFormRef.value?.validate();
    console.log("adsadsssss",isValid)

      submitLoading.value = true;
      try {
        // 接口要求只传username/realName/phone三个字段
        const updateData = {
          username: formData.value.username,
          realName: formData.value.realName,
          phone: formData.value.phone
        };

        const res = await updateUserInfo(updateData);
        console.log("asdsadas",res.data)
        if (res.data.code == 1) {
          userInfo.value = { ...formData.value };
          originalData.value = { ...formData.value };
          originalAvatar.value = avatarUrl.value;
          isEditing.value = false;
          ElMessage.success('个人信息修改成功');
        } else {
          ElMessage.error(res.data.message || '修改失败');
        }
      } catch (error) {
        ElMessage.error('修改信息失败');
        console.error('修改失败:', error);
      } finally {
        submitLoading.value = false;
      }
    
  } else {
    // 进入编辑模式
    originalAvatar.value = avatarUrl.value;
    isEditing.value = true;
    // 自动聚焦到第一个可编辑字段
    setTimeout(() => {
      const inputElement = document.querySelector('input:not([disabled])') as HTMLInputElement | null;
      inputElement?.focus();
    }, 300);
  }
};

// 取消编辑
const cancelEdit = () => {
  formData.value = { ...originalData.value };
  avatarUrl.value = originalAvatar.value;
  isEditing.value = false;
  profileFormRef.value?.clearValidate();
};

// 打开密码修改弹窗
const openPasswordDialog = () => {
  passwordDialogVisible.value = true;
  passwordFormData.value = {
    email: userInfo.value.email,
    code: '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  passwordFormRef.value?.clearValidate();
  // 重置验证码状态
  codeSent.value = false;
  countDown.value = 60;
  if (countDownTimer) clearInterval(countDownTimer);
};

// 发送验证码
const sendCode = async () => {
  try {
    const sendCode=await sendVerificationCode(passwordFormData.value.email);
    console.log("sendcode",sendCode.data)
    ElMessage.success('验证码发送成功，请注意查收');
    codeSent.value = true;
    // 倒计时逻辑
    countDownTimer = setInterval(() => {
      countDown.value--;
      if (countDown.value <= 0) {
        codeSent.value = false;
        countDown.value = 60;
        if (countDownTimer) clearInterval(countDownTimer);
      }
    }, 1000);
  } catch (error) {
    ElMessage.error('验证码发送失败');
    console.error('发送失败:', error);
  }
};

// 处理密码修改
const handlePasswordChange = async () => {
  const isValid = await passwordFormRef.value?.validate();
  if (isValid) {
    pwdLoading.value = true;
    try {
      const res = await changePassword({
        email: passwordFormData.value.email,
        code: passwordFormData.value.code,
        newPassword: passwordFormData.value.newPassword
      });
      if (res.data.code === 1) {
        passwordDialogVisible.value = false;
        ElMessage.success('密码修改成功，请重新登录');
        // 密码修改成功后可执行退出登录逻辑
        localStorage.removeItem('adminToken');
        setTimeout(() => {
          window.location.href = '/';
        }, 1500);
      } else {
        ElMessage.error(res.data.message || '密码修改失败');
      }
    } catch (error) {
      ElMessage.error('密码修改失败');
      console.error('修改失败:', error);
    } finally {
      pwdLoading.value = false;
    }
  }
};

// 初始化
onMounted(() => {
  initUserInfo();
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (countDownTimer) clearInterval(countDownTimer);
});
</script>
<style scoped>
/* 卡片通用样式 */
.profile-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(210, 230, 235, 0.6);
  border-radius: 20px;
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transition: all 0.3s ease;
  overflow: hidden;
  margin: 20px;

  &:hover {
    transform: translateY(-4px);
    box-shadow:
      0 20px 40px rgba(179, 219, 225, 0.2),
      0 0 0 1px rgba(255, 255, 255, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
  }
}

/* 卡片头部 */
.card-header {
  padding: 24px 30px;
  border-bottom: 1px solid rgba(210, 230, 235, 0.3);
  background: linear-gradient(135deg, rgba(179, 219, 225, 0.05), rgba(244, 162, 175, 0.05));

  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 700;
    color: #2d3748;
    background: linear-gradient(135deg, #4a5568, #2d3748);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #718096;
  }
}

/* 带有操作按钮的卡片头部 */
.card-header.with-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  flex: 1;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 个人信息卡片内容 */
.profile-content {
  padding: 30px;
}

/* 头像部分 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.avatar-wrapper {
  position: relative;
  width: 180px;
  height: 180px;
  margin-bottom: 12px;

  .user-avatar-lg {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid rgba(179, 219, 225, 0.3);
    box-shadow:
      0 12px 28px rgba(0, 0, 0, 0.15),
      0 0 0 1px rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
  }

  &:hover .user-avatar-lg {
    transform: scale(1.05);
    box-shadow:
      0 16px 32px rgba(179, 219, 225, 0.3),
      0 0 0 1px rgba(255, 255, 255, 0.5);
  }

  .avatar-upload-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: all 0.3s ease;
    color: #fff;
    cursor: pointer;

    .upload-icon {
      font-size: 32px;
      margin-bottom: 8px;
    }

    span {
      font-size: 14px;
      font-weight: 600;
    }
  }

  &:hover .avatar-upload-overlay {
    opacity: 1;
  }
}

.avatar-hint {
  margin: 0;
  font-size: 14px;
  color: #718096;
  font-style: italic;
}

/* 表单样式 */
.info-form, .password-form {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;

  .form-row {
    display: flex;
    gap: 24px;
    margin-bottom: 24px;

    :deep(.el-form-item) {
      flex: 1;
      margin-bottom: 0;
    }
  }

  .verification-row {
    display: flex;
    gap: 12px;

    :deep(.el-input) {
      flex: 1;
    }

    .verification-btn {
      flex-shrink: 0;
      padding: 0 20px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a5568;
  font-size: 14px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

/* 表单元素样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.8));
  border: 2px solid rgba(179, 219, 225, 0.3);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.2);
    border-color: rgba(179, 219, 225, 0.5);
  }

  &.is-focus {
    box-shadow: 0 8px 20px rgba(179, 219, 225, 0.3);
    border-color: rgba(179, 219, 225, 0.8);
  }
}

/* 表单操作按钮 - 现在已移到头部 */
.form-actions {
  display: none;
}

/* 按钮样式 */
:deep(.el-button) {
  background: linear-gradient(135deg, rgba(179, 219, 225, 0.9), rgba(159, 199, 205, 0.8)) !important;
  border: 2px solid rgba(179, 219, 225, 0.6) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  font-weight: 600 !important;
  padding: 10px 32px !important;
  font-size: 15px !important;
  color: #fff !important;
  transition: all 0.3s ease !important;
  cursor: pointer !important;

  &:hover {
    box-shadow: 0 8px 20px rgba(179, 219, 225, 0.5) !important;
    transform: translateY(-2px) !important;
    border-color: rgba(179, 219, 225, 0.8) !important;
    background: linear-gradient(135deg, rgba(179, 219, 225, 1), rgba(159, 199, 205, 0.9)) !important;
  }

  &:active {
    transform: translateY(0) !important;
    box-shadow: 0 2px 8px rgba(179, 219, 225, 0.3) !important;
  }

  &:disabled {
    background: linear-gradient(135deg, rgba(203, 213, 225, 0.7), rgba(196, 206, 216, 0.6)) !important;
    border-color: rgba(203, 213, 225, 0.5) !important;
    color: rgba(255, 255, 255, 0.8) !important;
    cursor: not-allowed !important;
    box-shadow: none !important;
  }
}

/* 确认按钮样式 */
:deep(.el-button--success) {
  background: linear-gradient(135deg, rgba(48, 209, 88, 0.9), rgba(46, 194, 85, 0.8)) !important;
  border-color: rgba(48, 209, 88, 0.6) !important;
  color: #fff !important;

  &:hover {
    background: linear-gradient(135deg, rgba(48, 209, 88, 1), rgba(46, 194, 85, 0.9)) !important;
    box-shadow: 0 8px 20px rgba(48, 209, 88, 0.5) !important;
    border-color: rgba(48, 209, 88, 0.8) !important;
  }
}

/* 密码弹窗 */
.password-dialog {
  --el-dialog-content-padding: 25px;
}

.password-dialog .el-dialog__header {
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
  margin-bottom: 10px;
}

.password-dialog .el-dialog__title {
  font-weight: 600;
  color: #1f2329;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-card {
    margin: 10px;
    border-radius: 16px;
  }

  .profile-content {
    padding: 16px;
  }

  /* 头像部分 - 移动端优化 */
  .avatar-section {
    margin-bottom: 20px;
  }

  .avatar-wrapper {
    width: 100px;
    height: 100px;
    margin-bottom: 8px;

    .user-avatar-lg {
      border-width: 3px;
      box-shadow:
        0 6px 16px rgba(0, 0, 0, 0.12),
        0 0 0 1px rgba(255, 255, 255, 0.5);
    }

    .avatar-upload-overlay {
      .upload-icon {
        font-size: 24px;
        margin-bottom: 4px;
      }

      span {
        font-size: 12px;
      }
    }
  }

  .avatar-hint {
    font-size: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .card-header h2 {
    font-size: 20px;
  }

  .card-header p {
    font-size: 12px;
  }

  .card-header.with-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    gap: 8px;
    flex-wrap: wrap;
  }

  /* 表单部分 - 移动端优化 */
  .info-form, .password-form {
    .form-row {
      flex-direction: column;
      gap: 10px;

      :deep(.el-form-item) {
        margin-bottom: 10px;
      }
    }
  }

  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    width: 100px;
  }

  /* 表单元素 - 移动端优化 */
  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  /* 按钮 - 移动端优化 */
  :deep(.el-button) {
    border-radius: 8px !important;
    padding: 8px 20px !important;
    font-size: 13px !important;
  }

  /* 密码弹窗 - 移动端优化 */
  .password-dialog {
    --el-dialog-content-padding: 20px;
    width: 90% !important;
    margin: 20px auto !important;
  }

  .password-dialog .el-dialog__header {
    padding-bottom: 12px;
    margin-bottom: 8px;
  }

  .password-dialog .el-dialog__title {
    font-size: 16px;
  }
}
</style>
