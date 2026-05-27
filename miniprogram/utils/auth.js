const { request } = require('./request')

// 检查是否已登录
function isLoggedIn() {
  return !!wx.getStorageSync('token')
}

// 获取token
function getToken() {
  return wx.getStorageSync('token') || ''
}

// 获取userId
function getUserId() {
  return wx.getStorageSync('userId') || ''
}

// 登录
function login() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          request({
            url: '/api/mp/login',
            method: 'POST',
            data: { code: res.code },
            noAuth: true
          }).then((data) => {
            wx.setStorageSync('token', data.token)
            wx.setStorageSync('userId', data.userId)
            resolve(data)
          }).catch(reject)
        } else {
          reject(new Error('wx.login failed'))
        }
      },
      fail: reject
    })
  })
}

// 确保已登录（如果没有token则自动登录）
function ensureLogin() {
  return new Promise((resolve, reject) => {
    if (isLoggedIn()) {
      resolve({ token: getToken() })
    } else {
      login().then(resolve).catch(reject)
    }
  })
}

// 退出登录
function logout() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('userId')
  const app = getApp()
  if (app) {
    app.globalData.token = null
    app.globalData.userInfo = null
  }
}

module.exports = {
  isLoggedIn,
  getToken,
  getUserId,
  login,
  ensureLogin,
  logout
}
