// @/apis/userappointAPI.js
import httpInstance from '@/utils/http' // 引入封装的axios实例

// 动态获取token（避免页面刷新后token失效问题）
const getToken = () => localStorage.getItem('token');

/**
 * 获取用户信息（头像、用户名）
 */
export const getUserInfo = async () => {
  return httpInstance({
    url: '/user/info',
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${getToken()}`, // 携带token请求
    },
  })
}

/**
 * 获取指定日期的会议室列表（含可预约时段）
 * @param {string} date - 选择的日期（格式：YYYY-MM-DD）
 */
export const getMeetingRooms = async (date) => {
  return httpInstance({
    url: '/user/getmeetingrooms',
    method: 'POST',
    params: {
      queryDate: date
    },
    headers: {
      'Authorization': `Bearer ${getToken()}`,
    },
  })
}

/**
 * 提交会议室预约
 * @param {object} appointData - 预约信息
 */
export const submitAppointment = async (appointData) => {
  return httpInstance({
    url: '/user/addreservation',
    method: 'POST',
    data: appointData,
    headers: {
      'Authorization': `Bearer ${getToken()}`,
    },
  })
}
