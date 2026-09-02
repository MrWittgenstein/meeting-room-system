// registerAPI.js
import httpInstance from '@/utils/http'

export function registerEmailCodeAPI(email) {
    return httpInstance({
        url: 'user/sendcode', // 区分注册验证码接口
        method: 'post',
        params: {
            email: email
        }
    })
}

export function registerAPI(username, email, code, password) {
    return httpInstance({
        url: 'user/register',
        method: 'post',
        data: {
            username: username,
            email: email,
            code: code,
            password: password,
            realName:"",
            phone:"",
            userType:1
        }
    })
}
