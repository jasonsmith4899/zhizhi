// API基地址，从app.globalData.apiBaseUrl读取
function getBaseUrl() {
  const app = getApp()
  return app ? app.globalData.apiBaseUrl : 'http://localhost:8080'
}

// 请求封装
function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')

    // 构建请求头
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }

    // 自动带token（除非明确指定noAuth）
    if (!options.noAuth && token) {
      header['Authorization'] = `Bearer ${token}`
    }

    wx.request({
      url: getBaseUrl() + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: options.timeout || 30000,
      success(res) {
        if (res.statusCode === 200) {
          const body = res.data
          if (body.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error(body.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          // token过期，清除登录态并重新登录
          wx.removeStorageSync('token')
          wx.removeStorageSync('userId')
          const app = getApp()
          if (app) {
            app.login().then(() => {
              // 重新发起原请求
              request(options).then(resolve).catch(reject)
            }).catch(() => {
              wx.showToast({ title: '登录失效，请重试', icon: 'none' })
              reject(new Error('登录失效'))
            })
          } else {
            reject(new Error('登录失效'))
          }
        } else {
          reject(new Error(res.data?.message || `请求失败(${res.statusCode})`))
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

module.exports = { request, getBaseUrl }
