<template>
    <div class="body">
        <div class="no0">
            <div class="head-container">
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
                <div class="head-right">
                    <div class="user-info" @click="tpinfo">
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
                    <!-- 消息通知内容区域（原n1空白区域替换为以下内容） -->
                    <div class="n1">
                        <div class="header">
                            <h2>消息通知</h2>
                            <el-radio-group v-model="mode" @change="loadList">
                                <el-radio-button label="priority">优先</el-radio-button>
                                <el-radio-button label="all">全部</el-radio-button>
                            </el-radio-group>
                        </div>
                        <div class="filters">
                            <el-select v-model="filters.newsType" placeholder="类型" clearable @change="applyFilters">
                                <el-option label="维修" value="维修" />
                                <el-option label="暂停使用" value="暂停使用" />
                                <el-option label="恢复使用" value="恢复使用" />
                                <el-option label="其他重要通知" value="其他重要通知" />
                            </el-select>

                            <el-input v-model="filters.keyword" placeholder="标题/内容关键字" clearable @input="applyFilters" />
                        </div>
                        <el-card class="list-card" v-loading="notiLoading">
                            <el-empty v-if="filtered.length === 0" description="暂无通知" />
                            <el-timeline v-else>
                                <el-timeline-item v-for="item in filtered" :key="item.id" :timestamp="formatRange(item.startTime, item.endTime)">
                                    <div class="item">
                                        <div class="title">
                                            <el-tag :type="typeTag(item.newsType)">{{ item.newsType }}</el-tag>

                                            <span class="text">{{ item.title }}</span>
                                        </div>
                                        <div class="content">{{ item.content }}</div>
                                        <div class="meta">
                                            <span>发布人：{{ item.publisherName || '系统' }}</span>
                                            <span v-if="item.relatedRoomId">关联会议室：{{ item.relatedRoomId }}</span>
                                            <el-tag v-if="item.status" :type="statusTag(item.status)" class="status" size="small">{{ item.status }}</el-tag>
                                        </div>
                                    </div>
                                </el-timeline-item>
                            </el-timeline>
                        </el-card>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 原有用户信息接口
import { getUserInfo } from '@/apis/userinfo'
// 新增消息通知接口
import { getImportantNewsAll, getImportantNewsValid, getImportantNewsPriority } from '@/apis/importantnews'

const router = useRouter()
const selectedIndex = ref("6")
// 原有加载状态（用户信息）
const loading = ref(false)

// 新增：消息通知相关响应式数据
defineOptions({ name: 'UserNotificationsPage' })
const notiLoading = ref(false) // 通知加载状态（避免与原有loading冲突）
const mode = ref('priority')
const list = ref([])
const filters = reactive({ newsType: '', severity: '', keyword: '' })

// 原有：用户信息数据
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
const img = ref("/lsj.jpg")
const name1 = ref("临时测试用户")
const originalInfo = ref({}) // 用于保存原始信息，取消编辑时恢复

// 原有：菜单选择处理
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

// 原有：获取用户信息
const fetchUserInfo = async () => {
    try {
        loading.value = true
        const response = await getUserInfo()
        console.log("111", response.data)
        if (response.data.message == 'success') {
            const userData = response.data.data
            // 保存原始信息用于取消编辑
            console.log("2222", userData)
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

// 原有：退出登录
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

const handleLogout = () => {
    router.push('/')
}

// 新增：消息通知时间格式化
const formatRange = (start, end) => {
    const s = start || ''
    const e = end || ''
    return e ? `${s} ~ ${e}` : s
}

// 新增：消息类型标签样式
const typeTag = (t) => {
    if (t === '维修') return 'danger'
    if (t === '暂停使用') return 'warning'
    if (t === '恢复使用') return 'success'
    if (t === '其他重要通知') return 'info'
    return 'primary'
}

// 新增：紧急程度标签样式
const severityTag = (s) => {
    if (s === '高') return 'danger'
    if (s === '中') return 'warning'
    if (s === '低') return 'info'
    return 'default'
}

// 新增：通知状态标签样式
const statusTag = (s) => {
    if (s === '未过期') return 'success'
    if (s === '已过期') return 'danger'
    return 'info'
}

// 新增：通知列表过滤（计算属性）
const filtered = computed(() => {
    const kw = filters.keyword.trim().toLowerCase()
    return list.value.filter(x => {
        if (filters.newsType && x.newsType !== filters.newsType) return false
        if (filters.severity && x.severity !== filters.severity) return false
        if (kw && !(`${x.title}${x.content}`.toLowerCase().includes(kw))) return false
        return true
    })
})
const tpinfo=()=>{
  router.push('./info')
}
// 新增：通知列表格式转换
const mapList = (arr) => {
    if (!Array.isArray(arr)) return []
    return arr.map(x => ({
        id: x.id,
        title: x.title,
        content: x.content,
        relatedRoomId: x.relatedRoomId,
        newsType: x.newsType,
        startTime: x.startTime,
        endTime: x.endTime,
        severity: x.severity,
        status: x.status,
        publisherId: x.publisherId,
        publisherName: x.publisherName,
        createTime: x.createTime
    }))
}

// 新增：加载通知列表
const loadList = async () => {
  notiLoading.value = true
  try {
    console.log("sadsadsadsadsadsa")
    let res;
    // 根据mode选择不同的API
    if (mode.value === 'valid') {
      res = await getImportantNewsValid()
    } else if (mode.value === 'priority') {
      res = await getImportantNewsPriority()
    } else {
      res = await getImportantNewsAll()
    }
    console.log("res0",res.data)


    list.value = mapList(res.data || [])

  } catch (error) {
    console.error('获取通知失败:', error)
    ElMessage.error('获取通知失败')
    list.value = []
  } finally {
    notiLoading.value = false
  }
}

// 新增：筛选触发（空函数，依赖computed自动响应）
const applyFilters = () => {}

// 合并：组件挂载时同时加载用户信息和通知列表
onMounted(() => {
    fetchUserInfo()
    loadList()
})
</script>

<style scoped>
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
  overflow: hidden;
  padding: 15px;
  gap: 15px;
  height: calc(100vh - 60px);
  max-height: calc(100vh - 60px);
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
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* 容器 */
.container {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 0;
}

/* 内容容器 - 3D效果 */
.n1 {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  width: 100%;
  min-height: 700px;
  position: relative;
  top: 30px;
  border-radius: 16px;
  box-shadow:
    0 10px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
  padding: 20px 50px;
  transform-style: preserve-3d;
}

/* 消息通知页面样式 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.3);
}

.header h2 {
  font-size: 24px;
  color: #666;
  font-weight: 700;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
  margin: 0;
}

.filters {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  padding: 15px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.list-card {
  min-height: 200px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3);
}

.item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px 15px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.2);
  background: rgba(255, 255, 255, 0.8);
  margin: 0 15px;
  border-radius: 12px;
}

.item:last-child {
  border-bottom: none;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-weight: 600;
}

.title .text {
  font-weight: 700;
  font-size: 16px;
  flex: 1;
  color: #666;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

.content {
  color: #606266;
  line-height: 1.6;
  padding: 10px 0;
  background: rgba(255, 255, 255, 0.6);
  padding: 12px;
  border-radius: 8px;
  border-left: 4px solid rgba(179, 219, 225, 0.3);
}

.meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
  padding: 10px 0;
  border-top: 1px dashed rgba(179, 219, 225, 0.2);
}

.status {
  margin-left: auto;
}

/* 时间轴样式 */
:deep(.el-timeline) {
  padding: 0 20px;
}

:deep(.el-timeline-item) {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.1);
}

:deep(.el-timeline-item:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

:deep(.el-timeline-item__tail) {
  background-color: rgba(179, 219, 225, 0.3);
}

:deep(.el-timeline-item__node) {
  background-color: rgba(179, 219, 225, 1);
  border-color: rgba(179, 219, 225, 1);
  box-shadow: 0 4px 12px rgba(179, 219, 225, 0.3);
}

/* 表单元素样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 按钮样式 */
:deep(.el-button) {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-left: 10px;
  &:hover {
    background: rgba(179, 219, 225, 0.8);
    border-color: rgba(179, 219, 225, 0.6);
    box-shadow: 0 6px 16px rgba(179, 219, 225, 0.3);
  }
  &:first-child {
    margin-left: 0;
  }
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
        width: 100% !important;
    }

    /* 移动端头部 */
    .header {
        flex-direction: row !important;
        justify-content: space-between !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 10px !important;
        margin: 0 !important;
        flex-wrap: nowrap !important;
    }

    /* 移动端头部标题 */
    .header h2 {
        font-size: 16px !important;
        margin-bottom: 0 !important;
        flex-shrink: 0 !important;
    }

    /* 移动端按钮组 */
    :deep(.el-radio-group) {
        width: auto !important;
        display: flex !important;
        flex-shrink: 1 !important;
        background: transparent !important;
    }

    /* 移动端按钮 */
    :deep(.el-radio-button) {
        flex: 1 !important;
        font-size: 12px !important;
        padding: 4px 12px !important;
        border-right: none !important;
        border-left: none !important;
        background: rgba(255, 255, 255, 0.8) !important;
    }

    /* 移动端第一个按钮 */
    :deep(.el-radio-button:first-child) {
        border-radius: 8px 0 0 8px !important;
        border-left: 1px solid rgba(200, 200, 200, 0.3) !important;
    }

    /* 移动端最后一个按钮 */
    :deep(.el-radio-button:last-child) {
        border-radius: 0 8px 8px 0 !important;
        border-right: 1px solid rgba(200, 200, 200, 0.3) !important;
    }

    /* 移动端按钮内部 */
    :deep(.el-radio-button__inner) {
        border: none !important;
        background: transparent !important;
        box-shadow: none !important;
    }

    /* 移动端按钮激活状态 */
    :deep(.el-radio-button.is-active .el-radio-button__inner) {
        background: rgba(179, 219, 225, 0.8) !important;
        color: #fff !important;
    }

    /* 移动端筛选栏 */
    .filters {
        flex-direction: column !important;
        gap: 8px !important;
        padding: 12px !important;
        margin: 0 4px 10px !important;
        border-radius: 12px !important;
        width: calc(100% - 8px) !important;
    }

    /* 移动端筛选组件 */
    .filters :deep(.el-select),
    .filters :deep(.el-input) {
        width: 100% !important;
    }

    /* 移动端列表卡片 */
    .list-card {
        padding: 8px !important;
        margin: 0 2px !important;
        border-radius: 10px !important;
        width: calc(100% - 4px) !important;
    }

    /* 移动端时间线项 */
    .item {
        padding: 10px !important;
    }

    /* 移动端标题 */
    .title {
        flex-direction: row !important;
        align-items: center !important;
        gap: 8px !important;
        flex-wrap: wrap !important;
    }

    /* 移动端标题文本 */
    .title .text {
        font-size: 14px !important;
        font-weight: 600 !important;
        flex: 1 !important;
    }

    /* 移动端时间轴时间戳 */
    :deep(.el-timeline-item__timestamp) {
        font-size: 11px !important;
        margin-bottom: 4px !important;
        color: #909399 !important;
    }

    /* 移动端内容 */
    .content {
        margin: 6px 0 !important;
        font-size: 12px !important;
        word-break: normal !important;
        overflow-wrap: break-word !important;
        white-space: normal !important;
        width: 100% !important;
        max-width: none !important;
        box-sizing: border-box !important;
        line-height: 1.4 !important;
        text-align: left !important;
        padding: 8px !important;
        border-radius: 6px !important;
    }

    /* 移动端时间轴容器 */
    :deep(.el-timeline) {
        padding: 0 !important;
        width: 100% !important;
    }

    /* 移动端时间轴项 */
    :deep(.el-timeline-item) {
        padding-left: 0 !important;
        width: 100% !important;
        margin-bottom: 8px !important;
    }

    /* 移动端时间轴内容 */
    .item {
        margin: 0 !important;
        padding: 10px !important;
        width: 100% !important;
        box-sizing: border-box !important;
        border-radius: 8px !important;
        gap: 6px !important;
    }

    /* 移动端时间轴节点 */
    :deep(.el-timeline-item__node) {
        left: 4px !important;
        top: 14px !important;
    }

    /* 移动端时间轴连线 */
    :deep(.el-timeline-item__tail) {
        left: 8px !important;
        width: 2px !important;
    }

    /* 移动端时间轴内容容器 */
    :deep(.el-timeline-item__content) {
        margin-left: 20px !important;
        width: calc(100% - 20px) !important;
        box-sizing: border-box !important;
        padding: 0 !important;
    }

    /* 移动端内容区域 */
    .n1 {
        padding: 6px !important;
        min-height: 600px !important;
        top: 4px !important;
        margin: 0 !important;
        width: 100% !important;
    }

    /* 移动端右侧内容区域 */
    .no2 {
        padding: 0 !important;
        width: 100% !important;
    }

    /* 移动端元信息 */
    .meta {
        flex-direction: row !important;
        flex-wrap: wrap !important;
        align-items: center !important;
        gap: 8px !important;
        font-size: 11px !important;
        padding: 6px 0 !important;
    }

    /* 移动端标签 */
    .title :deep(.el-tag) {
        margin-right: 6px !important;
        margin-bottom: 4px !important;
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

    /* 移动端按钮组 */
    :deep(.el-radio-group) {
        width: 100% !important;
        display: flex !important;
    }

    /* 移动端按钮 */
    :deep(.el-radio-button) {
        flex: 1 !important;
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
}




</style>
