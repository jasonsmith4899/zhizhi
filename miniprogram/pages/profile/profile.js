const { logout, getUserId, ensureLogin } = require('../../utils/auth')

Page({
  data: {
    userId: '',
    isLoggedIn: false
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.checkLogin()
  },

  // 检查登录状态
  async checkLogin() {
    try {
      await ensureLogin()
      const userId = getUserId()
      this.setData({
        userId: userId,
        isLoggedIn: !!userId
      })
    } catch (err) {
      console.error('登录检查失败:', err)
      this.setData({
        isLoggedIn: false,
        userId: ''
      })
    }
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          logout()
          this.setData({
            isLoggedIn: false,
            userId: ''
          })
          wx.showToast({ title: '已退出登录', icon: 'success' })
          // 跳转到首页
          setTimeout(() => {
            wx.switchTab({
              url: '/pages/index/index'
            })
          }, 1500)
        }
      }
    })
  },

  // 重新登录
  async handleRelogin() {
    try {
      await ensureLogin()
      const userId = getUserId()
      this.setData({
        userId: userId,
        isLoggedIn: !!userId
      })
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (err) {
      wx.showToast({ title: '登录失败', icon: 'none' })
    }
  }
})
