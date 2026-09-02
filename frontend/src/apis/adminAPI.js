import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');

/**
 * 获取全部预约清单
 * @param {Object} params - 查询参数（可选）
 * @param {number} [params.roomId] - 会议室ID
 * @param {string} [params.date] - 日期（格式 yyyy-MM-dd，支持范围如"2025-12-01,2025-12-07"）
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=1000] - 每页条数（默认取足量数据用于统计）
 */
export function getReservationListAPI(params) {
  return httpInstance({
    url: 'approver/reservation/getlist',
    method: 'get',
    params: {
      page: 1,
      size: 1000,
      ...params
    },
    headers:{
      'Authorization': `Bearer ${token}`,
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
      'Authorization': `Bearer ${token}`,
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
      'Authorization': `Bearer ${token}`,
    },
  })
}

/**
 * 获取管理员统计信息
 */
export function getAdminStatsAPI() {
  return httpInstance({
    url: '/api/statistics/admin',
    method: 'get',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
}

/**
 * 获取会议室使用频率图表数据
 * @param {Object} params - 查询参数
 * @param {string} params.period - 时间周期（week/month/quarter）
 */
export function getRoomFrequencyChartAPI(params) {
  return httpInstance({
    url: '/api/statistics/room-frequency/chart',
    method: 'get',
    params: params,
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
}
