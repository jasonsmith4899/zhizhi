const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    isLoggedIn: false,
    userId: '',
    loading: true
  },

  onLoad() {
    this.initLogin()
  },

  onShow() {
    // 每次显示页面时检查登录态
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
    } catch (err) {
      console.error('登录失败:', err)
      wx.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 开始新对话
  startChat() {
    if (!this.data.isLoggedIn) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      this.initLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/chat/chat'
    })
  },

  // 跳转到历史对话
  goHistory() {
    wx.switchTab({
      url: '/pages/history/history'
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.initLogin().finally(() => {
      wx.stopPullDownRefresh()
    })
  }
})
