import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');

/**
 * 获取所有会议室清单（分页）
 * 接口文档对应：GET /approver/meetingroom/list
 */
export function getMeetingRoomList(params) {
    return httpInstance({
        url: 'approver/meetingroom/list',
        method: 'get',
        params: {
            page: 1,
            size: 10,
            ...params
        }
    })
}

/**
 * 新增会议室
 * 接口文档对应：POST /approver/meetingroom/create
 */
export function addMeetingRoom(data) {
    const headers = {
        'Authorization': `Bearer ${token}`
    }
    return httpInstance({
        url: 'approver/meetingroom/create',
        method: 'post',
        headers,
        data: data
    })
}

/**
 * 编辑会议室信息
 * 接口文档对应：PUT /approver/meetingroom/meetroom/change
 */
export function updateMeetingRoom(meetingRoomData) {
    return httpInstance({
        url: 'approver/meetingroom/meetroom/change',
        method: 'put',
        data: meetingRoomData
    })
}

/**
 * 删除会议室
 * 接口文档对应：DELETE /approver/meetingroom/delete/{roomId}
 */
export function deleteMeetingRoom(roomId) {
    return httpInstance({
        url: `approver/meetingroom/delete/${roomId}`,
        method: 'delete',
        params:{
          roomId:roomId
        },
    })
}

/**
 * 上传会议室图片
 * 接口文档对应：POST /approver/meetingroom/image
 */
export function uploadMeetingRoomImage(params) {
    const { roomId, file } = params;
    const formData = new FormData();
    formData.append('file', file);
    return httpInstance({
        url: 'approver/meetingroom/image',
        method: 'post',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        params: { roomId },
        data: formData
    })
}
