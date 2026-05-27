const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    conversations: [],
    loading: true,
    startX: 0,
    swipedIndex: -1
  },

  onLoad() {
    this.loadConversations()
  },

  onShow() {
    // 每次显示时刷新列表
    this.loadConversations()
  },

  // 加载会话列表
  async loadConversations() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const conversations = await request({
        url: '/api/mp/conversations'
      })
      // 格式化时间
      const formatted = conversations.map(item => ({
        ...item,
        updatedAtText: this.formatTime(item.updatedAt)
      }))
      this.setData({ conversations: formatted })
    } catch (err) {
      console.error('加载会话失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 格式化时间
  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date

    // 今天内
    if (diff < 86400000 && date.getDate() === now.getDate()) {
      const h = date.getHours().toString().padStart(2, '0')
      const m = date.getMinutes().toString().padStart(2, '0')
      return `${h}:${m}`
    }

    // 昨天
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.getDate() === yesterday.getDate() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getFullYear() === yesterday.getFullYear()) {
      return '昨天'
    }

    // 今年内
    if (date.getFullYear() === now.getFullYear()) {
      return `${date.getMonth() + 1}月${date.getDate()}日`
    }

    // 更早
    return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
  },

  // 点击进入对话
  enterChat(e) {
    const sessionId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/chat/chat?sessionId=${sessionId}`
    })
  },

  // 左滑开始
  touchStart(e) {
    this.setData({
      startX: e.touches[0].clientX,
      swipedIndex: -1
    })
  },

  // 左滑移动
  touchMove(e) {
    const startX = this.data.startX
    const moveX = e.touches[0].clientX
    const diff = startX - moveX
    const idx = e.currentTarget.dataset.idx

    if (diff > 50) {
      this.setData({ swipedIndex: idx })
    } else {
      this.setData({ swipedIndex: -1 })
    }
  },

  // 左滑结束
  touchEnd() {
    // 保持当前滑动状态
  },

  // 删除会话
  async deleteConversation(e) {
    const id = e.currentTarget.dataset.id
    const idx = e.currentTarget.dataset.idx

    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个对话吗？',
      confirmColor: '#ff4d4f',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({
              url: `/api/mp/conversations/${id}`,
              method: 'DELETE'
            })
            // 从列表中移除
            const conversations = this.data.conversations
            conversations.splice(idx, 1)
            this.setData({
              conversations,
              swipedIndex: -1
            })
            wx.showToast({ title: '已删除', icon: 'success' })
          } catch (err) {
            console.error('删除失败:', err)
            wx.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      }
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadConversations().finally(() => {
      wx.stopPullDownRefresh()
    })
  }
})
