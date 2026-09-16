<template>
  <div class="auth-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="particles-container"></div>
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 主卡片容器 -->
    <div class="auth-card">
      <!-- 切换标签 -->
      <div class="auth-tabs">
        <div
          class="tab-item"
          :class="{ active: isLogin }"
          @click="switchToLogin"
        >
          登录
        </div>
        <div
          class="tab-item"
          :class="{ active: !isLogin }"
          @click="switchToRegister"
        >
          注册
        </div>
      </div>

      <!-- 登录表单 -->
      <div class="form-container" v-if="isLogin">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">请输入您的邮箱和密码</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="0px"
          class="auth-form"
        >
          <el-form-item prop="email">
            <el-input
              v-model="loginForm.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
              class="auth-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              class="auth-input"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe" class="remember-checkbox">
              记住我
            </el-checkbox>
            <a href="#" class="forgot-password" @click.prevent="handleForgotPassword">忘记密码?</a>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              class="auth-button"
              @click="handleLogin"
              :loading="isLoading"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 注册表单 -->
      <div class="form-container" v-else>
        <h2 class="form-title">创建新账号</h2>
        <p class="form-subtitle">填写以下信息完成注册</p>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-width="0px"
          class="auth-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              class="auth-input"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
              class="auth-input"
            />
          </el-form-item>

          <el-form-item prop="verificationCode">
            <el-row :gutter="10">
              <el-col :span="16">
                <el-input
                  v-model="registerForm.verificationCode"
                  placeholder="请输入6位验证码"
                  prefix-icon="Key"
                  class="auth-input"
                />
              </el-col>
              <el-col :span="8">
                <el-button
                  type="default"
                  class="code-button"
                  @click="sendVerificationCode"
                  :disabled="isSendingCode || !registerForm.email"
                >
                  {{ codeButtonText }}
                </el-button>
              </el-col>
            </el-row>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              placeholder="请输入密码 (至少8位)"
              prefix-icon="Lock"
              show-password
              class="auth-input"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              placeholder="确认密码"
              prefix-icon="Check"
              show-password
              class="auth-input"
            />
          </el-form-item>

          <el-form-item prop="agreement">
            <el-checkbox v-model="registerForm.agreement">
              我已阅读并同意<a href="#" class="agreement-link">用户协议</a>和<a href="#" class="agreement-link">隐私政策</a>
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="auth-button"
              @click="handleRegister"
              :loading="isLoading"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog
      v-model="forgotPasswordVisible"
      title="找回密码"
      width="30%"
      :before-close="handleCloseForgotDialog"
    >
      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-width="80px"
        class="forgot-form"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="forgotForm.email" placeholder="请输入您的注册邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="verificationCode">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-input
                v-model="forgotForm.verificationCode"
                placeholder="请输入6位验证码"
                class="auth-input"
              />
            </el-col>
            <el-col :span="8">
              <el-button
                type="default"
                @click="sendForgotCode"
                :disabled="isSendingForgotCode || !forgotForm.email"
              >
                {{ forgotCodeButtonText }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            placeholder="请输入新密码"
            show-password
            class="auth-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseForgotDialog">取消</el-button>
        <el-button type="primary" @click="handleSendResetEmail">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
// 引入API接口
import { registerEmailCodeAPI, registerAPI } from '@/apis/registerAPI.js'
import {
  loginEmailCodeAPI,
  passwordLoginAPI,
  emailLoginAPI,
  changePasswordAPI,
  forgotPasswordCodeAPI  // 引入忘记密码验证码接口
} from '@/apis/loginAPI.js'

// 路由实例
const router = useRouter()

// 状态变量
const isLogin = ref(true)
const isLoading = ref(false)
const rememberMe = ref(true)
const forgotPasswordVisible = ref(false)
const isSendingCode = ref(false)
const isSendingForgotCode = ref(false)
const codeButtonText = ref('获取验证码')
const forgotCodeButtonText = ref('获取验证码')
const countdown = ref(60)
const forgotCountdown = ref(60)

// 表单引用
const loginFormRef = ref()
const registerFormRef = ref()
const forgotFormRef = ref()

// 登录表单数据
const loginForm = reactive({
  email: '',
  password: ''
})

// 注册表单数据
const registerForm = reactive({
  username: '',
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  agreement: false
})

// 忘记密码表单数据
const forgotForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: ''
})

// 登录表单验证规则
const loginRules = reactive({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ]
})

// 注册表单验证规则
const registerRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在2-20个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  agreement: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请同意用户协议和隐私政策'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
})

// 忘记密码验证规则
const forgotRules = reactive({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ]
})

// 切换到登录
const switchToLogin = () => {
  isLogin.value = true
  const formContainer = document.querySelector('.form-container')
  if (formContainer) {
    formContainer.classList.add('slide-out')
    setTimeout(() => {
      formContainer.classList.remove('slide-out')
    }, 300)
  }
}

// 切换到注册
const switchToRegister = () => {
  isLogin.value = false
  const formContainer = document.querySelector('.form-container')
  if (formContainer) {
    formContainer.classList.add('slide-out')
    setTimeout(() => {
      formContainer.classList.remove('slide-out')
    }, 300)
  }
}

// 处理登录
const handleLogin = async () => {
  try {
    isLoading.value = true
    // 先验证表单
    await loginFormRef.value.validate()

    // 调用密码登录API
    const res = await passwordLoginAPI(loginForm.email, loginForm.password)

    // 检查响应是否有效
    if (!res || !res.data) {
      throw new Error('服务器返回格式错误')
    }

    // 检查后端返回的成功标识
    if (res.data.code === 1 && res.data.message === 'success') {
      ElMessage.success('登录成功')
      const { token, sessionId, userType, role } = res.data.data;
      const activeSessionId = sessionId || token;

      if (!activeSessionId) {
        throw new Error('未获取到会话')
      }

      localStorage.setItem('sessionId', activeSessionId)
      localStorage.removeItem('token')
      localStorage.setItem('userInfo', JSON.stringify(res.data.data));

      // 根据用户类型跳转
      if (['会议室管理员', '超级管理员'].includes(userType)
        || ['room_admin', 'super_admin'].includes(role)) {
        await router.push('/manager')
      } else if (userType === '普通用户') {
        await router.push('/user/home')
      } else {
        // 未知用户类型处理
        await router.push('/')
      }
    } else {
      // 后端明确返回失败
      const errorMsg = res.data?.message || '登录失败'
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    console.error('登录过程发生异常:', error)
    let errorMsg = '登录过程出错，请重试'

    if (error.name === 'ValidationError') {
      errorMsg = '表单验证失败，请检查输入'
    } else if (error.response) {
      // 服务器返回错误
      errorMsg = error.response.data?.message || errorMsg
    } else if (error.message) {
      errorMsg = error.message
    }

    ElMessage.error(errorMsg)
  } finally {
    isLoading.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  try {
    isLoading.value = true
    await registerFormRef.value.validate()

    // 调用注册API
    const res = await registerAPI(
      registerForm.username,
      registerForm.email,
      registerForm.verificationCode,
      registerForm.password
    )

    // 检查响应有效性
    if (!res || !res.data) {
      throw new Error('服务器返回格式错误')
    }

    // 按接口文档判断成功
    if (res.data.code === 1 && res.data.message === 'success') {
      ElMessage.success('注册成功，请登录')
      setTimeout(() => {
        isLogin.value = true
        loginForm.email = registerForm.email
        // 清空注册表单
        registerForm.username = ''
        registerForm.verificationCode = ''
        registerForm.password = ''
        registerForm.confirmPassword = ''
        registerForm.agreement = false
        registerFormRef.value?.clearValidate()
      }, 1000)
    } else {
      const errorMsg = res.data?.message || '注册失败'
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    console.error('注册过程发生异常:', error)
    let errorMsg = '注册过程出错，请重试'

    if (error.name === 'ValidationError') {
      errorMsg = '表单验证失败，请检查输入'
    } else if (error.response) {
      errorMsg = error.response.data?.message || errorMsg
    }

    ElMessage.error(errorMsg)
  } finally {
    isLoading.value = false
  }
}

// 发送注册验证码
const sendVerificationCode = async () => {
  // 验证邮箱格式
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(registerForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  try {
    isSendingCode.value = true
    codeButtonText.value = '发送中...'

    const res = await registerEmailCodeAPI(registerForm.email)

    // 检查响应有效性
    if (!res || !res.data) {
      throw new Error('服务器返回格式错误')
    }

    // 按接口文档判断成功
    if (res.data.code === 1 && res.data.message === 'success') {
      ElMessage.success('验证码已发送，请注意查收')

      // 开始倒计时
      const timer = setInterval(() => {
        countdown.value--
        codeButtonText.value = `${countdown.value}s后重发`

        if (countdown.value <= 0) {
          clearInterval(timer)
          isSendingCode.value = false
          codeButtonText.value = '获取验证码'
          countdown.value = 60
        }
      }, 1000)
    } else {
      const errorMsg = res.data?.message || '验证码发送失败'
      ElMessage.error(errorMsg)
      isSendingCode.value = false
      codeButtonText.value = '获取验证码'
    }
  } catch (error) {
    console.error('发送验证码异常:', error)
    ElMessage.error('发送验证码失败，请重试')
    isSendingCode.value = false
    codeButtonText.value = '获取验证码'
  }
}

// 发送忘记密码验证码
const sendForgotCode = async () => {
  // 验证邮箱格式
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(forgotForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  try {
    isSendingForgotCode.value = true
    forgotCodeButtonText.value = '发送中...'

    // 使用专门的忘记密码验证码接口
    const res = await forgotPasswordCodeAPI(forgotForm.email)

    // 检查响应有效性
    if (!res || !res.data) {
      throw new Error('服务器返回格式错误')
    }

    // 按接口文档判断成功
    if (res.data.code === 1 && res.data.message === 'success') {
      ElMessage.success('验证码已发送，请注意查收')

      // 开始倒计时
      const timer = setInterval(() => {
        forgotCountdown.value--
        forgotCodeButtonText.value = `${forgotCountdown.value}s后重发`

        if (forgotCountdown.value <= 0) {
          clearInterval(timer)
          isSendingForgotCode.value = false
          forgotCodeButtonText.value = '获取验证码'
          forgotCountdown.value = 60
        }
      }, 1000)
    } else {
      const errorMsg = res.data?.message || '验证码发送失败'
      ElMessage.error(errorMsg)
      isSendingForgotCode.value = false
      forgotCodeButtonText.value = '获取验证码'
    }
  } catch (error) {
    console.error('发送验证码异常:', error)
    ElMessage.error('发送验证码失败，请重试')
    isSendingForgotCode.value = false
    forgotCodeButtonText.value = '获取验证码'
  }
}

// 处理忘记密码
const handleForgotPassword = () => {
  forgotPasswordVisible.value = true
  forgotForm.email = ''
  forgotForm.verificationCode = ''
  forgotForm.newPassword = ''
  if (forgotFormRef.value) {
    forgotFormRef.value.clearValidate()
  }
}

// 关闭忘记密码对话框
const handleCloseForgotDialog = () => {
  forgotPasswordVisible.value = false
}

// 发送密码重置请求
const handleSendResetEmail = async () => {
  try {
    await forgotFormRef.value.validate()

    // 调用修改密码API
    const res = await changePasswordAPI({
      email: forgotForm.email,
      code: forgotForm.verificationCode,
      newPassword: forgotForm.newPassword
    })

    // 检查响应有效性
    if (!res || !res.data) {
      throw new Error('服务器返回格式错误')
    }

    // 按接口文档判断成功
    if (res.data.code === 1 && res.data.message === 'success') {
      ElMessage.success('密码修改成功，请使用新密码登录')
      forgotPasswordVisible.value = false
      isLogin.value = true
      loginForm.email = forgotForm.email
    } else {
      const errorMsg = res.data?.message || '密码修改失败'
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    console.error('修改密码异常:', error)
    let errorMsg = '操作失败，请重试'

    if (error.name === 'ValidationError') {
      errorMsg = '表单验证失败，请检查输入'
    } else if (error.response) {
      errorMsg = error.response.data?.message || errorMsg
    }

    ElMessage.error(errorMsg)
  }
}

// 初始化
onMounted(() => {
  createBackgroundParticles();
})

// 创建背景动态粒子
const createBackgroundParticles = () => {
  const container = document.querySelector('.particles-container');
  if (!container) return;

  const particleCount = 25;
  const containerWidth = window.innerWidth;
  const containerHeight = window.innerHeight;

  for (let i = 0; i < particleCount; i++) {
    const particle = document.createElement('div');
    particle.classList.add('particle');

    const size = Math.random() * 2 + 1;
    particle.style.width = `${size}px`;
    particle.style.height = `${size}px`;

    particle.style.left = `${Math.random() * 100}%`;
    particle.style.top = `${Math.random() * 100}%`;

    const durationX = Math.random() * 40 + 20;
    const durationY = Math.random() * 40 + 20;
    const delayX = Math.random() * 10;
    const delayY = Math.random() * 10;

    particle.style.animation = `
      floatX ${durationX}s ease-in-out ${delayX}s infinite alternate,
      floatY ${durationY}s ease-in-out ${delayY}s infinite alternate
    `;

    container.appendChild(particle);
  }
}
</script>

<style scoped>
/* 卡通风格登录页面样式 */
.auth-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
  animation: backgroundFloat 20s ease-in-out infinite;
}

@keyframes backgroundFloat {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.particles-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  background-color: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  opacity: 0.8;
  transition: all 3s ease-in-out;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
}

@keyframes floatX {
  from { transform: translateX(-50px) rotate(0deg); }
  to { transform: translateX(50px) rotate(360deg); }
}

@keyframes floatY {
  from { transform: translateY(-50px); }
  to { transform: translateY(50px); }
}

/* 卡通风格装饰圆形 */
.circle {
  position: absolute;
  border-radius: 50%;
  animation: float 8s ease-in-out infinite;
  filter: blur(2px);
}

.circle-1 {
  width: 450px;
  height: 450px;
  top: -120px;
  left: -120px;
  background: radial-gradient(circle, rgba(255, 209, 102, 0.3) 0%, rgba(255, 209, 102, 0.1) 70%);
  animation-delay: 0s;
  animation-duration: 10s;
  box-shadow: 0 0 60px rgba(255, 209, 102, 0.4);
}

.circle-2 {
  width: 350px;
  height: 350px;
  bottom: -180px;
  right: -180px;
  background: radial-gradient(circle, rgba(155, 93, 229, 0.3) 0%, rgba(155, 93, 229, 0.1) 70%);
  animation-delay: 2s;
  animation-duration: 12s;
  box-shadow: 0 0 60px rgba(155, 93, 229, 0.4);
}

.circle-3 {
  width: 250px;
  height: 250px;
  top: 50%;
  right: 10%;
  transform: translateY(-50%);
  background: radial-gradient(circle, rgba(107, 203, 119, 0.3) 0%, rgba(107, 203, 119, 0.1) 70%);
  animation-delay: 4s;
  animation-duration: 8s;
  box-shadow: 0 0 60px rgba(107, 203, 119, 0.4);
}

@keyframes float {
  0% {
    transform: translateY(0px) rotate(0deg) scale(1);
    opacity: 0.7;
  }
  50% {
    transform: translateY(-300px) rotate(5deg) scale(1.2);
    opacity: 0.9;
  }
  100% {
    transform: translateY(100px) rotate(0deg) scale(1);
    opacity: 0.7;
  }
}

/* 增强版3D玻璃卡片效果 */
.auth-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  z-index: 10;
  transition: all 0.5s ease;
  animation: cardAppear 0.8s ease-out;
  transform-style: preserve-3d;
  perspective: 1000px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  box-shadow: 
    0 15px 40px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset 0 -1px 0 rgba(0, 0, 0, 0.05);
  &:before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: 24px;
    background: linear-gradient(145deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.2));
    transform: translateZ(5px);
    pointer-events: none;
  }
  &:hover {
    transform: translateY(-10px) rotateX(5deg) translateZ(15px);
    box-shadow: 
      0 20px 50px rgba(0, 0, 0, 0.2),
      0 0 0 1px rgba(255, 255, 255, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.9),
      inset 0 -1px 0 rgba(0, 0, 0, 0.08);
  }
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(50px) scale(0.9) rotateY(-15deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1) rotateY(0deg);
  }
}

.auth-tabs {
  display: flex;
  height: 70px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 24px 24px 0 0;
  padding: 10px;
  gap: 10px;
}

.tab-item {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform-style: preserve-3d;
}

.tab-item.active {
  color: rgb(179, 219, 225);
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px) translateZ(5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -11px;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 4px;
  background: linear-gradient(90deg, rgb(179, 219, 225), rgba(159, 199, 205, 1));
  border-radius: 4px;
  animation: tabIndicator 0.3s ease-out;
}

@keyframes tabIndicator {
  from {
    width: 0;
    left: 50%;
  }
  to {
    width: 80%;
    left: 50%;
  }
}

.tab-item:hover {
  color: rgb(179, 219, 225);
  transform: translateY(-3px) translateZ(5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.form-container {
  padding: 30px 40px;
  transition: all 0.5s ease;
}

.slide-out {
  animation: slideOut 0.3s ease-out;
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0) rotateY(0deg);
  }
  to {
    opacity: 0;
    transform: translateX(-30px) rotateY(-5deg);
  }
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: rgb(179, 219, 225);
  margin-bottom: 12px;
  text-align: center;
  position: relative;
  overflow: hidden;
  text-shadow: 2px 2px 4px rgba(255, 255, 255, 0.8);
}

.form-title::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 4px;
  background: linear-gradient(90deg, rgb(179, 219, 225), rgba(159, 199, 205, 1));
  border-radius: 4px;
  animation: titleUnderline 1s ease-out;
}

@keyframes titleUnderline {
  from {
    width: 0;
  }
  to {
    width: 80px;
  }
}

.form-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
  text-align: center;
  animation: subtitleFade 1s ease-out 0.3s both;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
}

@keyframes subtitleFade {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-form, .forgot-form {
  width: 100%;
}

.auth-input {
  height: 55px;
  border-radius: 16px;
  font-size: 16px;
  animation: inputAppear 0.5s ease-out;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 5px 15px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    inset 0 -1px 0 rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
  &:hover {
    border-color: rgb(179, 219, 225);
    transform: translateY(-2px) translateZ(5px);
    box-shadow: 
      0 8px 20px rgba(0, 0, 0, 0.12),
      0 0 0 1px rgba(0, 0, 0, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.95),
      inset 0 -1px 0 rgba(0, 0, 0, 0.05);
  }
  &:focus {
    border-color: rgb(179, 219, 225);
    box-shadow: 
      0 8px 20px rgba(0, 0, 0, 0.12),
      0 0 0 2px rgb(179, 219, 225),
      inset 0 1px 0 rgba(255, 255, 255, 0.95),
      inset 0 -1px 0 rgba(0, 0, 0, 0.05);
    transform: translateY(-3px) translateZ(8px);
  }
}

.code-button {
  height: 55px;
  width: 100%;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.6);
  color: #606266;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 
    0 5px 15px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transform-style: preserve-3d;
  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.5);
    border-color: rgb(179, 219, 225);
    color: rgb(179, 219, 225);
    transform: translateY(-3px) translateZ(8px);
    box-shadow: 
      0 8px 20px rgba(0, 0, 0, 0.12),
      0 0 0 1px rgba(179, 219, 225, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.7);
  }
  &:disabled {
    background: rgba(255, 255, 255, 0.2);
    color: #c0c4cc;
    cursor: not-allowed;
    opacity: 0.7;
  }
}

@keyframes inputAppear {
  from {
    opacity: 0;
    transform: translateY(15px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  font-size: 14px;
  animation: optionsAppear 0.5s ease-out 0.5s both;
}

@keyframes optionsAppear {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.remember-checkbox .el-checkbox__label {
  color: #666;
  font-weight: 500;
}

.forgot-password {
  color: $primaryColor;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  cursor: pointer;
  font-weight: 600;
  &:hover {
    color: $accentColor;
    transform: translateY(-2px);
  }
}

.auth-button {
  width: 100%;
  height: 55px;
  border-radius: 16px;
  font-size: 18px;
  font-weight: 700;
  background: rgba(179, 219, 225, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.6);
  color: #333;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: buttonAppear 0.5s ease-out 0.7s both;
  box-shadow: 
    0 8px 20px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(179, 219, 225, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset 0 -1px 0 rgba(0, 0, 0, 0.05);
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transform-style: preserve-3d;
  &:hover {
    background: rgba(179, 219, 225, 1);
    transform: translateY(-5px) scale(1.03) translateZ(10px);
    box-shadow: 
      0 12px 30px rgba(0, 0, 0, 0.18),
      0 0 0 1px rgba(179, 219, 225, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.9),
      inset 0 -1px 0 rgba(0, 0, 0, 0.08);
  }
  &:active {
    transform: translateY(-2px) scale(0.98) translateZ(5px);
  }
}

@keyframes buttonAppear {
  from {
    opacity: 0;
    transform: translateY(15px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.agreement-link {
  color: $primaryColor;
  text-decoration: none;
  position: relative;
  font-weight: 600;
  transition: all 0.3s ease;
  &:hover {
    color: $accentColor;
    transform: translateY(-2px);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .form-container {
    padding: 25px 20px;
  }

  .auth-input {
    height: 50px;
  }

  .code-button, .auth-button {
    height: 50px;
  }

  .form-title {
    font-size: 24px;
  }
}

/* 对话框玻璃效果 */
:deep(.el-dialog) {
  @extend .glass-effect;
  border-radius: $radiusLarge;
  .el-dialog__header {
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  }
  .el-dialog__title {
    color: $primaryColor;
    font-weight: 700;
  }
}
</style>
