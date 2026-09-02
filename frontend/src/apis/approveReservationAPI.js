import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');
const authHeaders = {
  'Authorization': `Bearer ${token}`
};

/**
 * 获取当天待审批预约列表（分页）
 * 接口文档对应：GET /approver/reservation/getlist
 * @param {Object} params - 请求参数
 * @param {string} params.date - 会议日期（格式：YYYY-MM-DD，必选）
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页条数
 * @param {number} [params.status=0] - 预约状态（0-待审批，必选）
 */
export function getPendingApprovalList(params) {
  return httpInstance({
    url: 'approver/reservation/getlist',
    method: 'get',
    headers: authHeaders,
    params: {
      page: 1,
      size: 10,
      status: 0, // 默认待审批状态（接口文档中status=0为待审批）
      ...params
    }
  });
}

/**
 * 审批预约（通过/拒绝）
 * 接口文档对应：POST /approver/reservation/approve
 * @param {Object} data - 审批参数
 * @param {number} data.reservationId - 预约ID（必选）
 * @param {number} data.approveStatus - 审批状态（1-通过，2-拒绝，必选）
 * @param {string} [data.rejectReason] - 拒绝原因（拒绝时必选）
 */
export function approveReservation(data) {
  return httpInstance({
    url: 'approver/reservation/approve',
    method: 'post',
    headers: authHeaders,
    data: {
      approveTime: new Date().toISOString(), // 审批时间（接口文档ApproveDto可选，补充提交）
      ...data
    }
  });
}

/**
 * 查看预约详情（含会议室信息）
 * 接口文档对应：GET /approver/reservation/getlist + GET /approver/meetingroom/list
 * @param {number} reservationId - 预约ID
 * @param {number} roomId - 会议室ID（用于关联获取会议室信息）
 */
export function getReservationDetail({ reservationId, roomId }) {
  // 1. 获取预约详情
  const getReservation = httpInstance({
    url: 'approver/reservation/getlist',
    method: 'get',
    headers: authHeaders,
    params: {
      reservationId, // 传预约ID筛选单个详情
      page: 1,
      size: 1
    }
  });

  // 2. 获取关联的会议室信息
  const getRoomInfo = httpInstance({
    url: 'approver/meetingroom/list',
    method: 'get',
    headers: authHeaders,
    params: {
      roomId, // 传会议室ID筛选单个会议室
      page: 1,
      size: 1
    }
  });

  // 3. 合并预约+会议室信息返回
  return Promise.all([getReservation, getRoomInfo]).then(([reservationRes, roomRes]) => {
    const reservation = reservationRes.data.data[0] || {};
    const room = roomRes.data.data[0] || {};
    // 字段映射（匹配页面显示需求）
    return {
      id: reservation.reservationId,
      roomType: room.type || '未知类型', // 会议室类型（来自会议室接口）
      roomLocation: room.location || '未知位置', // 会议室地点（来自会议室接口）
      title: reservation.purpose || '无会议标题', // 会议标题（对应接口purpose）
      description: reservation.purpose || '无会议描述', // 会议描述（对应接口purpose）
      date: reservation.date || '', // 会议日期
      startTime: reservation.startTime || '', // 开始时间
      endTime: reservation.endTime || '', // 结束时间
      initiator: `用户${reservation.username}` || '未知申请人', // 申请人（暂用userId，可后续关联用户接口）
      phone: reservation.phone || '无联系电话', // 联系电话（假设接口返回phone字段）
      participantsCount: reservation.participantsCount || 0, // 参会人数
      status: reservation.status, // 审核状态
      applyTime: reservation.createTime || '', // 申请时间（对应接口createTime）
      remark: reservation.remark || '无备注' // 备注信息
    };
  });
}
