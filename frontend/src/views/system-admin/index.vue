<template>
  <div class="system-shell">
    <header class="system-header">
      <div class="brand"><el-icon><Grid /></el-icon><strong>智能会议室系统</strong><el-tag effect="plain">系统管理员</el-tag></div>
      <div class="account"><el-avatar :size="34"><User /></el-avatar><span>{{ identity.userName || '系统管理员' }}</span><el-button @click="logout">退出登录</el-button></div>
    </header>
    <div class="system-body">
      <aside>
        <p class="nav-caption">系统管理</p>
        <el-menu router :default-active="route.path">
          <el-menu-item index="/system-admin/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
          <el-menu-item index="/system-admin/logs"><el-icon><Document /></el-icon><span>日志查询</span></el-menu-item>
        </el-menu>
      </aside>
      <main><router-view /></main>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'SystemAdminLayout' })
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Grid, User, Document } from '@element-plus/icons-vue'
import http from '@/utils/http'
const route = useRoute()
const router = useRouter()
const identity = JSON.parse(localStorage.getItem('userInfo') || '{}')
async function logout() {
  try { await ElMessageBox.confirm('确定退出系统管理员账号？', '退出登录', { type: 'info' }) } catch { return }
  try {
    await http.post('/user/logout')
    for (const key of ['sessionId', 'token', 'userInfo']) localStorage.removeItem(key)
    await router.replace('/')
  } catch (error) { ElMessage.error(error.response?.data?.message || '退出失败，请重试') }
}
</script>

<style scoped>
.system-shell { min-height: 100vh; background: linear-gradient(135deg, #edf5fb, #f8f9fc); color: #303133; }
.system-header { min-height: 64px; padding: 12px 28px; display: flex; justify-content: space-between; align-items: center; background: white; box-shadow: 0 2px 14px #22405b0c; gap: 20px; }
.brand, .account { display: flex; align-items: center; gap: 14px; }
.brand > .el-icon { font-size: 26px; color: #69aede; }
.system-body { display: flex; min-height: calc(100vh - 88px); }
aside { width: 210px; flex-shrink: 0; background: #ffffffc9; padding-top: 24px; }
.nav-caption { margin: 0 24px 14px; color: #909399; font-size: 12px; }
.el-menu { border: 0; background: transparent; }
.el-menu-item { margin: 6px 12px; border-radius: 10px; }
.el-menu-item.is-active { background: #eaf4fc; color: #409eff; }
main { flex: 1; min-width: 0; padding: 28px; }
:deep(.page-heading) { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 22px; flex-wrap: wrap; }
:deep(h1) { margin: 0 0 6px; font-size: 24px; font-weight: 600; }
:deep(.muted) { color: #909399; font-size: 13px; margin: 0; }
:deep(.el-card) { border: 0; border-radius: 16px; box-shadow: 0 4px 24px #233b5410; }
:deep(.el-form-item__label) { color: #606266; }
:deep(.el-table th.el-table__cell) { background: #edf5fc; color: #49647d; }
:deep(.el-table__empty-text) { color: #909399; }
:deep(.toolbar) { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; align-items: center; }
:deep(.el-pagination) { margin-top: 20px; justify-content: flex-end; flex-wrap: wrap; }
@media (max-width: 720px) { .system-header { padding: 12px; flex-wrap: wrap; } .system-body { flex-direction: column; } aside { width: 100%; padding: 8px 0; } .nav-caption { display: none; } .el-menu { display: flex; } main { padding: 16px; } }
</style>
