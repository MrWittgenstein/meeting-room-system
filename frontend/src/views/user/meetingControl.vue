<template>
  <div class="body">
    <div class="no0">
      <div class="head-container">
        <div class="head-left">
          <div class="mobile-menu-toggle" @click="toggleMenu">
            <el-icon class="menu-icon"><Menu /></el-icon>
          </div>
          <div class="logo">
            <el-icon class="logo-icon"><Grid /></el-icon>
            <span class="logo-text">智能会议室系统</span>
          </div>
        </div>

        <div class="head-right">
          <div class="user-info" @click.stop @click="tpinfo()">
            <el-avatar class="user-avatar" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
          </div>
          <el-button class="logout-btn" @click="handleExit">退出登录</el-button>
        </div>
      </div>
    </div>

    <div class="main-container">
      <el-menu
        :default-active="selectedIndex"
        @select="handleMenuSelect"
        :collapse="false"
        mode="vertical"
        background-color="#f5f5f5"
        text-color="black"
        active-text-color="#87CEEB"
        width="130px"
        class="no1"
      >
        <el-menu-item index="1"><el-icon><House /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="2"><el-icon><EditPen /></el-icon><span>预约会议</span></el-menu-item>
        <el-menu-item index="3"><el-icon><ChatLineSquare /></el-icon><span>查询预约</span></el-menu-item>
        <el-menu-item index="5"><el-icon><PieChart /></el-icon><span>会议记录</span></el-menu-item>
        <el-menu-item index="4"><el-icon><User /></el-icon><span>个人信息</span></el-menu-item>
        <el-menu-item index="6"><el-icon><Bell /></el-icon><span>消息通知</span></el-menu-item>
        <el-menu-item index="7"><el-icon><Setting /></el-icon><span>会议控制</span></el-menu-item>
      </el-menu>

      <div class="no2">
        <div class="control-layout">

          <!-- ===== 顶部大卡片 ===== -->
          <section class="hero-card">
            <div class="hero-left">
              <h1>{{ roomNameMap[currentRoom] }} · 3D展示与环境联动</h1>
              <p>实时监控会议室状态，智能联动空调与设备</p>
            </div>
            <div class="hero-stats">
              <div class="hero-stat">
                <span class="hero-stat-label">在线会议室</span>
                <strong class="hero-stat-val">3</strong>
                <small>全部可用</small>
              </div>
              <div class="hero-stat">
                <span class="hero-stat-label">联动设备</span>
                <strong class="hero-stat-val">{{ onlineDevices }}</strong>
                <small>空调 / 灯光 / 投影</small>
              </div>
              <div class="hero-stat">
                <span class="hero-stat-label">会议状态</span>
                <strong class="hero-stat-val" :class="{ danger: roomStatus !== '正常' }">{{ roomStatus }}</strong>
                <small>模拟运行中</small>
              </div>
            </div>
          </section>

          <!-- ===== 中间行：智能控制 + 3D ===== -->
          <div class="content-row">
            <div class="control-side-card">
              <div class="control-card-title">
                <el-icon><Setting /></el-icon>
                <span>智能控制</span>
              </div>

              <!-- 会议室切换 -->
              <div class="control-section">
                <div class="control-section-label">会议室</div>
                <div class="room-switch-options">
                  <button v-for="room in roomOptions" :key="room.value" class="room-option-btn" :class="{ active: currentRoom === room.value }" @click="selectRoom(room.value)">
                    <span class="room-option-icon">{{ room.icon }}</span>
                    <span class="room-option-name">{{ room.label }}</span>
                  </button>
                </div>
              </div>

              <!-- 设备控制 -->
              <div class="control-section">
                <div class="control-section-label">设备</div>
                <!-- 空调行 -->
                <div class="device-status-row ac-row" :class="{ expanded: showAcPanel }" @click="showAcPanel = !showAcPanel">
                  <div class="device-status-left">
                    <div class="device-icon airConditioner">AC</div>
                    <div>
                      <div class="device-name">空调</div>
                      <div class="device-note">
                        <span v-if="acPower">{{ acTemperature }}°C · {{ acModeLabel }} · {{ acFanLabel }}</span>
                        <span v-else>已关闭</span>
                      </div>
                    </div>
                  </div>
                  <div class="ac-row-actions">
                    <el-icon class="ac-expand-icon" :class="{ rotated: showAcPanel }"><ArrowDown /></el-icon>
                    <el-switch v-model="deviceState.airConditioner" active-color="#8fc7d1" @click.stop />
                  </div>
                </div>
                <!-- 展开的空调遥控器 -->
                <div class="ac-expand-panel" v-if="showAcPanel" @click.stop>
                  <div class="ac-inline-row">
                    <button class="ac-power-btn-sm" :class="{ on: acPower }" @click="acPower = !acPower">
                      <el-icon><SwitchButton /></el-icon><span>{{ acPower ? '运行中' : '已关机' }}</span>
                    </button>
                  </div>
                  <div class="ac-inline-row temp-row">
                    <button class="ac-circle-sm" :disabled="!acPower || acTemperature <= 16" @click="acTemperature--">−</button>
                    <input type="range" class="ac-slider-sm" min="16" max="30" v-model.number="acTemperature" :disabled="!acPower" />
                    <button class="ac-circle-sm" :disabled="!acPower || acTemperature >= 30" @click="acTemperature++">+</button>
                    <span class="ac-temp-val">{{ acTemperature }}°C</span>
                  </div>
                  <div class="ac-inline-row chip-row">
                    <button v-for="m in acModes" :key="m.value" class="ac-chip-sm" :class="{ sel: acMode === m.value }" :disabled="!acPower" @click="acMode = m.value">{{ m.icon }} {{ m.label }}</button>
                  </div>
                  <div class="ac-inline-row chip-row">
                    <button v-for="f in acFanSpeeds" :key="f.value" class="ac-chip-sm" :class="{ sel: acFanSpeed === f.value }" :disabled="!acPower" @click="acFanSpeed = f.value">{{ f.label }}</button>
                  </div>
                  <div class="ac-inline-row chip-row">
                    <button v-for="d in acSwingDirs" :key="d.value" class="ac-chip-sm" :class="{ sel: acSwingDir === d.value }" :disabled="!acPower" @click="acSwingDir = d.value">{{ d.icon }} {{ d.label }}</button>
                  </div>
                </div>

                <!-- 灯光 -->
                <div class="device-status-row">
                  <div class="device-status-left">
                    <div class="device-icon light">LT</div>
                    <div><div class="device-name">灯光</div><div class="device-note">支持分区照明与演示亮度</div></div>
                  </div>
                  <el-switch v-model="deviceState.light" active-color="#8fc7d1" />
                </div>
                <!-- 投影仪 -->
                <div class="device-status-row">
                  <div class="device-status-left">
                    <div class="device-icon projector">PJ</div>
                    <div><div class="device-name">投影仪</div><div class="device-note">演示与投屏设备</div></div>
                  </div>
                  <el-switch v-model="deviceState.projector" active-color="#8fc7d1" />
                </div>
              </div>

              <!-- 场景模式 -->
              <div class="control-section">
                <div class="control-section-label">模式</div>
                <div class="scene-btns">
                  <button class="scene-btn presentation" @click="applyScene('presentation')">📊 演示模式</button>
                  <button class="scene-btn energy" @click="applyScene('energy')">🌿 节能模式</button>
                </div>
              </div>
            </div>

            <div class="threejs-block">
              <div class="threejs-top">
                <h3><el-icon><View /></el-icon> 3D 会议室预览</h3>
              </div>
              <div class="threejs-box" id="threejsContainer">
                <iframe ref="threeIframe" :src="roomHtmlMap[currentRoom]" class="three-iframe" @load="onThreeLoad"></iframe>
              </div>
            </div>
          </div>

          <div class="bottom-row">
            <div class="log-env-row">
                <el-card class="panel-card env-card">
                  <template #header>
                    <div class="card-title-row compact">
                      <div>
                        <h3>环境传感</h3>
                        <p>树莓派数据</p>
                      </div>
                    </div>
                  </template>
                  <div class="env-list">
                    <div v-for="metric in metrics" :key="metric.label" class="env-row">
                      <span class="env-label">{{ metric.label }}</span>
                      <strong class="env-value">{{ metric.value }}</strong>
                    </div>
                  </div>
                  <div ref="lineChartRef" class="chart-box compact-chart"></div>
                </el-card>

                <el-card class="panel-card log-card">
                  <template #header>
                    <div class="card-title-row compact">
                      <div><h3>系统日志</h3><p>操作记录</p></div>
                    </div>
                  </template>
                  <div class="log-list" ref="logListRef">
                    <div v-if="systemLogs.length === 0" class="log-empty">暂无操作记录</div>
                    <div v-for="(log, idx) in systemLogs" :key="idx" class="log-item">
                      <span class="log-time">{{ log.time }}</span>
                      <span class="log-msg">{{ log.msg }}</span>
                    </div>
                  </div>
                </el-card>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getUserInfoAPI } from '@/apis/meetingControlAPI.js'
import { useIoTWebSocket } from '@/composables/useIoTWebSocket.js'

const router = useRouter()
const selectedIndex = ref('7')
const userName = ref('临时测试用户')
const userAvatar = ref('/lsj.jpg')

const currentRoom = ref('room1')
const roomNameMap = {
  room1: '会议室 1（大会议厅）',
  room2: '会议室 2（中会议厅）',
  room3: '会议室 3（小会议厅）'
}
const roomHtmlMap = {
  room1: '/3d/nordic-room.html',
  room2: '/3d/conference-room.html',
  room3: '/3d/sunny-meeting-room.html',
}
const roomOptions = [
  { value: 'room1', label: '会议室 1',  icon: '🏢' },
  { value: 'room2', label: '会议室 2',  icon: '🏛️' },
  { value: 'room3', label: '会议室 3',  icon: '🏠' }
]
const selectRoom = (val) => {
  currentRoom.value = val
  threeReady.value = false
  if (threeIframe.value) {
    threeIframe.value.src = roomHtmlMap[val] || roomHtmlMap.room1
  }
  addLog('切换到 ' + (roomNameMap[val] || val))
}
const roomMetricsMap = {
  room1: [
    { label: '温度', value: '25.8 °C' },
    { label: '湿度', value: '46 %' },
    { label: '光照强度', value: '532 lx' },
    { label: '烟雾浓度', value: '0.3 ppm' },
    { label: '人员数量', value: '12 人' }
  ],
  room2: [
    { label: '温度', value: '24.2 °C' },
    { label: '湿度', value: '42 %' },
    { label: '光照强度', value: '420 lx' },
    { label: '烟雾浓度', value: '0.2 ppm' },
    { label: '人员数量', value: '8 人' }
  ],
  room3: [
    { label: '温度', value: '26.1 °C' },
    { label: '湿度', value: '50 %' },
    { label: '光照强度', value: '310 lx' },
    { label: '烟雾浓度', value: '0.1 ppm' },
    { label: '人员数量', value: '4 人' }
  ]
}

const acPower = ref(true)
const acTemperature = ref(25)
const acMode = ref('cool')
const acFanSpeed = ref('auto')
const acSwingDir = ref('swing')

const acModes = [
  { value: 'cool', label: '制冷', icon: '❄️' },
  { value: 'heat', label: '制热', icon: '🔥' },
  { value: 'fan', label: '送风', icon: '💨' },
  { value: 'dry', label: '除湿', icon: '💧' }
]
const acFanSpeeds = [
  { value: 'auto', label: '自动' },
  { value: 'low', label: '低风' },
  { value: 'mid', label: '中风' },
  { value: 'high', label: '高风' }
]
const acSwingDirs = [
  { value: 'swing', label: '扫风', icon: '↕️' },
  { value: 'up', label: '向上', icon: '⬆️' },
  { value: 'down', label: '向下', icon: '⬇️' },
  { value: 'left', label: '向左', icon: '⬅️' },
  { value: 'right', label: '向右', icon: '➡️' }
]

const acModeLabel = computed(() => (acModes.find(item => item.value === acMode.value) || {}).label || '制冷')
const acFanLabel = computed(() => (acFanSpeeds.find(item => item.value === acFanSpeed.value) || {}).label || '自动')
const acSwingLabel = computed(() => (acSwingDirs.find(item => item.value === acSwingDir.value) || {}).label || '扫风')

const deviceState = reactive({
  airConditioner: true,
  light: true,
  projector: true
})

const deviceList = [
  { key: 'airConditioner', name: '空调', short: 'AC', note: '温控联动，保持舒适环境' },
  { key: 'light', name: '灯光', short: 'LT', note: '支持分区照明与演示亮度' },
  { key: 'projector', name: '投影仪', short: 'PJ', note: '演示与投屏设备' }
]
const otherDevices = computed(() => deviceList.filter(d => d.key !== 'airConditioner'))
const showAcPanel = ref(false)

// ---- 3D 场景通信 ----
const threeIframe = ref(null)
const threeReady = ref(false)
const sendToThree = (key, value) => {
  if (!threeIframe.value || !threeReady.value) return
  threeIframe.value.contentWindow.postMessage({ type: 'setDevice', key, value }, '*')
}
const onThreeLoad = () => {}
const onThreeReady = () => {
  threeReady.value = true
  const doorClosed = iot.doorState.value === '已关闭'
  const s = { airConditioner: acPower.value, light: deviceState.light, projector: deviceState.projector, door: doorClosed, acTemperature: acTemperature.value, acMode: acMode.value, acFanSpeed: acFanSpeed.value }
  Object.entries(s).forEach(([k, v]) => sendToThree(k, v))
}
const handleThreeMessage = (e) => {
  if (e.data?.type === 'threeReady') onThreeReady()
  if (e.data?.type === 'deviceStateChanged' && e.data.state) {
    const s = e.data.state
    if (s.airConditioner !== undefined && acPower.value !== s.airConditioner) acPower.value = s.airConditioner
    if (s.light !== undefined && deviceState.light !== s.light) deviceState.light = s.light
    if (s.projector !== undefined && deviceState.projector !== s.projector) deviceState.projector = s.projector
    if (s.acTemperature !== undefined && acTemperature.value !== s.acTemperature) acTemperature.value = s.acTemperature
  }
}
watch(acPower, (v) => sendToThree('airConditioner', v))
watch(() => deviceState.light, (v) => sendToThree('light', v))
watch(() => deviceState.projector, (v) => sendToThree('projector', v))
watch(acTemperature, (v) => sendToThree('acTemperature', v))
watch(acMode, (v) => sendToThree('acMode', v))
watch(acFanSpeed, (v) => sendToThree('acFanSpeed', v))
// ---- IoT WebSocket ----
const iot = useIoTWebSocket()

watch(() => iot.doorState.value, (v) => {
  if (v === '已关闭' || v === '已打开') sendToThree('door', v === '已关闭')
})

const metrics = computed(() => {
  const t = iot.temperature.value
  const h = iot.humidity.value
  const l = iot.lightRaw.value
  const s = iot.smokeRaw.value
  const p = iot.peopleCount.value
  return [
    { label: '温度', value: t != null ? t.toFixed(1) + ' °C' : '-- °C', tip: t != null ? (t > 26 ? '偏高' : t < 20 ? '偏低' : '舒适') : '等待数据' },
    { label: '湿度', value: h != null ? h.toFixed(0) + ' %' : '-- %', tip: h != null ? (h > 60 ? '偏高' : h < 30 ? '偏低' : '正常') : '等待数据' },
    { label: '光照强度', value: l != null ? l.toFixed(0) + ' lx' : '-- lx', tip: l != null ? (l > 500 ? '明亮' : '适中') : '等待数据' },
    { label: '烟雾浓度', value: s != null ? s.toFixed(1) + ' ppm' : '-- ppm', tip: s != null ? (s > 1 ? '注意' : '安全') : '等待数据' },
    { label: '人员数量', value: p != null ? p + ' 人' : '-- 人', tip: '实时统计' },
    { label: '门的状态', value: iot.doorState.value || '--', tip: '实时监测' },
    { label: '人员距离', value: iot.distance.value != null ? iot.distance.value.toFixed(0) + ' cm' : '-- cm', tip: '超声波测距' },
  ]
})
const onlineDevices = computed(() => Object.values(deviceState).filter(Boolean).length)
const roomStatus = computed(() => '正常')

watch(() => deviceState.airConditioner, value => { if (acPower.value !== value) acPower.value = value })

// ---- 系统日志 ----
const systemLogs = ref([])
const logListRef = ref(null)

const nowTime = () => {
  const d = new Date()
  return d.getHours().toString().padStart(2, '0') + ':' +
         d.getMinutes().toString().padStart(2, '0') + ':' +
         d.getSeconds().toString().padStart(2, '0')
}
const addLog = (msg) => {
  systemLogs.value.unshift({ time: nowTime(), msg })
  if (systemLogs.value.length > 50) systemLogs.value.pop()
  setTimeout(() => { if (logListRef.value) logListRef.value.scrollTop = 0 }, 50)
}

watch(acPower, (v) => { deviceState.airConditioner = v; addLog(v ? '空调已开启' : '空调已关闭') })
watch(acTemperature, (v, old) => { if (old !== undefined) addLog('温度调整为 ' + v + '°C') })
watch(acMode, () => { addLog('模式切换：' + acModeLabel.value) })
watch(acFanSpeed, () => { addLog('风速切换：' + acFanLabel.value) })
watch(acSwingDir, () => { addLog('风向切换：' + acSwingLabel.value) })
watch(() => deviceState.light, (v) => { addLog(v ? '灯光已开启' : '灯光已关闭') })
watch(() => deviceState.projector, (v) => { addLog(v ? '投影仪已开启' : '投影仪已关闭') })

const applyScene = scene => {
  if (scene === 'presentation') {
    deviceState.airConditioner = true; deviceState.light = true; deviceState.projector = true
    acPower.value = true; acTemperature.value = 25; acMode.value = 'cool'; acFanSpeed.value = 'low'
    addLog('演示模式')
  }
  if (scene === 'energy') {
    deviceState.airConditioner = false; deviceState.light = false; deviceState.projector = false
    acPower.value = false
    addLog('节能模式')
  }
}

const handleMenuSelect = menuIndex => {
  switch (menuIndex) {
    case '1': router.push('/user/home'); break
    case '2': router.push('/user/appoint'); break
    case '3': router.push('/user/check'); break
    case '4': router.push('/user/info'); break
    case '5': router.push('/user/meetingMinu'); break
    case '6': router.push('/user/notifications'); break
    case '7': router.push('/user/control'); break
    default: router.push('/user/home')
  }
}

const tpinfo = () => { router.push('/user/info') }
const handleExit = () => { iot.resetPeopleCount(); router.push('/') }

const createOverlay = () => {
  const overlay = document.createElement('div')
  overlay.className = 'menu-overlay'
  overlay.addEventListener('click', toggleMenu)
  document.body.appendChild(overlay)
  return overlay
}

const toggleMenu = () => {
  const menuElement = document.querySelector('.no1')
  const overlayElement = document.querySelector('.menu-overlay') || createOverlay()
  if (menuElement) {
    menuElement.classList.toggle('menu-expanded')
    overlayElement.classList.toggle('menu-overlay-visible')
  }
}

const lineChartRef = ref(null)
let lineChart = null
const disposeCharts = () => {
  lineChart?.dispose()
  lineChart = null
}
const renderCharts = () => {
  if (!lineChartRef.value) return
  const pts = iot.dataPoints.value || []
  const labels = pts.length > 0 ? pts.map(d => '#' + d.idx) : ['--']
  const temps  = pts.length > 0 ? pts.map(d => d.temperature ?? null) : [24]
  const hums   = pts.length > 0 ? pts.map(d => d.humidity ?? null) : [50]
  const lights = pts.length > 0 ? pts.map(d => d.light ?? null) : [300]
  const smokes = pts.length > 0 ? pts.map(d => d.smoke ?? null) : [0.3]

  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['温度', '湿度', '光照', '烟雾'],
      top: 0, left: 'center',
      itemWidth: 12, itemHeight: 8, icon: 'roundRect',
      textStyle: { color: '#606266', fontSize: 12 }
    },
    grid: { left: 38, right: 44, top: 38, bottom: 18, containLabel: true },
    xAxis: { type: 'category', data: labels },
    yAxis: [{ type: 'value' }, { type: 'value' }],
    series: [
      { name: '温度', type: 'line', smooth: true, data: temps },
      { name: '湿度', type: 'line', smooth: true, data: hums },
      { name: '光照', type: 'bar', yAxisIndex: 1, data: lights },
      { name: '烟雾', type: 'line', smooth: true, data: smokes, lineStyle: { color: '#999' }, itemStyle: { color: '#999' } }
    ]
  })
}
const resizeCharts = () => { lineChart?.resize() }

watch(deviceState, () => renderCharts(), { deep: true })
watch(() => iot.dataPoints.value, () => renderCharts(), { deep: true })
const fetchUserInfo = async () => {
  try {
    const res = await getUserInfoAPI()
    if (res.data?.data?.username) {
      userName.value = res.data.data.username
    }
    if (res.data?.data?.thumbnailUrl) {
      userAvatar.value = res.data.data.thumbnailUrl
    }
  } catch (e) {
    // 接口失败使用默认值
  }
}

onMounted(async () => {
  await fetchUserInfo()
  iot.connect()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
  window.addEventListener('message', handleThreeMessage)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  window.removeEventListener('message', handleThreeMessage)
  iot.disconnect()
  disposeCharts()
})
</script>

<style scoped>
/* ==================== 页面背景渐变（与 home.vue 完全一致） ==================== */
.body {
  border-radius: 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  background-attachment: fixed;
}

/* ==================== 顶部导航栏（与 home.vue 完全一致） ==================== */
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
}
.no0:hover {
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

/* 左侧Logo区域 */
.head-left {
  display: flex;
  align-items: center;
}
.head-left .logo {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 48px;
  padding: 0 16px;
  border-radius: 20px;
  background: linear-gradient(145deg, rgb(179, 219, 225), rgba(159, 199, 205, 1));
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}
.head-left .logo:hover {
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
}
.head-left .logo .logo-icon {
  font-size: 28px;
  color: #fff;
  animation: rotate 6s linear infinite;
}
.head-left .logo .logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
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
}
.user-info:hover {
  background: rgba(255, 255, 255, 1);
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
}
.user-info .user-avatar {
  width: 32px;
  height: 32px;
  border: 3px solid rgb(179, 219, 225);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}
.user-info .user-avatar:hover {
  transform: scale(1.1);
}
.user-info .user-name {
  color: rgb(179, 219, 225);
  font-size: 16px;
  font-weight: 600;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
}
.logout-btn:hover {
  background: rgb(244, 162, 175);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
}

/* 旋转动画 */
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ==================== 主容器（与 home.vue 完全一致） ==================== */
.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 15px;
  gap: 15px;
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

/* 右侧内容区域 - 白底玻璃质感 */
.no2 {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
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

/* 移动端菜单切换按钮 */
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
}
.mobile-menu-toggle .menu-icon {
  font-size: 20px;
  color: rgb(179, 219, 225);
}
.mobile-menu-toggle:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.control-layout { display: flex; flex-direction: column; gap: 16px; }

/* ===== 顶部大卡片 ===== */
.hero-card {
  display: flex; justify-content: space-between; align-items: center; gap: 32px;
  padding: 32px 36px;
  background: #fff; border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.04);
}
.hero-left h1 { margin: 0; font-size: 22px; font-weight: 700; color: #1a1a2e; }
.hero-left p { margin: 8px 0 0; font-size: 14px; color: #909399; }
.hero-stats { display: flex; gap: 16px; flex-shrink: 0; }
.hero-stat {
  background: #f8fafc; border-radius: 16px; padding: 18px 24px; text-align: center;
  min-width: 130px; border: 1px solid #f0f0f0;
}
.hero-stat-label { display: block; font-size: 12px; color: #909399; margin-bottom: 6px; }
.hero-stat-val { display: block; font-size: 28px; font-weight: 700; color: #303133; }
.hero-stat-val.danger { color: #f56c6c; }
.hero-stat small { display: block; font-size: 11px; color: #c0c4cc; margin-top: 4px; }

/* ===== 中间行：会议室切换 + 3D ===== */
.content-row { display: grid; grid-template-columns: 280px 1fr; gap: 14px; }

/* 智能控制侧边卡片 */
.control-side-card {
  background: #fff; border-radius: 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.04);
  padding: 16px; display: flex; flex-direction: column; gap: 16px; overflow-y: auto;
}
.control-card-title {
  display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 700; color: #303133;
}
.control-section { display: flex; flex-direction: column; gap: 8px; }
.control-section-label { font-size: 11px; font-weight: 600; color: #909399; text-transform: uppercase; letter-spacing: 0.5px; }
.room-switch-options { display: flex; flex-direction: column; gap: 6px; }
.room-option-btn {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border-radius: 10px; border: 1.5px solid #ebeef5; background: #fafbfc;
  cursor: pointer; transition: all 0.2s;
}
.room-option-btn:hover { border-color: #b3d8ff; background: #f5f9ff; }
.room-option-btn.active { border-color: #409eff; background: #ecf5ff; }
.room-option-icon { font-size: 18px; }
.room-option-name { font-size: 13px; font-weight: 600; color: #303133; }

/* 场景按钮 */
.scene-btns { display: flex; flex-direction: column; gap: 6px; }
.scene-btn {
  width: 100%; padding: 9px 12px; border-radius: 10px; border: 1.5px solid #ebeef5;
  background: #fff; cursor: pointer; font-size: 13px; font-weight: 600; transition: all 0.2s;
}
.scene-btn.presentation { border-color: #fae3cc; background: linear-gradient(135deg, #fef9f2, #fff); color: #e6a23c; }
.scene-btn.presentation:hover { box-shadow: 0 3px 10px rgba(230,162,60,0.12); }
.scene-btn.energy { border-color: #cfe8d5; background: linear-gradient(135deg, #f2faf4, #fff); color: #67c23a; }
.scene-btn.energy:hover { box-shadow: 0 3px 10px rgba(103,194,58,0.12); }

.threejs-block {
  background: rgba(255, 255, 255, 0.95); border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06); overflow: hidden;
}
.threejs-top {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 20px; border-bottom: 1px solid rgba(179, 219, 225, 0.2);
}
.threejs-top h3 { margin: 0; font-size: 16px; color: #4e5969; display: flex; align-items: center; gap: 8px; }
.threejs-desc { font-size: 12px; color: #909399; }
.threejs-box {
  margin: 12px; height: 480px; overflow: hidden;
  border-radius: 12px; box-sizing: border-box;
  background: #d6d0c8;
}
.three-iframe { width: 100%; height: 100%; border: none; display: block; }

.bottom-row { display: block; }

/* 设备列表 AC 行 */
.ac-row { cursor: pointer; transition: all 0.2s; }
.ac-row:hover { background: #eef6fb; border-color: #d0e6f5; }
.ac-row.expanded { background: #eef6fb; border-color: #b3d8ff; }
.ac-row-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.ac-expand-hint { color: #b0b0b0; font-size: 11px; }
.ac-expand-icon { font-size: 14px; color: #909399; transition: transform 0.25s; }
.ac-expand-icon.rotated { transform: rotate(180deg); }

/* 展开的遥控器面板 */
.ac-expand-panel {
  background: #f8fafc; border: 1px solid #e8ecf1; border-radius: 14px;
  padding: 16px; margin-top: -4px; display: grid; gap: 14px;
}
.ac-lcd-inline {
  text-align: center; padding: 14px 0 10px;
  background: linear-gradient(135deg, #0f172a, #1e293b); border-radius: 14px; color: #fff;
}
.ac-lcd-num-sm { font-size: 40px; font-weight: 300; letter-spacing: -1px; }
.ac-lcd-unit-sm { font-size: 20px; margin-left: 3px; opacity: 0.7; }
.ac-lcd-inline-info { margin-top: 4px; font-size: 12px; color: #94a3b8; }
.ac-inline-row { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }
.ac-power-btn-sm {
  display: flex; align-items: center; gap: 6px; padding: 8px 20px; border-radius: 24px;
  border: 1.5px solid #dcdfe6; background: #fff; font-size: 13px; cursor: pointer;
  color: #909399; transition: all 0.3s; font-weight: 600;
}
.ac-power-btn-sm.on { background: #10b981; border-color: #10b981; color: #fff; box-shadow: 0 3px 12px rgba(16,185,129,0.25); }
.temp-row { align-items: center; }
.ac-circle-sm {
  width: 30px; height: 30px; border-radius: 50%; border: 1.5px solid #dcdfe6; background: #fff;
  font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: #606266; transition: all 0.2s; flex-shrink: 0;
}
.ac-circle-sm:hover:not(:disabled) { background: #f0f0f0; }
.ac-circle-sm:disabled { opacity: 0.35; cursor: not-allowed; }
.ac-slider-sm {
  width: 120px; height: 4px; -webkit-appearance: none; appearance: none;
  background: linear-gradient(to right, #3b82f6, #f59e0b, #ef4444); border-radius: 2px;
  outline: none; cursor: pointer;
}
.ac-slider-sm::-webkit-slider-thumb {
  -webkit-appearance: none; width: 18px; height: 18px; border-radius: 50%;
  background: #fff; border: 3px solid #3b82f6; box-shadow: 0 2px 6px rgba(0,0,0,0.15); cursor: pointer;
}
.ac-temp-val { font-size: 16px; font-weight: 700; color: #303133; min-width: 40px; }
.ac-chip-sm {
  padding: 6px 12px; border-radius: 20px; border: 1.5px solid #ebeef5; background: #fff;
  cursor: pointer; font-size: 12px; color: #909399; transition: all 0.2s; font-weight: 500;
}
.ac-chip-sm:hover:not(:disabled) { border-color: #b3d8ff; background: #fafcff; }
.ac-chip-sm.sel { border-color: #409eff; background: #ecf5ff; color: #337ecc; font-weight: 600; }
.ac-chip-sm:disabled { opacity: 0.35; cursor: not-allowed; }
.ac-btn-icon { font-size: 18px; }

.panel-card {
  border: none;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}
.panel-card:hover { box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.06); }
.panel-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid rgba(179, 219, 225, 0.15);
  background: linear-gradient(180deg, rgba(179, 219, 225, 0.06), transparent);
}
.panel-card :deep(.el-card__body) { padding: 16px 18px; color: #303133; }

.card-title-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.card-title-row.compact h3 { margin: 0; color: #303133; font-size: 15px; font-weight: 600; }
.card-title-row.compact p { margin: 2px 0 0; font-size: 12px; color: #909399; }

.device-status-list { display: grid; gap: 8px; }
.device-status-row {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px; background: #f8fafc;
  border: 1px solid #f0f0f0;
}
.device-status-left { display: flex; align-items: center; gap: 10px; }
.device-icon { width: 40px; height: 40px; border-radius: 12px; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 12px; }
.device-icon.airConditioner { background: linear-gradient(135deg, #7ec8e3, #5fa9d9); }
.device-icon.light          { background: linear-gradient(135deg, #f7c873, #f09c52); }
.device-icon.projector      { background: linear-gradient(135deg, #8bc1a4, #5fa58f); }
.device-icon.door           { background: linear-gradient(135deg, #9aa7d1, #6f7db0); }
.device-name { font-weight: 700 !important; color: #111827 !important; font-size: 14px; }
.device-note { font-size: 13px; color: #374151 !important; margin-top: 1px; font-weight: 500 !important; }

.env-list { display: grid; gap: 6px; }
.env-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-radius: 10px; background: #f8fafc;
}
.env-label { color: #374151; font-size: 13px; font-weight: 600; }
.env-value { color: #1a1a2e; font-size: 16px; font-weight: 700; }

.scene-cards-inline { display: flex; gap: 8px; flex-shrink: 0; }
.scene-cards-inline .scene-card {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  border-radius: 20px; cursor: pointer; transition: all 0.2s;
  border: 1.5px solid #ebeef5; background: #fff; font-size: 13px;
}
.scene-cards-inline .scene-card:hover { transform: translateY(-1px); }
.scene-cards-inline .scene-card.presentation { border-color: #fae3cc; background: linear-gradient(135deg, #fef9f2, #fff); color: #e6a23c; }
.scene-cards-inline .scene-card.presentation:hover { box-shadow: 0 3px 10px rgba(230, 162, 60, 0.15); border-color: #e6a23c; }
.scene-cards-inline .scene-card.energy { border-color: #cfe8d5; background: linear-gradient(135deg, #f2faf4, #fff); color: #67c23a; }
.scene-cards-inline .scene-card.energy:hover { box-shadow: 0 3px 10px rgba(103, 194, 58, 0.15); border-color: #67c23a; }
.scene-cards-inline .scene-card-icon { font-size: 16px; flex-shrink: 0; }
.scene-cards-inline .scene-card-label { font-weight: 600; }

.chart-box { height: 280px; margin-top: 14px; }
.compact-chart { height: 220px; margin-top: 10px; }

/* 系统日志 + 环境传感 并排 */
.log-env-row { display: grid; grid-template-columns: 1.35fr 0.65fr; gap: 14px; }
.log-card :deep(.el-card__body) { max-height: 460px; overflow: hidden; display: flex; flex-direction: column; }
.log-list { flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; }
.log-list::-webkit-scrollbar { width: 4px; }
.log-list::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 2px; }
.log-empty { text-align: center; color: #909399; font-size: 13px; padding: 28px 0; }
.log-item {
  display: flex; gap: 8px; padding: 6px 10px; border-radius: 8px;
  background: #fafbfc; align-items: flex-start;
}
.log-time { color: #606266; font-size: 11px; white-space: nowrap; font-family: monospace; flex-shrink: 0; font-weight: 500; }
.log-msg { color: #1a1a2e; font-size: 13px; word-break: break-all; line-height: 1.5; font-weight: 600; }

@media (max-width: 1200px) {
  .hero-card { flex-direction: column; align-items: flex-start; }
  .hero-stats { width: 100%; }
  .hero-stat { flex: 1; min-width: 0; }
  .content-row { grid-template-columns: 1fr; }
  .room-switch-options { flex-direction: row; }
  .room-option-btn { flex: 1; text-align: center; align-items: center; }
  .bottom-row { grid-template-columns: 1fr; }
  .threejs-box { height: 360px; }
}

@media (max-width: 768px) {
  .head-container { padding: 0 12px !important; }
  .logo-text { display: none !important; }
  .user-name { display: block !important; font-size: 12px !important; max-width: 80px !important; }
  .user-info { padding: 0 12px !important; gap: 6px !important; }
  .user-avatar { width: 28px !important; height: 28px !important; }
  .mobile-menu-toggle { display: flex !important; align-items: center; justify-content: center; }
  .main-container { flex-direction: column !important; padding: 0 !important; gap: 0 !important; position: relative !important; flex: 1 !important; min-height: calc(100vh - 60px) !important; }
  .no1 {
    width: 0 !important; padding: 0 !important; overflow: hidden !important;
    transform: translateX(-100%) !important; position: fixed !important;
    left: 0 !important; top: 60px !important; bottom: 0 !important;
    z-index: 100 !important; height: calc(100vh - 60px) !important; width: 200px !important;
    transition: all 0.3s ease !important;
  }
  .no1.menu-expanded { transform: translateX(0) !important; width: 200px !important; padding: 10px !important; }
  .no2 { width: 100% !important; margin: 0 !important; padding: 10px !important; overflow-y: auto !important; -webkit-overflow-scrolling: touch !important; scroll-behavior: smooth !important; flex: 1 !important; min-height: 450px !important; border-radius: 0 !important; }
  body { margin: 0 !important; padding: 0 !important; }
  .body { border-radius: 0 !important; margin: 0 !important; padding: 0 !important; }
  .no0 { border-radius: 0 !important; margin: 0 !important; padding: 0 !important; }
  .head-container { padding: 0 8px !important; }
  .menu-overlay {
    position: fixed !important; top: 60px !important; left: 0 !important; right: 0 !important; bottom: 0 !important;
    background: rgba(0, 0, 0, 0.3) !important; z-index: 99 !important;
    opacity: 0 !important; visibility: hidden !important; transition: all 0.3s ease !important;
  }
  .menu-overlay.menu-overlay-visible { opacity: 1 !important; visibility: visible !important; }
  .hero-card { flex-direction: column !important; padding: 20px !important; }
  .hero-stats { width: 100% !important; }
  .hero-stat { flex: 1 !important; min-width: 0 !important; padding: 12px 8px !important; }
  .hero-stat-val { font-size: 24px !important; }
  .content-row { grid-template-columns: 1fr !important; }
  .room-switch-options { flex-direction: row !important; }
  .room-option-btn { flex: 1 !important; text-align: center !important; align-items: center !important; }
  .bottom-row { grid-template-columns: 1fr; }
  .log-env-row { grid-template-columns: 1fr; }
  .threejs-box { height: 260px; margin: 6px; }
  .threejs-desc { display: none; }
  .no2::-webkit-scrollbar { display: none !important; }
}
</style>

<style>
/* 强制加深三个卡片文字颜色 —— 非 scoped */
.env-card .env-label,
.env-card .env-value,
.log-card .log-msg,
.log-card .log-time,
.log-card .log-empty {
  color: inherit;
}

.env-card .env-label { color: #374151 !important; font-weight: 600 !important; }
.env-card .env-value { color: #111827 !important; font-weight: 700 !important; }
.log-card .log-msg   { color: #111827 !important; font-weight: 600 !important; }
.log-card .log-time  { color: #606266 !important; font-weight: 500 !important; }
.log-card .log-empty { color: #909399 !important; }
</style>
