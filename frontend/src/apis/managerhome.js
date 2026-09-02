// adminAPI.js
import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');
/**
 * 获取全部预约清单
 * @param {Object} params - 查询参数（可选）
 * @param {number} [params.roomId] - 会议室ID
 * @param {string} [params.date] - 日期（格式 yyyy-MM-dd）
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页条数
 */
export function getReservationListAPI(params) {
  return httpInstance({
    url: 'approver/reservation/getlist',
    method: 'get',
    params: {
      page: 1, // 默认值
      size: 10, // 默认值
      ...params // 覆盖默认参数
    },
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

/**
 * 获取所有会议室清单
 * @param {Object} params - 分页参数（必选）
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页条数
 */
export function getAllMeetroomsAPI(params) {
  return httpInstance({
    url: 'approver/meetingroom/list',
    method: 'get',
    params: params,
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

/**
 * 获取当前登录用户信息（管理员）
 */
export function getUserInfoAPI() {
  return httpInstance({
    url: 'user/info',
    method: 'get',
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}
