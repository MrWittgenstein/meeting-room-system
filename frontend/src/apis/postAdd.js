import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function postAdd(){
    return httpInstance({
        url:'/user/addreservation',
        method:'post',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        }
    })
}