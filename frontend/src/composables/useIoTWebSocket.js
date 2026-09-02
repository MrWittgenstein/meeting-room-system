import { ref } from 'vue'

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

  let pollTimer = null

  const applyEnv = (env) => {
    if (!env) return
    if (env.temperature !== undefined) temperature.value = Number(env.temperature)
    if (env.humidity !== undefined) humidity.value = Number(env.humidity)
    if (env.light !== undefined) lightRaw.value = Number(env.light)
    if (env.smoke_raw !== undefined) smokeRaw.value = Number(env.smoke_raw)
  }

  const pollLatest = async () => {
    try {
      const res = await fetch('http://localhost:8080/iot/rooms/14/latest')
      const json = await res.json()
      const env = json?.data?.environment
      if (env) {
        applyEnv(env)
        const faceCount = json.data?.access_control?.face_count
        const faceAuth = json.data?.access_control?.authorized
        console.log('[Poll] 人脸:', { faceCount, faceAuth, currentPeople: peopleCount.value })
        if (faceCount !== undefined && Number(faceCount) > 0 && (faceAuth === true || faceAuth === 'true')) {
          peopleCount.value += Number(faceCount)
          localStorage.setItem(STORAGE_KEY, peopleCount.value)
          console.log('[Poll] 人员数量 +' + faceCount + ' → ' + peopleCount.value)
        }
        const ds = json.data?.access_control?.door_state
        if (ds !== undefined) doorState.value = ds === 'closed' ? '已关闭' : '已打开'
        const pir = json.data?.presence?.pir
        if (pir !== undefined) pirState.value = pir ? '有人' : '无人'
        const dist = json.data?.presence?.distance
        if (dist !== undefined) distance.value = Number(dist)
        connected.value = true
        console.log('[Poll] 数据更新:', { t: temperature.value, h: humidity.value, l: lightRaw.value, s: smokeRaw.value, p: peopleCount.value, door: doorState.value })
      }
    } catch (e) {
      console.warn('[Poll] 请求失败:', e.message)
    }
  }

  const pollLastSeven = async () => {
    try {
      const res = await fetch('http://localhost:8080/iot/rooms/14/last-seven')
      const json = await res.json()
      const list = json?.data || json || []
      if (Array.isArray(list) && list.length > 0) {
        const reversed = [...list].reverse()
        const newData = reversed.map((d, i) => ({
          idx: reversed.length - i,
          temperature: d?.environment?.temperature != null ? Number(d.environment.temperature) : null,
          humidity: d?.environment?.humidity != null ? Number(d.environment.humidity) : null,
          light: d?.environment?.light != null ? Number(d.environment.light) : null,
          smoke: d?.environment?.smoke_raw != null ? Number(d.environment.smoke_raw) : null,
          time: d?.time || d?.timestamp || '',
        }))
        dataPoints.value.splice(0, dataPoints.value.length, ...newData)
      }
    } catch (e) {
      console.warn('[Poll] last-seven 失败:', e.message)
    }
  }

  const pollAll = async () => {
    await pollLastSeven()
    await pollLatest()
  }

  const connect = () => {
    pollAll()
    pollTimer = setInterval(pollAll, 5000)
    connected.value = true
    console.log('[Poll] 开始轮询 (每5秒)')
  }

  const resetPeopleCount = () => {
    peopleCount.value = 0
    localStorage.removeItem(STORAGE_KEY)
  }

  const disconnect = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
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
