<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const isRegister = ref(false)
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
}

const formRef = ref()

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isRegister.value) {
      await authStore.register(form.username, form.email, form.password)
      ElMessage.success('注册成功')
    } else {
      await authStore.login(form.username, form.password)
      ElMessage.success('登录成功')
    }
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 背景粒子效果 -->
    <div class="particles">
      <div v-for="i in 50" :key="i" class="particle" :style="{
        left: `${Math.random() * 100}%`,
        top: `${Math.random() * 100}%`,
        animationDelay: `${Math.random() * 5}s`,
        animationDuration: `${5 + Math.random() * 10}s`
      }"></div>
    </div>

    <!-- 网格背景 -->
    <div class="grid-overlay"></div>

    <!-- 主登录卡片 -->
    <div class="login-container">
      <!-- 左侧装饰 -->
      <div class="login-decoration">
        <div class="decoration-content">
          <div class="logo-container">
            <div class="logo-glow"></div>
            <h1 class="logo-text">智知</h1>
            <div class="logo-subtitle">ZhiZhi AI</div>
          </div>
          <div class="tagline">
            <div class="tagline-line">智能知识库</div>
            <div class="tagline-line">未来已来</div>
          </div>
          <div class="decoration-circles">
            <div class="circle circle-1"></div>
            <div class="circle circle-2"></div>
            <div class="circle circle-3"></div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-container">
        <div class="form-header">
          <h2>{{ isRegister ? '创建账户' : '欢迎回来' }}</h2>
          <p>{{ isRegister ? '加入智知，开启智能知识管理' : '登录您的智知账户' }}</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item v-if="isRegister" label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              :prefix-icon="Message"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="submit-btn"
              @click="handleSubmit"
            >
              <span class="btn-text">{{ isRegister ? '注册' : '登录' }}</span>
              <div class="btn-glow"></div>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <el-link type="primary" @click="isRegister = !isRegister">
            {{ isRegister ? '已有账户？去登录' : '没有账户？去注册' }}
          </el-link>
        </div>

        <!-- 装饰性元素 -->
        <div class="form-decoration">
          <div class="hex hex-1"></div>
          <div class="hex hex-2"></div>
          <div class="hex hex-3"></div>
        </div>
      </div>
    </div>

    <!-- 底部装饰线 -->
    <div class="bottom-decoration">
      <div class="line line-1"></div>
      <div class="line line-2"></div>
      <div class="line line-3"></div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-dark);
  position: relative;
  overflow: hidden;
}

/* 粒子效果 */
.particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 2px;
  height: 2px;
  background: var(--color-neon-blue);
  border-radius: 50%;
  animation: float-particle linear infinite;
  opacity: 0;
}

@keyframes float-particle {
  0% {
    opacity: 0;
    transform: translateY(0) translateX(0);
  }
  10% {
    opacity: 0.8;
  }
  90% {
    opacity: 0.8;
  }
  100% {
    opacity: 0;
    transform: translateY(-100vh) translateX(100px);
  }
}

/* 网格背景 */
.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(var(--overlay-primary-05) 1px, transparent 1px),
    linear-gradient(90deg, var(--overlay-primary-05) 1px, transparent 1px);
  background-size: 30px 30px;
  pointer-events: none;
}

/* 登录容器 */
.login-container {
  display: flex;
  width: 900px;
  min-height: 520px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 0 25px 80px var(--overlay-black-50);
  border: 1px solid var(--border-color);
  position: relative;
  z-index: 10;
  animation: fade-in-up 0.8s ease-out;
}

/* 左侧装饰 */
.login-decoration {
  flex: 1;
  background: linear-gradient(135deg, var(--color-deep-space) 0%, var(--color-stellar-blue) 100%);
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-decoration::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, var(--overlay-neon-10) 0%, transparent 50%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.decoration-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.logo-container {
  position: relative;
  margin-bottom: 40px;
}

.logo-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse-glow 3s ease-in-out infinite;
}

.logo-text {
  font-family: 'Orbitron', monospace;
  font-size: 48px;
  font-weight: 800;
  color: var(--text-primary);
  text-shadow: 0 0 30px var(--color-neon-blue-glow);
  position: relative;
  z-index: 1;
}

.logo-subtitle {
  font-family: 'Rajdhani', sans-serif;
  font-size: 18px;
  font-weight: 300;
  color: var(--color-neon-blue);
  letter-spacing: 8px;
  text-transform: uppercase;
  margin-top: 10px;
}

.tagline {
  margin-top: 40px;
}

.tagline-line {
  font-family: 'Rajdhani', sans-serif;
  font-size: 24px;
  font-weight: 300;
  color: var(--text-secondary);
  letter-spacing: 4px;
  margin-bottom: 10px;
}

.decoration-circles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border: 1px solid var(--overlay-neon-20);
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: -50px;
  right: -50px;
  animation-delay: 0s;
}

.circle-2 {
  width: 150px;
  height: 150px;
  bottom: -30px;
  left: -30px;
  animation-delay: 2s;
}

.circle-3 {
  width: 100px;
  height: 100px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 4s;
}

/* 右侧表单 */
.login-form-container {
  flex: 1;
  background: var(--bg-card);
  backdrop-filter: blur(30px);
  padding: 60px 50px;
  position: relative;
  overflow: hidden;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-family: 'Rajdhani', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.form-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-form {
  position: relative;
  z-index: 1;
}

.login-form :deep(.el-form-item__label) {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.login-form :deep(.el-input__wrapper) {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: none;
  transition: all var(--transition-normal);
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--color-primary);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-neon-blue);
  box-shadow: 0 0 0 3px var(--overlay-neon-10);
}

.login-form :deep(.el-input__inner) {
  color: var(--text-primary);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: var(--text-muted);
}

.login-form :deep(.el-input__prefix) {
  color: var(--text-secondary);
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  border-radius: var(--radius-md);
}

.submit-btn:hover {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-primary) 100%);
  box-shadow: 0 0 30px var(--overlay-primary-40);
  transform: translateY(-2px);
}

.btn-text {
  position: relative;
  z-index: 1;
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--overlay-white-20), transparent);
  transition: left 0.5s ease;
}

.submit-btn:hover .btn-glow {
  left: 100%;
}

.form-footer {
  text-align: center;
  margin-top: 30px;
  position: relative;
  z-index: 1;
}

/* 表单装饰 */
.form-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.hex {
  position: absolute;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, var(--overlay-primary-10), var(--overlay-neon-05));
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  animation: float 8s ease-in-out infinite;
}

.hex-1 {
  top: 20px;
  right: 20px;
  animation-delay: 0s;
}

.hex-2 {
  bottom: 40px;
  right: 60px;
  animation-delay: 2s;
}

.hex-3 {
  top: 50%;
  left: 20px;
  animation-delay: 4s;
}

/* 底部装饰 */
.bottom-decoration {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
}

.line {
  position: absolute;
  bottom: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-neon-blue), transparent);
  animation: line-move 3s ease-in-out infinite;
}

.line-1 {
  width: 100px;
  left: -100px;
  animation-delay: 0s;
}

.line-2 {
  width: 150px;
  left: -150px;
  animation-delay: 1s;
}

.line-3 {
  width: 80px;
  left: -80px;
  animation-delay: 2s;
}

@keyframes line-move {
  0% {
    transform: translateX(0);
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: translateX(calc(100vw + 200px));
    opacity: 0;
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 90%;
    min-height: auto;
  }

  .login-decoration {
    padding: 40px 20px;
  }

  .login-form-container {
    padding: 40px 30px;
  }
}
</style>
