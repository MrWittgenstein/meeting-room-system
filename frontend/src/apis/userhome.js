// @/apis/userAPI.js
import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');
/**
 * 获取用户信息（真实接口+容错）
 */
export const getUserInfoAPI = async () => {
  try {
    return await httpInstance({
      url: '/user/info',
      method: 'get',
      headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
    })
  } catch (error) {
    // 接口失败时返回默认结构，不影响界面渲染
    return {
      data: {
        code: 0,
        message: '接口暂未授权',
        data: {
          username: '临时测试用户',
          thumbnailUrl: '/lsj.jpg'
        }
      }
    }
  }
}

/**
 * 获取用户预约记录（真实接口+容错）
 */
export const getUserReservationsAPI = async () => {
  try {
    return await httpInstance({
      url: '/user/getreservations',
      method: 'get',
      headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
    })
  } catch (error) {
    // 接口失败时返回空数据
    return {
      data: {
        code: 0,
        message: '接口暂未授权',
        data: []
      }
    }
  }
}
