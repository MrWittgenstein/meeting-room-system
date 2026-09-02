<template>
  <div class="app-wrapper">
    <!-- 头部组件 - 固定定位 -->
    <layoutHead />

    <!-- 主体布局容器 - 填充剩余空间 -->
    <div class="layout-container">
      <!-- 导航栏组件 -->
      <layoutNav />
      <!-- 二级路由出口 -->
      <router-view class="router-view-container" />
    </div>
  </div>
</template>

<script setup>
import layoutHead from './components/layoutHead.vue';
import layoutNav from './components/layoutNav.vue';

// 文件类型验证函数（供子组件使用）
const validateFileType = (file) => {
  const allowedTypes = [
    'image/jpeg', 'image/png', 'image/gif',
    'application/pdf',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ];
  if (!allowedTypes.includes(file.type)) {
    return {
      valid: false,
      message: `不支持${file.type}类型，支持jpg/png/gif/pdf/word/excel`
    };
  }
  const maxSize = 5 * 1024 * 1024; // 5MB
  if (file.size > maxSize) {
    return { valid: false, message: '文件大小超过5MB，请压缩后上传' };
  }
  return { valid: true };
};

defineExpose({ validateFileType });
</script>

<style scoped>
/* 全局布局容器 */
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  /* 移除overflow: hidden，允许页面滚动 */
}

/* 头部组件样式 - 确保正确显示 */
.layout-head {
  z-index: 1000 !important;
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100% !important;
  height: 60px !important;
}

/* 主体布局容器 - 填充头部下方的剩余空间 */
.layout-container {
  display: flex;
  flex: 1; /* 占满剩余高度 */
  margin-top: 60px; /* 与头部高度一致，避免重叠 */
  width: 100%;
  box-sizing: border-box;
  /* 移除overflow: hidden，允许子元素滚动 */
}

/* 路由内容容器 */
.router-view-container {
  flex: 1;
  padding: 20px;
  margin-left: 64px; /* 导航栏折叠宽度 */
  transition: margin-left 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: auto; /* 允许内容滚动 */
  box-sizing: border-box;
  height: calc(100vh - 80px); /* 计算可用高度，确保滚动正常 */
}

/* 导航栏展开时调整左边距 */
:deep(.nav-container:not(.nav-collapsed)) + .router-view-container {
  margin-left: 200px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .router-view-container {
    margin-left: 0 !important;
    padding: 10px;
    width: 100% !important;
  }

  :deep(.nav-container) {
    width: 0 !important;
    padding: 0 !important;
    transform: translateX(-100%);
  }

  :deep(.el-menu-vertical-demo) {
    width: 200px;
    position: fixed;
    left: 0;
    top: 60px;
    bottom: 0;
    z-index: 100;
    background: rgba(255, 255, 255, 0.95) !important;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
  }

  /* 添加移动端菜单遮罩 */
  :deep(.nav-container)::before {
    content: '';
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 99;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
  }

  /* 菜单展开时显示遮罩 */
  :deep(.nav-container.nav-expanded)::before {
    opacity: 1;
    visibility: visible;
  }

  /* 菜单展开时调整宽度 */
  :deep(.nav-container.nav-expanded) {
    width: 200px !important;
    transform: translateX(0);
  }

  /* 移动端菜单按钮样式 */
  .mobile-menu-toggle {
    display: block !important;
    position: fixed;
    left: 10px;
    top: 70px;
    z-index: 101;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 8px;
    padding: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    transition: all 0.3s ease;
  }

  /* 调整布局容器 */
  .layout-container {
    position: relative;
  }
}

/* 表格横向滚动适配 */
:deep(.table-responsive),
:deep(.el-table) {
  width: 100%;
  overflow-x: auto;
}

/* 确保表格容器有横向滚动 */
:deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

/* 弹窗响应式适配 */
:deep(.el-dialog) {
  max-width: 90vw !important;
}
</style>
