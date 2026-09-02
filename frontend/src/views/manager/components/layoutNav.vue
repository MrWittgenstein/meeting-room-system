<template>
  <div class="nav-container">
    <!-- 垂直菜单 -->
    <el-menu
      default-active="meetingRoomInfo"
      class="el-menu-vertical-demo"
      :collapse="false"
      @open="handleOpen"
      @close="handleClose"
      @select="handleMenuSelect"
      background-color="#f5f7fa"
      text-color="#333"
      active-text-color="rgb(179, 219, 225)"
      router
    >
      <!-- 首页 -->
      <el-menu-item index="/manager">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </el-menu-item>

      <!-- 会议室管理 -->
      <el-sub-menu index="meeting">
        <template #title>
          <el-icon><Grid /></el-icon>
          <span>会议室管理</span>
        </template>
        <el-menu-item-group>
          <el-menu-item index="/manager/meetingRoomInfo">会议室信息</el-menu-item>
          <el-menu-item index="/manager/meetingRoomRecords">会议室使用记录</el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>



      <!-- 审批管理 -->
      <el-sub-menu index="approval">
        <template #title>
          <el-icon><List /></el-icon>
          <span>审批管理</span>
        </template>
        <el-menu-item-group>
          <el-menu-item index="/manager/pendingApprovalList">待审批</el-menu-item>
          <el-menu-item index="/manager/ApprovedList">审批记录</el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>

      <!-- 消息通知管理 -->
      <el-sub-menu index="notification">
        <template #title>
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
        </template>
        <el-menu-item-group>
          <el-menu-item index="/manager/notificationManagement">通知管理</el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>

      <!-- 个人信息 -->
      <el-sub-menu index="personal">
        <template #title>
          <el-icon><Avatar /></el-icon>
          <span>个人信息</span>
        </template>
        <el-menu-item-group>
          <el-menu-item index="/manager/personalInfo">个人信息</el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

// 菜单展开事件
const handleOpen = (key: string, keyPath: string[]) => {

}

// 菜单关闭事件
const handleClose = (key: string, keyPath: string[]) => {

}

// 菜单选择事件 - 点击菜单项后自动关闭导航栏
const handleMenuSelect = (key: string, keyPath: string[]) => {
  // 检查是否在移动端
  if (window.innerWidth <= 768) {
    // 关闭导航栏
    const navContainer = document.querySelector('.nav-container');
    if (navContainer) {
      navContainer.classList.remove('nav-expanded');
    }
  }
}
</script>

<style scoped lang="scss">

/* 导航整体容器-固定定位 */
.nav-container {
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  width: 200px;
  padding: 15px;
  box-sizing: border-box;
  // 玻璃质感侧边栏
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 4px 0 20px rgba(31, 38, 135, 0.1);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 99;
  overflow: hidden;
  transform-style: preserve-3d;
  perspective: 1000px;
  &:hover {
    box-shadow: 6px 0 30px rgba(31, 38, 135, 0.15);
  }
}

/* 垂直菜单基础样式 */
.el-menu-vertical-demo {
  height: 100%;
  overflow-y: auto;
  border-right: none;
  background: transparent;
}

/* 移动设备适配 */
@media (max-width: 768px) {
  .nav-container {
    width: 100%;
    max-height: calc(100vh - 60px);
    overflow: hidden;
  }

  .el-menu-vertical-demo {
    height: 100%;
    max-height: calc(100vh - 60px);
    overflow-y: auto !important;
    -webkit-overflow-scrolling: touch !important;
    scroll-behavior: smooth !important;
  }
}

.el-menu-vertical-demo::-webkit-scrollbar {
  width: 6px;
}
.el-menu-vertical-demo::-webkit-scrollbar-thumb {
  background-color: rgba(179, 219, 225, 0.4);
  border-radius: 12px;
  &:hover {
    background-color: rgb(179, 219, 225);
  }
}
.el-menu-vertical-demo::-webkit-scrollbar-track {
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
}

/* 菜单项样式 */
.el-menu-item,
.el-sub-menu__title {
  height: 55px !important;
  line-height: 55px !important;
  margin-bottom: 8px !important;
  border-radius: 20px !important;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  padding: 0 20px !important;
  background: rgba(255, 255, 255, 0.8) !important;
  border: 2px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08) !important;
  color: #666 !important;
  font-weight: 600 !important;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

.el-menu-item:hover,
.el-sub-menu__title:hover {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.15), rgba(179, 219, 225, 0.05)) !important;
  border-color: rgba(179, 219, 225, 0.4) !important;
  color: rgb(179, 219, 225) !important;
  transform: translateX(5px) translateY(-3px) !important;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12) !important;
}

.el-menu-item.is-active {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.25), rgba(179, 219, 225, 0.15)) !important;
  border-color: rgb(179, 219, 225) !important;
  color: rgb(179, 219, 225) !important;
  font-weight: 700 !important;
  transform: translateX(5px) !important;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12) !important;
}

/* 子菜单样式 */
.el-sub-menu.is-opened > .el-sub-menu__title {
  background: linear-gradient(145deg, rgba(179, 219, 225, 0.2), rgba(179, 219, 225, 0.1)) !important;
  border-color: rgba(179, 219, 225, 0.4) !important;
  color: rgb(179, 219, 225) !important;
}

.el-menu-item-group {
  padding: 0 !important;
}

.el-menu-item-group .el-menu-item {
  padding-left: 45px !important;
  margin-bottom: 5px !important;
  height: 48px !important;
  line-height: 48px !important;
  background: rgba(255, 255, 255, 0.7) !important;
  border-left: 4px solid rgba(179, 219, 225, 0.3) !important;
}

/* 图标样式 */
:deep(.el-icon) {
  font-size: 22px;
  margin-right: 12px;
  transition: all 0.3s ease;
  color: rgb(179, 219, 225);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  &:hover {
    color: rgba(159, 199, 205, 1);
  }
}

.el-menu-item__title,
.el-sub-menu__title span {
  display: inline-block;
  transition: opacity 0.3s ease, transform 0.3s ease;
  font-size: 16px;
}

.el-menu-item-group__title {
  padding: 0 !important;
  height: 0 !important;
}

/* 菜单展开/折叠动画 */
:deep(.el-menu) {
  transition: all 0.3s ease;
}

:deep(.el-sub-menu .el-menu) {
  background: transparent !important;
  padding: 8px 0;
}

:deep(.el-sub-menu .el-menu-item) {
  margin-bottom: 4px !important;
}
</style>
