import httpInstance from '@/utils/http'
// 获取本地存储的管理员token
const token = localStorage.getItem('token') || ''

/**
 * 获取已审批会议列表（对接接口：GET /approver/reservation/getlist）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页条数
 * @param {string} [params.roomType] - 会议室类型筛选
 * @returns {Promise} - 请求结果
 */
export const getApprovedListAPI = (params) => {
  return httpInstance({
    url: 'approver/reservation/getlist',
    method: 'get',
    headers: {
      'Authorization': `Bearer ${token}`
    },

    params: {
      page: 1,
      size: 20,
      ...params
    }
  })
}

/**
 * 删除已审批会议记录（对接接口：DELETE /user/cancelreservation）
 * @param {number} reservationId - 会议预约ID
 * @returns {Promise} - 请求结果
 */
export const deleteApprovedRecordAPI = (reservationId) => {
  return httpInstance({
    url: 'user/cancelreservation',
    method: 'delete',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    params: {
      reservationId // 接口必填参数：会议预约ID
    }
  })
}
