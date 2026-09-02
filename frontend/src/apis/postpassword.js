import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function postpassword(data){
    
    return  httpInstance({
        url:'/user/changepassword',
        method:'post',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
        data:data
    })
}