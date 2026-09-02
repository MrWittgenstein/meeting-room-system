import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

// 后端 STOMP 实时推送端点（WebSocketConfig 注册的原生 WS 端点 /ws）
// 消息由 IotDeviceWebSocketHandler 在每次遥测落库后经 /topic/iot/rooms/{room_code} 广播
const STOMP_BROKER_URL = 'ws://localhost:8080/ws'
const ROOM_CODE = 'meeting_room_14'
const API_BASE = 'http://localhost:8080'

export function useIoTWebSocket() {
  const connected = ref(false)
  const temperature = ref(null)
  const humidity = ref(null)
  const lightRaw = ref(null)
  const smokeRaw = ref(null)
  const doorState = ref('--')
  const pirState = ref('--')
  const distance = ref(null)
  const dataPoints = ref([])
  const STORAGE_KEY = 'iot_people_count'
  const peopleCount = ref(Number(localStorage.getItem(STORAGE_KEY)) || 0)

  let client = null

  const applyEnv = (env) => {
    if (!env) return
    if (env.temperature !== undefined) temperature.value = Number(env.temperature)
    if (env.humidity !== undefined) humidity.value = Number(env.humidity)
    if (env.light !== undefined) lightRaw.value = Number(env.light)
    if (env.smoke_raw !== undefined) smokeRaw.value = Number(env.smoke_raw)
  }

  // current 为 IotTelemetryVo：{ environment, presence, access_control, ... }（推送消息无 Result 包装）
  const applyCurrent = (current, countPeople = true) => {
    if (!current) return
    applyEnv(current.environment)
    const faceCount = current.access_control?.face_count
    const faceAuth = current.access_control?.authorized
    if (countPeople && faceCount !== undefined && Number(faceCount) > 0 && (faceAuth === true || faceAuth === 'true')) {
      peopleCount.value += Number(faceCount)
      localStorage.setItem(STORAGE_KEY, peopleCount.value)
      console.log('[Realtime] 人员数量 +' + faceCount + ' → ' + peopleCount.value)
    }
    const ds = current.access_control?.door_state
    if (ds !== undefined) doorState.value = ds === 'closed' ? '已关闭' : '已打开'
    const pir = current.presence?.pir
    if (pir !== undefined) pirState.value = pir ? '有人' : '无人'
    const dist = current.presence?.distance
    if (dist !== undefined) distance.value = Number(dist)
    connected.value = true
    console.log('[Realtime] 数据更新:', { t: temperature.value, h: humidity.value, l: lightRaw.value, s: smokeRaw.value, p: peopleCount.value, door: doorState.value })
  }

  // 按时间升序的遥测列表 → 折线图数据点
  const applyHistory = (ascendingList) => {
    const newData = ascendingList.map((d, i) => ({
      idx: ascendingList.length - i,
      temperature: d?.environment?.temperature != null ? Number(d.environment.temperature) : null,
      humidity: d?.environment?.humidity != null ? Number(d.environment.humidity) : null,
      light: d?.environment?.light != null ? Number(d.environment.light) : null,
      smoke: d?.environment?.smoke_raw != null ? Number(d.environment.smoke_raw) : null,
      time: d?.time || d?.timestamp || '',
    }))
    dataPoints.value.splice(0, dataPoints.value.length, ...newData)
  }

  // 推送消息 body 为 IotRealtimeMessageVo：{ current, history }，history 已按时间升序（后端已反转）
  const handleRealtime = (frame) => {
    let body = null
    try {
      body = JSON.parse(frame.body)
    } catch (e) {
      console.warn('[Realtime] 消息解析失败:', e.message)
      return
    }
    if (body?.current) applyCurrent(body.current)
    const list = body?.history || []
    if (list.length > 0) applyHistory(list)
  }

  // 首次连接时用 REST 拉一次最新值 + 历史曲线作为种子数据（不统计人数，避免与推送重复计数），
  // 之后完全依靠 STOMP 实时推送，不再轮询
  const seedFromRest = async () => {
    try {
      const latestRes = await fetch(`${API_BASE}/iot/rooms/14/latest`)
      const latestJson = await latestRes.json()
      if (latestJson?.data) applyCurrent(latestJson.data, false)
    } catch (e) {
      console.warn('[Realtime] 种子数据(latest) 失败:', e.message)
    }
    try {
      const historyRes = await fetch(`${API_BASE}/iot/rooms/14/last-seven`)
      const historyJson = await historyRes.json()
      const list = historyJson?.data || historyJson || []
      if (Array.isArray(list) && list.length > 0) {
        // REST 返回按时间倒序，反转成升序
        applyHistory([...list].reverse())
      }
    } catch (e) {
      console.warn('[Realtime] 种子数据(last-seven) 失败:', e.message)
    }
  }

  const connect = () => {
    client = new Client({
      brokerURL: STOMP_BROKER_URL,
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        console.log('[Realtime] STOMP 已连接，订阅 /topic/iot/rooms/' + ROOM_CODE)
        client.subscribe('/topic/iot/rooms/' + ROOM_CODE, handleRealtime)
        seedFromRest()
      },
      onStompError: (frame) => {
        console.warn('[Realtime] STOMP 错误:', frame.headers?.message || frame.body)
      },
      onWebSocketClose: () => {
        connected.value = false
      },
    })
    client.activate()
  }

  const resetPeopleCount = () => {
    peopleCount.value = 0
    localStorage.removeItem(STORAGE_KEY)
  }

  const disconnect = () => {
    client?.deactivate()
    client = null
    connected.value = false
  }

  return {
    connected,
    temperature,
    humidity,
    lightRaw,
    smokeRaw,
    doorState,
    pirState,
    distance,
    dataPoints,
    peopleCount,
    connect,
    disconnect,
    resetPeopleCount,
  }
}
