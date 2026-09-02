import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function getUserInfo(){
    
    return  httpInstance({
        url:'/api/user/all',
        method:'get',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
        params:{

        }
    })
}
