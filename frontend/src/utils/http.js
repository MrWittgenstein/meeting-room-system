// axios基础的封装
import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'

const httpInstance = axios.create({
  baseURL: 'http://8.141.97.91:8080',
  timeout: 5000
})
// 请求拦截器
httpInstance.interceptors.request.use(config => {
  // 在发送请求之前做些什么

  const token = localStorage.getItem('token');
  // 如果token存在，添加到请求头
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`; // 注意Bearer后的空格
  }
  return config
}, error => {
  // 对请求错误做些什么
  return Promise.reject(error)
})

// 响应拦截器
httpInstance.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    // 处理401未授权错误（通常表示token无效或过期）
    if (error.response && error.response.status === 401) {
      // 清除本地存储的token
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      // 跳转到登录页
      router.push('/');
      // 提示用户重新登录
      ElMessage.error('登录已过期，请重新登录');
    }
    return Promise.reject(error);
  }
);

export default httpInstance
