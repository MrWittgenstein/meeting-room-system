import httpInstance  from "@/utils/http";
const token = localStorage.getItem('token');
export function postconfirm(data){
    
    return  httpInstance({
        url:'/user/update',
        method:'put',
        headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
        data:data
    })
}
