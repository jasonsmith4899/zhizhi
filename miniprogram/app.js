const { login } = require('./utils/auth')

App({
  globalData: {
    userInfo: null,
    token: null,
    apiBaseUrl: 'http://localhost:8080'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  },

  // 全局登录方法，委托给auth模块
  login() {
    return login().then((data) => {
      this.globalData.token = data.token
      return data
    })
  }
})
