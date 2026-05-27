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
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
    <el-card style="width: 420px; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3)">
      <div style="text-align: center; margin-bottom: 32px">
        <h1 style="font-size: 28px; color: #303133; margin-bottom: 8px">智知 AI知识库</h1>
        <p style="color: #909399">{{ isRegister ? '创建您的商户账户' : '登录商户管理后台' }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item v-if="isRegister" label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="handleSubmit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div style="text-align: center">
        <el-link type="primary" @click="isRegister = !isRegister">
          {{ isRegister ? '已有账户？去登录' : '没有账户？去注册' }}
        </el-link>
      </div>
    </el-card>
  </div>
</template>
