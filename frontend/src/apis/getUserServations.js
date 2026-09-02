import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function getUserServations(){
    return httpInstance({
        url:'/user/getreservations',
        method:'get',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        }
    })
}