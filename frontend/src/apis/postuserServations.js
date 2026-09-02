import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function postuserSrevations(){
    
    return  httpInstance({
        url:'/user/getmeetingrooms',
        method:'post',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
    })
}