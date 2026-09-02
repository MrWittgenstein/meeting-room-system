// @/apis/userAPI.js
import httpInstance from '@/utils/http'

// 从本地存储获取登录token（登录时已存入）
const token = localStorage.getItem('adminToken');
const authHeaders = {
  'Authorization': `Bearer ${token}`
};

/**
 * 获取用户个人信息
 * 接口文档对应：GET /user/info
 */
export const getUserInfo = async () => {
  try {
    return await httpInstance({
      url: '/user/info',
      method: 'get',
      headers: authHeaders
    });
  } catch (error) {
    // 接口异常时返回默认数据，避免页面崩溃
    return {
      data: {
        code: 0,
        message: '获取用户信息失败',
        data: {
          userId: 'U20230518001',
          username: 'johndoe',
          realName: '张三',
          phone: '13800138000',
          email: 'john@example.com',
          thumbnailUrl: '' // 头像地址默认空
        }
      }
    };
  }
};

/**
 * 更新用户基础信息
 * 接口文档对应：PUT /user/update
 * @param {Object} userInfo - 待更新的用户信息
 */
export const updateUserInfo = (userInfo) => {
  return httpInstance({
    url: '/user/update',
    method: 'put',
    headers: authHeaders,
    data: userInfo
  });
};

/**
 * 修改密码
 * 接口文档对应：POST /user/changepassword
 * @param {Object} passwordData - 密码修改参数
 * @param {string} passwordData.email - 绑定邮箱
 * @param {string} passwordData.code - 验证码
 * @param {string} passwordData.newPassword - 新密码
 */
export const changePassword = (passwordData) => {
  return httpInstance({
    url: '/user/changepassword',
    method: 'post',
    headers: authHeaders,
    data: passwordData
  });
};

/**
 * 上传用户头像
 * 接口文档对应：POST /user/uploadavatar
 * @param {FormData} formData - 包含头像文件的表单数据
 */
export const uploadAvatar = (formData) => {
  return httpInstance({
    url: '/user/uploadavatar',
    method: 'post',
    headers: {
      ...authHeaders,
      'Content-Type': 'multipart/form-data' // 上传文件需指定该类型
    },
    data: formData
  });
};

/**
 * 发送验证码（用于修改密码时验证身份）
 * 接口文档对应：POST /user/sendcode
 * @param {string} email - 接收验证码的邮箱
 */
export const sendVerificationCode = (email) => {
  return httpInstance({
    url: '/user/sendcode',
    method: 'post',
    params: { email } // 接口要求email为query参数
  });
};
