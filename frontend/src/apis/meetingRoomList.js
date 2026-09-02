import httpInstance from '@/utils/http'
const token = localStorage.getItem('token');
export function getMeetingRoomList() {
    return httpInstance({
        url: 'approver/meetingroom/list',
        method: 'get',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        params: {
            page: 1,
            size: 10
        }
    })
}

export function pendingApprovalList(status) {
    return httpInstance({
        url: 'approver/reservation/getlist',
        method: 'get',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        params: {
            page: 1,
            size: 10,
            status:status
        }
    })
}

export function addMeetingRoom(meetingRoomData) {
    return httpInstance({
        url: 'approver/meetingroom/create',
        method: 'post',
        data: meetingRoomData
    })
}
export function deleteMeetingRoom(roomId) {
    return httpInstance({
        url: `approver/meetingroom/delete/${roomId}`,
        method: 'delete',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
    })
}
