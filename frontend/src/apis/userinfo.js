// @/apis/userAPI.js
import httpInstance from '@/utils/http' // 假设已经有请求工具
const token = localStorage.getItem('token');
// 获取用户信息
export const getUserInfo = async () => {
  try {
    return await httpInstance({
      url: '/user/info',
      method: 'get',
      headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
    })
  } catch (error) {
    // 接口失败时返回默认结构，不影响界面渲染
    return {
      data: {
        code: 0,
        message: '接口暂未授权',
        data: {
          username: '临时测试用户',
          thumbnailUrl: '/lsj.jpg'
        }
      }
    }
  }
}

// 发送验证码（用于修改密码）
export const sendVerificationCode = (email) => {
  return httpInstance({
    url: '/user/sendcode',
    method: 'post',
    data:email,
  })
}

// 更新用户信息
export const updateUserInfo = (userInfo) => {
  return httpInstance({
    url: '/user/update',
    method: 'put',
    data: userInfo,
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

// 修改密码
export const changePassword = (data) => {
  return httpInstance({
    url: '/user/changepassword',
    method: 'post',
    data,
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}

// 上传头像
export const uploadAvatar = (formData) => {
  return httpInstance({
    url: '/user/uploadavatar',
    method: 'post',
    data: formData,
    headers:{
            'Authorization': `Bearer ${token}`, // 添加JWT请求头
        },
  })
}
