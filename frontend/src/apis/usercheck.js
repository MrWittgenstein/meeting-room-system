// @/apis/usercheck.js
import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');
/**
 * 获取用户信息（头像、用户名）
 */
export const getUserInfo = async () => {
  return httpInstance({
    url: '/user/info',
    method: 'GET',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

/**
 * 获取用户预约记录
 */
export const getBookingRecords = async () => {
  return httpInstance({
    url: '/user/getreservations',
    method: 'GET',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

/**
 * 取消预约申请
 * @param {number} reservationId - 预约记录ID
 */
export const cancelBooking = async (reservationId) => {
  return httpInstance({
    url: '/user/cancelreservation',
    method: 'DELETE',
    params: { reservationId },
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}
