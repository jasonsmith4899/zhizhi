const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    messages: [],
    inputText: '',
    sessionId: '',
    loading: false,
    scrollToView: '',
    showSources: {},
    knowledgeBaseId: null
  },

  onLoad(options) {
    this.scrollTop = 0
    this.messageCount = 0

    // 接收知识库ID
    if (options.knowledgeBaseId) {
      this.setData({ knowledgeBaseId: Number(options.knowledgeBaseId) })
    }

    // 如果传入sessionId则加载历史对话
    if (options.sessionId) {
      this.setData({ sessionId: options.sessionId })
      this.loadHistory(options.sessionId)
    } else {
      // 新对话，生成sessionId
      this.setData({
        sessionId: this.generateSessionId()
      })
    }
  },

  onShow() {
    this.ensureLoginStatus()
  },

  // 确保登录态
  async ensureLoginStatus() {
    try {
      await ensureLogin()
    } catch (err) {
      console.error('登录失败:', err)
      wx.showToast({ title: '登录失败', icon: 'none' })
    }
  },

  // 生成sessionId
  generateSessionId() {
    return 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  },

  // 加载历史消息
  async loadHistory(sessionId) {
    wx.showLoading({ title: '加载中...' })
    try {
      const messages = await request({
        url: `/api/mp/conversations/${sessionId}/messages`
      })
      // 格式化消息
      const formattedMessages = messages.map(msg => ({
        id: msg.id,
        role: msg.role,
        content: msg.content,
        sources: msg.sources || [],
        createdAt: msg.createdAt,
        showSources: false
      }))
      this.setData({ messages: formattedMessages })
      this.scrollToBottom()
    } catch (err) {
      console.error('加载历史失败:', err)
      wx.showToast({ title: '加载历史失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  // 输入框内容变化
  onInputChange(e) {
    this.setData({ inputText: e.detail.value })
  },

  // 发送消息
  async sendMessage() {
    const text = this.data.inputText.trim()
    if (!text || this.data.loading) return

    if (!this.data.knowledgeBaseId) {
      wx.showToast({ title: '请先选择知识库', icon: 'none' })
      return
    }

    // 添加用户消息
    const userMsg = {
      id: 'user_' + Date.now(),
      role: 'user',
      content: text,
      createdAt: new Date().toISOString()
    }

    this.setData({
      messages: [...this.data.messages, userMsg],
      inputText: '',
      loading: true
    })

    this.scrollToBottom()

    try {
      const res = await request({
        url: '/api/mp/chat',
        method: 'POST',
        data: {
          message: text,
          knowledgeBaseId: this.data.knowledgeBaseId,
          sessionId: this.data.sessionId
        }
      })

      // 添加AI回复
      const aiMsg = {
        id: res.messageId || 'ai_' + Date.now(),
        role: 'assistant',
        content: res.reply,
        sources: res.sources || [],
        createdAt: new Date().toISOString(),
        showSources: false
      }

      // 更新sessionId（如果是新对话，后端会返回sessionId）
      const sessionId = res.sessionId || this.data.sessionId

      this.setData({
        messages: [...this.data.messages, aiMsg],
        sessionId: sessionId
      })

      this.scrollToBottom()
    } catch (err) {
      console.error('发送失败:', err)
      // 添加错误提示消息
      const errorMsg = {
        id: 'err_' + Date.now(),
        role: 'assistant',
        content: '抱歉，消息发送失败，请重试。',
        sources: [],
        createdAt: new Date().toISOString(),
        isError: true
      }
      this.setData({
        messages: [...this.data.messages, errorMsg]
      })
      wx.showToast({ title: '发送失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
      this.scrollToBottom()
    }
  },

  // 滚动到底部
  scrollToBottom() {
    this.messageCount++
    const viewId = 'msg-' + (this.data.messages.length - 1)
    this.setData({
      scrollToView: viewId
    })
  },

  // 切换sources显示
  toggleSources(e) {
    const idx = e.currentTarget.dataset.idx
    const key = 'messages[' + idx + '].showSources'
    const currentVal = this.data.messages[idx].showSources
    this.setData({
      [key]: !currentVal
    })
  },

  // 键盘确认发送
  onConfirmSend(e) {
    this.setData({ inputText: e.detail.value })
    this.sendMessage()
  },

  // 阻止页面滚动（解决scroll-view问题）
  preventScroll() {
    return false
  }
})
