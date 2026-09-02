import httpInstance from '@/utils/http'

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
