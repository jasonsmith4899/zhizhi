const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    isLoggedIn: false,
    userId: '',
    loading: true,
    knowledgeBases: [],
    selectedKbId: null
  },

  onLoad() {
    this.initLogin()
  },

  onShow() {
    this.checkLoginStatus()
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userId = wx.getStorageSync('userId')
    this.setData({
      isLoggedIn: !!token,
      userId: userId || ''
    })
  },

  async initLogin() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      this.checkLoginStatus()
      await this.loadKnowledgeBases()
    } catch (err) {
      console.error('登录失败:', err)
      wx.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadKnowledgeBases() {
    try {
      const res = await request({ url: '/api/mp/knowledge-bases' })
      const kbs = res.data || res || []
      this.setData({ knowledgeBases: kbs })
      // 如果只有一个知识库，自动选中
      if (kbs.length === 1) {
        this.setData({ selectedKbId: kbs[0].id })
      }
    } catch (err) {
      console.error('加载知识库失败:', err)
    }
  },

  onSelectKb(e) {
    const kbId = e.currentTarget.dataset.id
    this.setData({ selectedKbId: kbId })
  },

  startChat() {
    if (!this.data.isLoggedIn) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      this.initLogin()
      return
    }
    if (!this.data.selectedKbId) {
      wx.showToast({ title: '请先选择知识库', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: '/pages/chat/chat?knowledgeBaseId=' + this.data.selectedKbId
    })
  },

  goHistory() {
    wx.switchTab({
      url: '/pages/history/history'
    })
  },

  onPullDownRefresh() {
    this.initLogin().finally(() => {
      wx.stopPullDownRefresh()
    })
  }
})
