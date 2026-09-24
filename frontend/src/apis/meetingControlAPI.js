import httpInstance from '@/utils/http'

export const getControlRoomsAPI = () => httpInstance({
  url: '/iot/control-rooms',
  method: 'get'
})

/**
 * 获取用户信息
 */
export const getUserInfoAPI = () => {
  const token = localStorage.getItem('token')
  return httpInstance({
    url: '/user/info',
    method: 'get',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }).catch(() => {
    return {
      data: {
        code: 0,
        data: {
          username: '临时测试用户',
          thumbnailUrl: '/lsj.jpg'
        }
      }
    }
  })
}

export const sendDeviceCommandAPI = (deviceId, command) => httpInstance({
  url: `/iot/devices/${encodeURIComponent(deviceId)}/commands`,
  method: 'post',
  data: command
})

export const getDeviceControlAccessAPI = (deviceId) => httpInstance({
  url: `/iot/devices/${encodeURIComponent(deviceId)}/control-access`,
  method: 'get'
})

export const getDeviceLatestAPI = (deviceId) => httpInstance({
  url: `/iot/devices/${encodeURIComponent(deviceId)}/latest`,
  method: 'get'
})
