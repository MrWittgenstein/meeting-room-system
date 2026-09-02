<template>
  <div class="layout-head">
    <div class="head-container">
      <!-- 左侧 Logo 区域 -->
      <div class="head-left">
        <!-- 移动端菜单切换按钮 -->
        <div class="mobile-menu-toggle" @click="toggleMobileMenu">
          <el-icon class="menu-icon"><Menu /></el-icon>
        </div>
        <div class="logo">
          <el-icon class="logo-icon"><Grid /></el-icon>
          <span class="logo-text">智能会议室系统</span>
        </div>
      </div>

      <!-- 右侧功能区 -->
      <div class="head-right">
        <!-- 个人中心下拉菜单 -->
        <el-dropdown placement="bottom-end" :show-arrow="false">
          <div class="user-info" @click.stop>
            <el-avatar class="user-avatar" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
            <el-icon class="dropdown-arrow"><Grid /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item icon="User" @click="goToProfile">
                个人中心
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 退出登录按钮 -->
        <el-button
          size="small"
          type="text"
          icon="LogOut"
          class="logout-btn"
          @click="handleLogout"
        >
          退出登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getUserInfoAPI } from '@/apis/managerhome'
// 路由实例
const router = useRouter()

// 用户信息状态
const userName = ref('banyuan')
const userAvatar = ref('/avatar.jpg')

// 跳转到个人中心
const goToProfile = () => {
  router.push('/manager/personalInfo')
}

// 退出登录处理
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出智能会议室系统吗？',
    '退出确认',
    {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    // 清除登录状态
    localStorage.removeItem('meetingToken')
    localStorage.removeItem('userInfo')
    // 跳转到登录页
    router.push('/')
    ElMessage.success('已安全退出系统')
  })
}

const fetchAdminInfo = async () => {
  try {
    const res = await getUserInfoAPI();
    console.log("12332000",res.data.data)
    // 按实际接口返回字段调整（示例：若返回data.username则用res.data.username）
    userName.value = res.data.data.username || '刘昊洋';
    userAvatar.value=res.data.data.thumbnailUrl
  } catch (error) {
    userName.value = '刘昊洋'; // 默认名称兜底

  }
};

// 初始化 -
onMounted(() => {
  fetchAdminInfo()
})

// 移动端菜单切换
const toggleMobileMenu = () => {
  const navContainer = document.querySelector('.nav-container')
  if (navContainer) {
    navContainer.classList.toggle('nav-expanded')
  }
}


</script>

<style scoped lang="scss">

.layout-head {
  height: 60px;
  // 玻璃质感头部
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1000;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.1);
  transition: all 0.3s ease;
  transform-style: preserve-3d;
  perspective: 1000px;
  &:hover {
    box-shadow: 0 12px 40px rgba(31, 38, 135, 0.15);
    transform: translateY(-2px);
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

  // 左侧Logo区域
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

  // 右侧功能区
  .head-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  // 用户信息
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

    .dropdown-arrow {
      color: rgb(179, 219, 225);
      font-size: 16px;
      transition: all 0.3s ease;
      &:hover {
        color: rgba(159, 199, 205, 1);
      }
    }
  }

  // 退出登录按钮
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
}

// 旋转动画
@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 下拉菜单样式优化 - 玻璃质感
:deep(.el-dropdown-menu) {
  border-radius: 20px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 10px 0;

  .el-dropdown-item {
    padding: 10px 20px;
    font-size: 16px;
    font-weight: 500;
    color: #333;
    transition: all 0.3s ease;
    &:hover {
      background-color: rgba(179, 219, 225, 0.15);
      transform: translateX(5px);
      color: rgb(179, 219, 225);
    }
    &:focus {
      background-color: rgba(179, 219, 225, 0.2);
      color: rgb(179, 219, 225);
    }
  }
}

// 移动端菜单切换按钮样式
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

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  }

  .menu-icon {
    font-size: 20px;
    color: rgb(179, 219, 225);
  }
}

// 响应式适配
@media (max-width: 576px) {
  .logo-text {
    display: none;
  }

  .head-container {
    padding: 0 12px;
  }

  .logo {
    padding: 6px 12px !important;
    gap: 8px !important;
  }

  .logo-icon {
    font-size: 24px !important;
  }

  // 调整移动端用户信息布局
  .user-info {
    padding: 0 8px !important;
    gap: 4px !important;
  }

  .user-name {
    display: block !important;
    font-size: 11px !important;
    max-width: 60px !important;
  }

  .user-avatar {
    width: 26px !important;
    height: 26px !important;
  }

  .dropdown-arrow {
    font-size: 12px !important;
  }

  // 调整退出登录按钮
  .logout-btn {
    padding: 0 10px !important;
    font-size: 12px !important;
  }

  // 显示移动端菜单按钮
  .mobile-menu-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
