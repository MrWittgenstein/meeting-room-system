// loginAPI.js
import httpInstance from '@/utils/http'

export function loginEmailCodeAPI(email) {
    return httpInstance({
        url: 'user/sendcode',
        method: 'post',
        params: {
            email: email
        }
    })
}

export function passwordLoginAPI(email, password) {
    return httpInstance({
        url: 'user/login/password',
        method: 'post',
        data: {
            email: email,
            password: password
        }
    })
}

export function emailLoginAPI(email, code) {
    return httpInstance({
        url: 'user/login/email',
        method: 'post',
        data: {
            email: email,
            code: code
        }
    })
}

// 新增忘记密码验证码接口
export function forgotPasswordCodeAPI(email) {
    return httpInstance({
        url: 'user/sendcode',
        method: 'post',
        params: {
            email: email
        }
    })
}

// 修改密码API
export function changePasswordAPI(data) {
    return httpInstance({
        url: 'user/changepassword',
        method: 'post',
        data: data
    })
}
