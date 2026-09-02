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
          <div class="user-info" @click.stop>
            <el-avatar class="user-avatar" :src="img" />
            <span class="user-name">{{ name1 }}</span>
          </div>

          <el-button class="logout-btn" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </div>
        <div class="main-container">
            <el-menu  :default-active="selectedIndex" @select="handleMenuSelect" :collapse="false" mode="vertical"
                     background-color="#f5f5f5" text-color="black" active-text-color="#87CEEB"
                     width="130px" class="no1">
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
            <div class="user-profile-container">
                <!-- 个人信息卡片 -->
                <div class="profile-card">
                    <div class="card-header with-actions">
                        <div class="header-info">
                            <h2>个人信息</h2>
                            <p>管理您的个人资料</p>
                        </div>
                        <div class="header-actions">
                            <el-button @click="handleEdit" type="primary" :disabled="!changeable">编辑信息</el-button>
                            <el-button @click="handleConfirm" type="success" v-show="isshow">保存修改</el-button>
                            <el-button @click="cancelEdit" v-show="isshow">取消</el-button>
                        </div>
                    </div>

                    <div class="profile-content">
                        <!-- 头像部分 -->
                        <div class="avatar-section">
                            <el-upload
                                class="avatar-uploader"
                                :action="null"
                                :show-file-list="false"
                                :before-upload="beforeAvatarUpload"
                                :http-request="handleAvatarUpload"
                                accept="image/*"
                            >
                                <div class="avatar-wrapper">
                                    <img :src="info.thumbnailUrl" alt="用户头像" class="user-avatar-lg">
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
                                    <el-input v-model="info.username" :disabled="changeable" placeholder="请输入昵称"></el-input>
                                </el-form-item>

                                <el-form-item label="ID">
                                    <el-input v-model="info.userId" :disabled=true placeholder="用户ID"></el-input>
                                </el-form-item>
                            </div>

                            <div class="form-row">
                                <el-form-item label="真实姓名">
                                    <el-input v-model="info.realName" :disabled="changeable" placeholder="请输入真实姓名"></el-input>
                                </el-form-item>

                                <el-form-item label="手机号码">
                                    <el-input v-model="info.phone" :disabled="changeable" placeholder="请输入手机号码"></el-input>
                                </el-form-item>
                            </div>

                            <div class="form-row">
                                <el-form-item label="邮箱">
                                    <el-input v-model="info.email" :disabled=true placeholder="请输入邮箱"></el-input>
                                </el-form-item>
                            </div>
                        </el-form>
                    </div>
                </div>

                <!-- 密码修改卡片 -->
                <div class="password-card">
                    <div class="card-header with-actions">
                        <div class="header-info">
                            <h2>安全设置</h2>
                            <p>修改您的登录密码</p>
                        </div>
                        <div class="header-actions">
                            <el-button @click="handleEditPassword" type="primary" :disabled="isshow1">修改密码</el-button>
                            <el-button @click="handleConfirmPassword" type="success" v-show="isshow1">确认修改</el-button>
                            <el-button @click="cancelEditPassword" v-show="isshow1">取消</el-button>
                        </div>
                    </div>

                    <el-form label-width="120px" class="password-form">
                        <el-form-item label="新密码" v-show="isshow1">
                            <el-input v-model="info.password" type="password" placeholder="请输入新密码"></el-input>
                        </el-form-item>

                        <el-form-item label="验证码" v-show="isshow1">
                            <div class="verification-row">
                                <el-input v-model="info.code1" placeholder="请输入验证码"></el-input>
                                <el-button
                                    @click="getVerificationCode"
                                    :disabled="countdown>0"
                                    class="verification-btn"
                                >
                                    {{ countdown > 0 ? `${countdown}s后重新获取` : '获取验证码' }}
                                </el-button>
                            </div>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </div>
    </div>
    </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import {
  getUserInfo,
  sendVerificationCode,
  updateUserInfo,
  changePassword,
  uploadAvatar,
} from '@/apis/userinfo'
import { registerEmailCodeAPI } from '@/apis/registerAPI'
const router = useRouter()
const selectedIndex = ref("4")
// 原有加载状态（用户信息）
const loading = ref(false)

// 用户信息数据
const info = reactive({
  thumbnailUrl: "/lsj.jpg",
  username: "",
  email: "",
  userId: "",
  phone: "",
  realName: "",
  password: "",
  code1: ""
})

// 状态控制
const isshow = ref(false)
const isshow1 = ref(false)
const changeable = ref(true)
const countdown = ref(0)
const img = ref("/lsj.jpg")
const name1 = ref("临时测试用户")
const originalInfo = ref({}) // 用于保存原始信息，取消编辑时恢复

// 菜单选择处理
  const handleMenuSelect = (index) => {
    console.log("选中菜单:", index)
    const routes = {
      "1": './home',
      "2": './appoint',
      "3": './check',
      "4": './info',
      "5": './meetingMinu',
      "6": './notifications',
      "7": './control'
    }
    if (routes[index]) {
      router.push(routes[index])
    }
  }

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    loading.value = true
    const response = await getUserInfo()
    console.log("111",response.data)
    if (response.data.message=='success') {
      const userData = response.data.data
      // 保存原始信息用于取消编辑
      console.log("2222",userData)
      originalInfo.value = { ...userData }

      // 更新用户信息
      info.thumbnailUrl = userData.thumbnailUrl || "/lsj.jpg"
      info.username = userData.username || ""
      info.email = userData.email || ""
      info.userId = userData.userId || ""
      info.phone = userData.phone || ""
      info.realName = userData.realName || ""

      // 更新顶部显示
      img.value = userData.thumbnailUrl || "/lsj.jpg"
      name1.value = userData.username || ""
    } else {
      ElMessage.error(response.data.message || "获取用户信息失败")
    }
  } catch (error) {
    console.error("获取用户信息失败:", error)
    ElMessage.error("获取用户信息失败，请重试")
  } finally {
    loading.value = false
  }
}

// 编辑用户信息
const handleEdit = () => {
  isshow.value = true
  changeable.value = false
  // 保存当前信息用于取消时恢复
  originalInfo.value = {
    username: info.username,
    realName: info.realName,
    phone: info.phone
  }
}

// 取消编辑
const cancelEdit = () => {
  isshow.value = false
  changeable.value = true
  // 恢复原始信息
  info.username = originalInfo.value.username
  info.realName = originalInfo.value.realName
  info.phone = originalInfo.value.phone
}

// 确认更新用户信息
// vue文件内的handleConfirm
const handleConfirm = async () => {
  try {
    loading.value = true
    const updateData = {
      username: info.username,
      realName: info.realName,
      phone: info.phone,
    }

    const response = await updateUserInfo(updateData)
    console.log("更新响应:", response)

    // 更严谨的响应判断（适配接口常见的code+message结构）
    if (response.data?.code == 1 && response.data?.message == 'success') {
      ElMessage.success("信息更新成功")
      isshow.value = false
      changeable.value = true
      fetchUserInfo() // 重新获取用户信息
    } else {
      ElMessage.error(response.data?.message || "信息更新失败")
    }
  } catch (error) {
    console.error("更新用户信息失败:", error)
    ElMessage.error("信息更新失败，请重试")
  } finally {
    loading.value = false
  }
}

// 编辑密码
const handleEditPassword = () => {
  isshow1.value = true
  info.password = ""
  info.code1 = ""
}

// 取消修改密码
const cancelEditPassword = () => {
  isshow1.value = false
  info.password = ""
  info.code1 = ""
}

// 获取验证码
const getVerificationCode = async () => {
  if (!info.email) {
    ElMessage.warning("请先完善邮箱信息")
    return
  }

  try {
    const response = await registerEmailCodeAPI(info.email)
    if (response.data.message=='success') {
      ElMessage.success("验证码发送成功，请查收邮件")
      // 开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error(response.data.message || "验证码发送失败")
    }
  } catch (error) {
    console.error("发送验证码失败:", error)
    ElMessage.error("验证码发送失败，请重试")
  }
}
const tpinfo=()=>{
  router.push('./info')
}
// 确认修改密码
const handleConfirmPassword = async () => {
  console.log(info.password)
  if (!info.password) {
    ElMessage.warning("请输入新密码")
    return
  }

  if (!info.code1) {
    ElMessage.warning("请输入验证码")
    return
  }

  try {
    loading.value = true
    const response = await changePassword({
      email: info.email,
      code: info.code1,
      newPassword: info.password
    })
    console.log(response)

    if (response.data.message=='success') {
      ElMessage.success("密码修改成功,请重新登入")
      isshow1.value = false
      info.password = ""
      info.code1 = ""
      router.push('/')
      // 密码修改成功后退出登录

    } else {
      ElMessage.error(response.data.message || "密码修改失败")
    }
  } catch (error) {
    console.error("修改密码失败:", error)
    ElMessage.error("密码修改失败，请重试")
  } finally {
    loading.value = false
  }
}

// 头像上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('请上传图片文件!')
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

// 处理头像上传
const handleAvatarUpload = async (params) => {
  try {
    const formData = new FormData()
    formData.append('file', params.file)

    const response = await uploadAvatar(formData)
    console.log("112323",response)
    if (response.data.message=='success') {
      ElMessage.success("头像上传成功")
      // 更新头像显示
      info.thumbnailUrl = response.data.data.originalFileUrl
      img.value = response.data.data.originalFileUrl
    } else {
      ElMessage.error(response.data.message || "头像上传失败")
    }
  } catch (error) {
    console.error("头像上传失败:", error)
    ElMessage.error("头像上传失败，请重试")
  }
}

// 退出登录
const handleLogout =  () => {
  router.push('/')
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

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo()
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
  padding: 10px;
}

/* 容器 */
.container{
  width: 100%;
  max-width: 1350px;
  margin: 0 auto;
  padding: 0;
}

/* 用户资料容器 */
.user-profile-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 10px 0;
}

/* 卡片通用样式 */
.profile-card, .password-card {
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

/* 移除表单操作按钮的容器样式，因为按钮已经移到头部 */
.form-actions {
  display: none;
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

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(210, 230, 235, 0.3);
  justify-content: flex-start;
  align-items: center;
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

/* 密码卡片的按钮容器 */
.password-form .form-actions {
  justify-content: flex-start;
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

  /* 移动端个人资料容器 */
  .user-profile-container {
      padding: 8px !important;
      gap: 8px !important;
  }

  /* 移动端卡片头部 */
  .card-header {
      flex-direction: column !important;
      align-items: flex-start !important;
      gap: 8px !important;
      padding: 12px !important;
  }

  /* 移动端头部信息 */
  .header-info {
      text-align: left !important;
  }

  /* 移动端头部操作 */
  .header-actions {
      width: 100% !important;
      flex-direction: row !important;
      flex-wrap: wrap !important;
      gap: 6px !important;
  }

  /* 移动端头部按钮 */
  .header-actions .el-button {
      flex: 1 !important;
      min-width: calc(50% - 3px) !important;
      padding: 6px 12px !important;
      height: 36px !important;
  }

  /* 移动端头像部分 */
  .avatar-section {
      align-items: center !important;
      margin-bottom: 16px !important;
  }

  /* 移动端头像 */
  .user-avatar-lg {
      width: 100px !important;
      height: 100px !important;
  }

  /* 移动端表单 */
  .info-form, .password-form {
      max-width: 90% !important;
      margin: 0 auto !important;
      padding: 0 !important;
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;

      .form-row {
          display: flex !important;
          flex-direction: column !important;
          gap: 12px !important;
          margin-bottom: 12px !important;
          width: 100% !important;

          :deep(.el-form-item) {
              width: 100% !important;
              margin-bottom: 0 !important;
              display: flex !important;
              flex-direction: column !important;
              align-items: center !important;

              :deep(.el-form-item__label) {
                  font-size: 12px !important;
                  padding-right: 0 !important;
                  width: 100% !important;
                  text-align: center !important;
                  margin-bottom: 4px !important;
              }

              :deep(.el-form-item__content) {
                  flex: 1 !important;
                  margin-left: 0 !important;
                  width: 100% !important;
                  display: flex !important;
                  justify-content: center !important;
              }

              :deep(.el-input) {
                  width: 200px !important;
                  max-width: 100% !important;
              }

              :deep(.el-input__wrapper) {
                  height: 36px !important;
                  min-height: 36px !important;
                  padding: 0 12px !important;
                  width: 100% !important;
              }

              :deep(.el-input__inner) {
                  height: 36px !important;
                  line-height: 36px !important;
                  font-size: 12px !important;
                  padding: 0 !important;
                  text-align: center !important;
              }
          }
      }
  }

  /* 移动端验证码行 */
  .verification-row {
      flex-direction: row !important;
      gap: 8px !important;
      width: 100% !important;
      max-width: 200px !important;
      margin: 0 auto !important;

      :deep(.el-input) {
          flex: 1 !important;
          width: auto !important;
      }

      .verification-btn {
          flex-shrink: 0 !important;
          padding: 0 12px !important;
          height: 36px !important;
      }
  }

  /* 移动端表单项 */
  :deep(.el-form-item) {
      margin-bottom: 8px !important;
  }

  /* 移动端表单标签 */
  :deep(.el-form-item__label) {
      font-size: 12px !important;
  }

  /* 移动端表单输入框 */
  :deep(.el-input__wrapper) {
      width: 100% !important;
  }

  /* 移动端密码修改表单 */
  .password-form {
      padding: 12px !important;
  }

  /* 移动端密码表单标题 */
  .form-section-title {
      margin-bottom: 12px !important;
      font-size: 14px !important;
  }

  /* 移动端密码表单行 */
  .password-form .form-row {
      margin-bottom: 12px !important;
  }

  /* 移动端头像容器 */
  .avatar-wrapper {
      width: 100px !important;
      height: 100px !important;
  }

  /* 移动端头像上传遮罩 */
  .avatar-upload-overlay .upload-icon {
      font-size: 20px !important;
  }

  /* 移动端头像上传文字 */
  .avatar-upload-overlay span {
      font-size: 10px !important;
  }

  /* 移动端卡片内容 */
  .profile-content {
      padding: 12px !important;
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

  /* 移动端卡片 */
  .profile-card, .password-card {
      margin-bottom: 8px !important;
      border-radius: 12px !important;
      padding: 8px !important;
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
