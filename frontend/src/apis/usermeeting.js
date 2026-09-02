// @/apis/meetingRecordAPI.js
import httpInstance from '@/utils/http' // 项目封装的axios实例
const token = localStorage.getItem('token');
/**
 * 1. 获取用户信息（头像、用户名）
 * 接口文档对应：GET /user/info
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
 * 2. 获取用户预约记录（用于计算会议统计和图表数据）
 * 接口文档对应：GET /user/getreservations
 */
export const getUserReservations = async () => {
  return httpInstance({
    url: '/user/getreservations',
    method: 'GET',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

/**
 * 3. 辅助函数：根据会议室ID映射会议室名称（匹配页面原有显示格式）
 * 若接口返回包含roomNumber，可直接替换；此处按接口文档默认roomId映射
 */
export const getRoomNameByRoomId = (roomId) => {
  const roomMap = {
    1: '111会议室',
    2: '101会议室',
    3: '222会议室',
    4: '331会议室',
    5: '多功能厅'
  }
  return roomMap[roomId] || `${roomId}号会议室`
}

/**
 * 4. 辅助函数：根据roomId映射会议室类型（匹配页面“多媒体/普通/主席台”）
 * 可根据实际接口返回的roomType调整，此处按常见业务逻辑映射
 */
export const getRoomTypeByRoomId = (roomId) => {
  const typeMap = {
    1: '多媒体会议室',
    2: '普通会议室',
    3: '多媒体会议室',
    4: '多媒体会议室',
    5: '主席台'
  }
  return typeMap[roomId] || '普通会议室'
}
