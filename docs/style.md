# 智知 ZhiZhi · UI 设计规范（深空蓝主题）

> 本规范是全平台统一的视觉标准。**以 admin 端（`frontend/`）的"深空蓝"科幻暗色主题为唯一基准**，小程序及未来任何新端都必须向本规范对齐。
>
> - 规范唯一真实来源（token + 组件覆盖）：`frontend/src/styles/global.css`
> - 可视化预览：[`style.html`](./style.html)（浏览器直接打开，无需构建）
> - 任何新增页面/组件必须使用本文件定义的 token，**禁止散落硬编码色值**（详见 [§11 落地规范](#11-落地规范)）。

---

## 1. 设计语言与原则

**视觉关键词**：深空蓝 · 科幻 · 暗色 · 霓虹辉光（neon glow）· 玻璃拟态（glassmorphism）· 星空网格背景 · 几何六边形。

| 原则 | 含义 |
|------|------|
| **暗色为底** | 全站以深空蓝黑 `--bg-dark` 为底，内容承载在半透明玻璃卡上。不存在浅色模式。 |
| **辉光强调** | 焦点、激活、悬浮态用霓虹蓝辉光（`--shadow-neon` / `text-shadow`）做强调，而非实心高饱和填充。 |
| **玻璃层级** | 卡片、对话框、下拉、提示统一 `backdrop-filter: blur(20px)` + 半透明背景，营造空间纵深。 |
| **几何科技感** | 六边形 clip-path、网格背景、渐变装饰条、扫光动画构成科技氛围，作为"装饰层"而非内容层。 |
| **克制动效** | 动效服务于反馈（hover 上浮、focus 辉光、流式光标），时长 0.15–0.5s，缓动统一 `cubic-bezier(0.4,0,0.2,1)`。 |
| **字体分工** | 数据用 Orbitron，标题/控件用 Rajdhani，正文用 Exo 2（见 [§4](#4-字体与排版)）。 |

---

## 2. 色彩系统

所有颜色以 CSS 变量（design token）形式定义在 `:root`。**业务代码一律引用变量，不写裸色值。**

### 2.1 品牌主色

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-primary` | `#0066FF` | 主操作、主按钮、激活态、链接、进度 |
| `--color-primary-light` | `#3388FF` | hover 提亮、次级强调文字 |
| `--color-primary-dark` | `#0044CC` | 渐变暗端、按下态 |

### 2.2 科幻蓝色系

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-neon-blue` | `#00D4FF` | 霓虹强调：focus 边框、激活文字、辉光、渐变亮端 |
| `--color-neon-blue-glow` | `rgba(0,212,255,0.4)` | 辉光阴影/text-shadow 的基色 |
| `--color-deep-space` | `#0A1628` | 深空背景渐变 |
| `--color-stellar-blue` | `#1E3A5F` | 滚动条滑块、深色填充 |
| `--color-cosmic-purple` | `#7B61FF` | 紫色强调 / info 语义 / 第三档评分色 |

### 2.3 语义色

> ✅ **现状**：语义色已在 `global.css` 的 `:root` 中正式 token 化——每色含主色 + `-light`/`-dark` 变体 + `-bg`(10%) + `-border`(30%)，并已替换全项目（`global.css` 及各 `.vue`）的散落引用。新代码一律使用下表 token，不得再新增裸色值。

| 语义 | 主色 | 浅色背景（10%） | 边框（30%） | 既有用途 |
|------|------|----------------|-------------|----------|
| 成功 `--color-success` | `#00ff88` | `rgba(0,255,136,0.1)` | `rgba(0,255,136,0.3)` | success 按钮/标签、在线状态点、知识库 active 徽章、评分 >0.8 |
| 危险 `--color-danger` | `#ff4757` | `rgba(255,71,87,0.1)` | `rgba(255,71,87,0.3)` | danger 按钮/标签、删除操作、danger-card、退出登录 |
| 警告 `--color-warning` | `#ffa500` | `rgba(255,165,0,0.1)` | `rgba(255,165,0,0.3)` | warning 按钮/标签、评分图标 |
| 信息 `--color-info` | `#7b61ff` | `rgba(123,97,255,0.1)` | `rgba(123,97,255,0.3)` | info 按钮/标签、知识库 archived 徽章、评分 <0.6 |

> 注意：`#00ff88` 成功绿与 Element Plus 默认 `#67c23a` 不同；危险色统一用 `#ff4757`（个别旧代码出现过 `#f56c6c`/`#e6a23c`，属待治理的硬编码，应替换为上表标准值）。

### 2.4 背景层级

| Token | 值 | 用途 |
|-------|-----|------|
| `--bg-dark` | `#0B0E14` | 页面最底层背景（body） |
| `--bg-card` | `rgba(15,23,42,0.85)` | 卡片、对话框、下拉、提示（配合 blur） |
| `--bg-sidebar` | `rgba(10,22,40,0.95)` | 侧边栏 |
| `--bg-input` | `rgba(30,58,95,0.3)` | 输入框、文本域、次级按钮底色 |

### 2.5 文字层级

| Token | 值 | 用途 |
|-------|-----|------|
| `--text-primary` | `#E8F4FD` | 主要文字、标题、表格正文 |
| `--text-secondary` | `rgba(232,244,253,0.7)` | 次要文字、标签 label、说明 |
| `--text-muted` | `rgba(232,244,253,0.4)` | 占位符、禁用、时间戳、辅助提示 |

### 2.6 边框与辉光

| Token | 值 | 用途 |
|-------|-----|------|
| `--border-color` | `rgba(0,102,255,0.2)` | 默认边框（卡片、输入、分割） |
| `--border-glow` | `rgba(0,212,255,0.3)` | hover/激活态高亮边框 |

### 2.7 阴影

| Token | 值 | 用途 |
|-------|-----|------|
| `--shadow-card` | `0 8px 32px rgba(0,0,0,0.3)` | 卡片默认阴影 |
| `--shadow-glow` | `0 0 20px rgba(0,102,255,0.15), 0 0 60px rgba(0,212,255,0.1)` | 主色辉光 |
| `--shadow-neon` | `0 0 10px var(--color-neon-blue-glow), 0 0 40px rgba(0,212,255,0.2)` | 霓虹辉光（hover 强调） |
| `--shadow-online` | `0 0 10px rgba(0,255,136,0.5)` | 在线/激活状态点的绿色辉光 |
| `--shadow-focus-ring` | `0 0 0 3px var(--overlay-neon-10)` | 输入框/选择器 focus 态的青色描边环 |
| `--shadow-btn-primary-hover` | `0 0 20px var(--overlay-primary-40)` | 主按钮 hover 辉光 |
| `--shadow-btn-secondary-hover` | `0 0 20px var(--overlay-primary-20)` | 次级按钮 hover 辉光 |
| `--shadow-msg-user` | `0 4px 20px var(--overlay-primary-30)` | 用户消息气泡阴影 |

> 派生阴影 token 引用 `--overlay-*`，自身定义在 `:root`。组件需要焦点环/按钮辉光时直接引用这些语义 token，不要内联 `box-shadow: 0 0 …`。

### 2.8 叠加色（overlay）

半透明叠加色的统一档位。**组件 `<style scoped>` 内严禁直接写 `rgba(...)`**——背景、边框、阴影、渐变里的半透明色一律引用下列 token；缺档位时在 `global.css` 的 `:root` 补充，不在组件内硬编码。

| 家族 | Token | 值 |
|------|-------|-----|
| 主蓝 `0,102,255` | `--overlay-primary-02 / -03 / -04 / -05 / -08 / -10 / -12 / -15 / -20 / -25 / -30 / -40` | alpha 0.02→0.40，次级底色、网格、表格条纹、hover 底、辉光阴影 |
| 霓虹青 `0,212,255` | `--overlay-neon-03 / -05 / -08 / -10 / -15 / -20` | alpha 0.03→0.20，强调底色、激活态、青色辉光 |
| 白 `255,255,255` | `--overlay-white-10 / -20 / -30 / -50` | 扫光渐变、文字辉光、关闭按钮 hover |
| 黑 `0,0,0` | `--overlay-black-20 / -30 / -50` | 深色底、滚动条轨道、大投影 |
| 星蓝面 `30,58,95` | `--overlay-stellar-15 / -20` | 禁用输入底、列表项 hover 底（`--bg-input` 为 0.3 档） |
| 宇宙紫 `123,97,255` | `--overlay-purple-03 / -06` | 背景星云装饰 |
| 深空底 | `--overlay-deep-60`（`10,22,40`）、`--overlay-dark-80`（`11,14,20`=`--bg-dark`） | 表格容器底、loading 遮罩 |
| danger `255,71,87` | `--overlay-danger-20 / -40 / -50` | 危险态 hover/激活边框与辉光（0.1=`--color-danger-bg`，0.3=`--color-danger-border`） |

---

## 3. 间距、布局与圆角

### 3.1 布局尺寸

| Token | 值 | 用途 |
|-------|-----|------|
| `--sidebar-width` | `240px` | 侧边栏展开宽度（折叠态 `72px`） |
| `--header-height` | `64px` | 顶栏高度 |

- 页面容器 `.page-container`：内边距 `28px`，`min-height: 100vh`。
- 卡片内边距：标准 `24px`；紧凑场景 `16px`。
- 栅格间距（grid gap）：`24px`。

### 3.2 间距阶梯（推荐）

虽未 token 化，但全站约定使用 4 的倍数：`4 / 8 / 12 / 16 / 20 / 24 / 28 / 32 / 40px`。新代码请落在此阶梯内。

### 3.3 圆角

| Token | 值 | 用途 |
|-------|-----|------|
| `--radius-sm` | `6px` | 标签、小徽章、代码块 |
| `--radius-md` | `10px` | 输入框、下拉、按钮、菜单项、提示 |
| `--radius-lg` | `16px` | 卡片、统计卡、消息气泡 |
| `--radius-xl` | `24px` | 对话框、确认框、登录卡 |

---

## 4. 字体与排版

```
@import 'Orbitron'  →  数字 / 数据展示（统计值、ID、API Key）  fallback: monospace
@import 'Rajdhani'  →  标题 / 按钮 / 标签 / 表单 label / 表头  fallback: sans-serif
@import 'Exo 2'     →  正文（body 默认）  fallback: 'PingFang SC','Microsoft YaHei'
```

| 场景 | 字体 | 字号 | 字重 | 其它 |
|------|------|------|------|------|
| 正文 | Exo 2 | 14px | 400 | `line-height: 1.6` |
| 页面标题 `.page-header h2` | Rajdhani | 28px | 700 | `text-transform: uppercase`，`letter-spacing: 1–2px`，渐变文字 |
| 卡片小标题 h3 | Rajdhani | 18px | 600 | — |
| 按钮 | Rajdhani | — | 600 | `letter-spacing: 0.5px` |
| 表头 | Rajdhani | — | 600 | uppercase，`letter-spacing: 0.5px` |
| 统计数值 `.stat-card .value` | Orbitron | 36px | 700 | `text-shadow` 霓虹辉光 |
| 占位符/辅助 | Exo 2 | 12–13px | — | `--text-muted` |

**渐变标题**：标题统一用 `.gradient-text` 效果——`linear-gradient(135deg, var(--text-primary), var(--color-neon-blue))` + `background-clip: text`。

---

## 5. 全局元素

| 元素 | 规范 |
|------|------|
| **星空背景** | `body::before` 三处 `radial-gradient`（蓝/紫/青低透明度光晕），固定定位，`z-index:-1`。 |
| **科技网格** | `body::after` 双向 `linear-gradient` 1px 线，`background-size: 50px 50px`（聊天页用 `40px`），`z-index:-1`。 |
| **滚动条** | 宽 `6px`；轨道 `rgba(0,0,0,0.2)`；滑块 `--color-stellar-blue`，hover → `--color-primary`，圆角 `3px`。 |
| **文字选区** | `::selection` 背景 `--color-primary`，文字白色。 |
| **全局 reset** | `* { margin:0; padding:0; box-sizing:border-box; }`，`body { overflow-x:hidden }`。 |

---

## 6. 阴影与辉光效果（recipe）

可复用的视觉"配方"，跨组件复用，新组件应优先套用而非自创：

1. **玻璃卡片**：`background: var(--bg-card); backdrop-filter: blur(20px); border: 1px solid var(--border-color); border-radius: var(--radius-lg); box-shadow: var(--shadow-card);`（工具类 `.glass-effect`）
2. **悬浮上浮**：`hover { transform: translateY(-4px ~ -8px); box-shadow: var(--shadow-neon); border-color: var(--border-glow); }`
3. **顶/底渐变装饰条**：`linear-gradient(90deg, transparent, var(--color-primary), var(--color-neon-blue), transparent)`，高 2–3px，常配 hover 时 `opacity 0→1`。
4. **渐变图标方块**：`linear-gradient(135deg, var(--color-primary), var(--color-neon-blue))` + 圆角 + 内嵌 `icon-glow` 径向辉光。
5. **六边形装饰**：`clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)` + 渐变填充，作为空状态/卡片角落装饰。
6. **扫光（shine）**：按钮内置白色半透明 `linear-gradient` 覆盖层，hover 时从左扫到右（`.btn-shine` / `.btn-glow`）。
7. **辉光文字**：`text-shadow: 0 0 10–20px var(--color-neon-blue-glow)`（工具类 `.glow-text`）。
8. **状态点**：`6px` 圆点 + `box-shadow: 0 0 10px <色>`，在线态绿点常配 `blink` 闪烁。
9. **评分色阶**：`>0.8 → #00ff88` / `0.6–0.8 → #00d4ff` / `<0.6 → #7b61ff`。

---

## 7. 过渡与动画

### 7.1 过渡 token

| Token | 值 | 用途 |
|-------|-----|------|
| `--transition-fast` | `0.15s ease` | 颜色/小状态切换 |
| `--transition-normal` | `0.3s cubic-bezier(0.4,0,0.2,1)` | **默认**：hover、focus、卡片、菜单 |
| `--transition-slow` | `0.5s cubic-bezier(0.4,0,0.2,1)` | 扫光、大幅位移、入场 |

### 7.2 全局关键帧（定义于 `global.css`）

| `@keyframes` | 效果 | 典型用途 |
|--------------|------|----------|
| `pulse-glow` | box-shadow 辉光强弱呼吸 | 图标辉光、头像辉光、空状态 |
| `float` | 上下浮动 ±10px | 六边形/装饰元素漂浮 |
| `slide-in-left` / `slide-in-right` | 横向滑入 + 淡入 | 列表/面板入场 |
| `fade-in-up` | 上移 20px + 淡入 | 消息气泡、卡片入场 |

### 7.3 组件级关键帧（定义于各 `.vue`）

> 这些是局部动画，集中登记于此以免重复造轮子；复用时请引用同名实现。

| 动画 | 位置 | 效果 |
|------|------|------|
| `pulse` | Layout logo / ChatHeader 图标 | 图标缩放/辉光脉冲 |
| `fade` | Layout | 淡入 |
| `blink` | ChatHeader `tag-dot` | RAG 状态绿点闪烁 |
| `bounce` | MessageBubble | 输入中三点跳动（错峰延迟） |
| `cursor-blink` | MessageBubble | 流式输出光标闪烁 |
| `rotate` | Login 装饰 | 径向渐变旋转 |
| `float-particle` | Login | 50 粒子漂浮（随机延迟） |
| `line-move` | Login 底部线 | 装饰线流动 |
| `line-pulse` | MessageList | 装饰线 scaleX 脉冲（错峰） |

**交互规范**：可点击元素必须有 hover 反馈（上浮 / 辉光 / 变色之一）；输入类 focus 必须有霓虹蓝边框 + `0 0 0 3px rgba(0,212,255,0.1)` 外发光。

---

## 8. Element Plus 组件规范

> 全局覆盖见 `global.css`。下表覆盖项目**实际使用到的全部组件**，新页面沿用即可，无需重复写覆盖样式。

### 8.1 按钮 `el-button`
- 字体 Rajdhani / 600 / `letter-spacing: 0.5px`，过渡 `--transition-normal`。
- 类型：`primary`（主渐变蓝）、`default`（`--bg-input` 底 + 边框，hover 变主色）、`success`（绿渐变，**深色文字 `#0B0E14`**）、`warning`（橙渐变）、`danger`（红渐变）、`info`（紫渐变）。
- `is-plain` 变体：浅色半透明底 + 同色边框，hover 反相填充。
- hover 统一：渐变提亮 + 辉光 `box-shadow` + `translateY(-1px)`。

### 8.2 卡片 `el-card`
玻璃卡：`--bg-card` + blur(20) + `--border-color` + `--radius-lg` + `--shadow-card`；`__header` 底部 `--border-color` 分隔。

### 8.3 输入框 `el-input` / 文本域 `el-textarea`
- `__wrapper` 底 `--bg-input` + 边框 `--border-color` + `--radius-md`，去除默认 box-shadow。
- hover → 边框 `--color-primary`；focus → 边框 `--color-neon-blue` + `0 0 0 3px rgba(0,212,255,0.1)`。
- 文字 `--text-primary`，占位符 `--text-muted`；禁用态底色更暗、文字 `--text-muted`。

### 8.4 表单 `el-form` / `el-form-item`
label：`--text-secondary`，Rajdhani / 500。

### 8.5 表格 `el-table` / `el-table-column`
- 透明底（通过 `--el-table-*` CSS 变量 API 设定）；表头底 `rgba(0,102,255,0.1)`，表头文字 Rajdhani / 600 / uppercase。
- 行 hover `rgba(0,102,255,0.08–0.12)`；斑马纹 `rgba(0,102,255,0.04)`；边框 `--border-color`。
- 内嵌复选框：未选 `--bg-input` + `--border-color`，选中 `--color-primary`。
- 空状态文字 `--text-secondary`。

### 8.6 对话框 `el-dialog` / 抽屉 `el-drawer`
- 玻璃底 + blur(20) + `--radius-xl`（抽屉无强制圆角）。
- header/footer 用 `--border-color` 分隔；title Rajdhani / 600 / `--text-primary`；关闭按钮 hover → `--color-neon-blue`。
- header padding `20px 24px`，body `24px`，footer `16px 24px`。
- 宽度约定：表单类 `480–700px`；预览类 `70%`（顶部偏移 `5vh`）。

### 8.7 确认框 `el-message-box` / 消息 `el-message`
玻璃底 + blur + 边框；message-box title Rajdhani / 600，圆角 `--radius-xl`；message 圆角 `--radius-md`。命令式调用 `ElMessage` / `ElMessageBox` 自动套用。

### 8.8 标签 `el-tag`
Rajdhani / 600 / 圆角 `6px`；四语义色用对应"浅底 10% + 边框 30% + 主色文字"。注意：选择器多选回显标签复用 `el-tag--info`，被覆盖为主色蓝调。

### 8.9 选择器 `el-select` / `el-option`
- 触发器同输入框；focus 霓虹边框。
- 下拉面板玻璃底 + blur + `--radius-md`；选项 hover `rgba(0,102,255,0.15)`，选中 `--color-neon-blue` / 600。

### 8.10 日期/时间选择器 `el-date-picker` / `el-time-picker` / 范围选择
触发器同输入框；面板玻璃底 + blur + 边框；今天 `--color-neon-blue`，选中 `--color-primary`，可选 hover `rgba(0,102,255,0.1)`。

### 8.11 下拉菜单 `el-dropdown` / `el-dropdown-menu`
玻璃面板；item hover `rgba(0,102,255,0.15)` + `--color-neon-blue`；禁用 `--text-muted`。

### 8.12 工具提示 `el-tooltip` / 弹出框 `el-popover` / 气泡确认 `el-popconfirm`
统一玻璃底 + blur + 边框 + `--radius-md`，文字 `--text-primary`。

### 8.13 分页 `el-pagination`
按钮/页码底 `--bg-input` + 文字 `--text-secondary`，hover → `--color-neon-blue`，当前页 `--color-primary` / 白字。

### 8.14 标签页 `el-tabs` / `el-tab-pane`
item `--text-secondary` / Rajdhani，激活 `--color-neon-blue`，active-bar 主色→霓虹渐变。

### 8.15 步骤条 `el-steps` / `el-step`
title `--text-secondary`；进行中 `--color-neon-blue`，完成 `--color-primary`（含 head 描边）。

### 8.16 上传 `el-upload`（拖拽 dragger）
自定义边框 + 底色，hover 高亮边框（详见 KnowledgeDetail）。

### 8.17 描述列表 `el-descriptions`
label 底 `rgba(0,102,255,0.1)` / `--text-secondary`，content 底 `--bg-input` / `--text-primary`，边框 `rgba(0,102,255,0.15)`。

### 8.18 提示 `el-alert`
底 `rgba(0,102,255,0.1)` + 边框 `rgba(0,102,255,0.25)`；title `--text-primary`，描述 `--text-secondary`，图标 `--color-neon-blue`。

### 8.19 空状态 `el-empty`
透明底，描述文字 `--text-secondary` / Rajdhani。

### 8.20 加载 `el-loading`
遮罩 `rgba(11,14,20,0.8)` + blur(10)；spinner 描边 `--color-neon-blue`；文字 `--text-secondary`。

### 8.21 菜单 `el-menu` / `el-menu-item` / `el-sub-menu`
透明底、去右边框；item `--text-secondary`，hover `rgba(0,102,255,0.1)` + `--text-primary`，激活 `--color-neon-blue` + `rgba(0,212,255,0.08)` 底（业务侧另加 3px 渐变左边框，见 §9.1）。

### 8.22 面包屑 `el-breadcrumb`
普通 `--text-secondary`，可点击 hover `--color-neon-blue`，分隔符 `--text-muted`。

### 8.23 链接 `el-link`
Rajdhani / 500；primary 用 `--color-neon-blue`，hover 提亮 + 辉光文字。

### 8.24 其它已覆盖组件
单选 `el-radio`、复选 `el-checkbox`、开关 `el-switch`（开 `--color-primary`）、滑块 `el-slider`、评分 `el-rate`（激活 `#ffa500`）、进度条 `el-progress`（主色→霓虹渐变）、折叠面板 `el-collapse`、树 `el-tree`（当前节点霓虹）、轮播 `el-carousel`、头像 `el-avatar`、图标 `el-icon`、滚动条 `el-scrollbar`、布局容器 `el-container`/`el-aside`/`el-header`/`el-main`、栅格 `el-row`/`el-col`——均已在 `global.css` 中按深空蓝主题覆盖，直接使用。

---

## 9. 自定义业务组件规范

复用本节定义的组件结构与命名，保证跨页面一致。

### 9.1 应用框架（`Layout.vue`）
- **侧边栏**：`--bg-sidebar` + 渐变装饰，展开 `240px` / 折叠 `72px`，过渡 `--transition-normal`。
- **Logo**：`logo-icon` 渐变方块 + `logo-char` 霓虹 text-shadow + `logo-title`/`logo-subtitle`。
- **菜单项 `menu-item`**：含 `menu-icon-wrapper` + `menu-icon-glow`；hover 出现 3px 渐变左边框动画，激活态左边框 + 辉光。
- **顶栏 `top-header`**：玻璃底 + blur，含折叠按钮、`el-breadcrumb`、右侧 `user-info`（`user-avatar` + `user-status` 在线绿点）。

### 9.2 页面头部（`.page-header`，全局类）
左侧 `header-content`（`h2` 渐变标题 + `header-subtitle`），右侧主操作按钮；常配 `header-decoration`（点 + 线）。

### 9.3 统计卡片（`.stat-card` / `stats-grid`）
玻璃卡 + 顶部渐变条（hover 显现）+ `stat-icon-wrapper`（含 `icon-glow`）+ `stat-label`（Rajdhani uppercase）+ `stat-value`（Orbitron 36px + 辉光）+ `decoration-hex` 六边形 + hover `translateY(-8px)` + `--shadow-neon`。响应式：3 列 → 1024px 2 列 → 768px 1 列。

### 9.4 区块卡片（`.section-card`）
玻璃卡 + `section-header`（`section-icon` + 标题 + `section-line`）；内含 `action-grid`（`action-btn` 带 `btn-shine` 扫光 / `action-btn-secondary`）。

### 9.5 知识库卡片（`KnowledgeList` 的 `.kb-card` / `kb-grid`）
玻璃卡（`grid-template-columns: minmax(320px,...)`，gap 24px）+ `kb-icon`（渐变方块 + glow）+ `kb-status` 状态徽章（active 绿 / archived 紫，带 `status-dot`）+ `kb-name`/`kb-desc` + `kb-stats`（含 `stat-divider`）+ `card-border` 顶部辉光条 + `decoration-hex` + hover `translateY(-8px)` + `--shadow-glow`。

### 9.6 空状态（`.empty-state` / `.empty-chat`，复用模式）
居中 `empty-icon`（六边形 clip-path + `icon-glow` 径向辉光 + `pulse-glow`/`float` 动画）+ `empty-title` + `empty-subtitle` + 可选 `decoration-line`（`line-pulse` 错峰脉冲）。

### 9.7 聊天头部（`ChatHeader.vue`）
玻璃底 + 底部渐变边框 + `title-icon`（渐变方块 + `icon-pulse`）+ `title-text` + `api-select`（`el-select`）+ `rag-tag`（含 `tag-dot` 绿点 `blink`）+ `new-chat-btn`。`z-index:10`。

### 9.8 聊天输入（`ChatInput.vue`）
玻璃 `input-wrapper` + 顶部渐变边框 + `message-input`（`el-input` 多行）+ `decoration-line`（`focus-within` 显现）+ `send-btn`（56px 渐变按钮 + `btn-glow` 扫光，移动端 48px 隐藏文字）。

### 9.9 消息气泡（`MessageBubble.vue`）
- **用户气泡 `msg-user`**：右对齐，`linear-gradient(135deg, primary, primary-dark)` 实心，`box-shadow: 0 4px 20px rgba(0,102,255,0.3)`，右下角小圆角 `4px`。
- **AI 气泡 `msg-ai`**：左对齐，玻璃底 + 顶部渐变边框，`fade-in-up` 入场。
- **头像 `ai-avatar`/`user-avatar`**：44px 圆角方块（移动端 36px）+ `avatar-glow`（`pulse-glow`）+ 霓虹 text-shadow。
- **`markdown-body`**：`:deep()` 富文本（标题 20/18/16px、代码块、引用、表格、列表）。
- **`typing-indicator`**：三点 `bounce` 错峰；**`streaming-cursor`**：`cursor-blink`。
- **来源 `msg-references`**：`references-header`（`ref-icon-wrapper`）+ `references-list`，每项用 CSS counter 编号徽章（22px 渐变方块）+ `ref-doc-name` + `ref-score`（评分色阶 §6.9）+ 左边框 3px，hover `translateX` 位移。

### 9.10 消息列表（`MessageList.vue`）/ 来源预览（`SourcePreview.vue`）
- 列表：`chat-messages` 容器 + `msg-row`，空态用 §9.6 模式。
- 预览：`el-dialog`（700px）+ `el-scrollbar`（max-height 500px）+ `preview-meta`（flex 元信息）+ `preview-text`（`white-space: pre-wrap; word-break: break-word`）。

### 9.11 历史会话项（`ChatHistory.vue`）
`el-row`/`el-col` 8/16 分栏；`conv-item` 默认/hover/`conv-item-selected`（强调底 + 主色边框）；右侧 `markdown-body` 渲染，容器 `max-height:600px` 滚动。

### 9.12 个人中心（`Profile.vue`）
`settings-grid` 2 列（1024px 折叠）；卡片族：`user-card`（`user-avatar` + `avatar-glow`）、`plan-card`（`plan-badge` + `badge-glow`，`--plan-color` 动态色）、`api-card`（`api-table` + `key-value` 等宽字体 + `kb-tag`）、`danger-card`（红色调边框/阴影，`logout-btn`）。表单含 `preset-btn` 预设按钮组。

### 9.13 登录页（`Login.vue`）
双栏：左 `login-decoration`（`logo-glow` + `tagline` + `decoration-circles` 3 旋转圆 `rotate` + `hex` 六边形），右 `login-form-container`（玻璃卡 blur(30) + `submit-btn` 含 `btn-glow` 扫光）；背景 `particles`（50 粒子 `float-particle`）+ `grid-overlay`（30px 网格）+ 底部 `line`（`line-move`）。

---

## 10. 工具类

| 类名 | 效果 |
|------|------|
| `.glow-text` | 霓虹蓝 text-shadow 辉光 |
| `.glow-border` | 外发光 + 内发光边框 |
| `.glass-effect` | 玻璃拟态（半透明 + blur(20) + 边框） |
| `.gradient-text` | 主文字→霓虹蓝渐变文字（标题用） |

---

## 11. 落地规范

1. **统一基准**：admin 端深空蓝主题为唯一视觉标准。小程序当前为独立的浅色蓝白方案（`#1890ff`/`#f5f7fa`），属历史遗留，后续改版须向本规范靠拢（暗色 + 深空蓝 token）。
2. **必须用 token**：新页面/组件的颜色、圆角、阴影、过渡一律引用 `:root` 变量；**禁止新增裸色值**。语义色用 §2.3 的 `--color-success/danger/warning/info`（及 `-light`/`-dark`/`-bg`/`-border` 变体），均已在 `global.css` 定义。
3. **治理存量硬编码**：语义色（success/danger/warning/info）已完成 token 化清理。剩余待治理项为：品牌蓝的透明度变体（`rgba(0,102,255,*)`）与散落裸 px，以及少量无对应 token 的派生透明度辉光（如 `rgba(255,71,87,0.5)`）和中性灰 `#909399`——改动相关组件时顺手收敛为 token / 间距阶梯。
4. **复用优先**：先套用 §6 的 recipe 与 §9 的业务组件结构，再考虑自定义；新动画优先复用 §7 已登记的关键帧。
5. **组件覆盖集中维护**：Element Plus 的主题覆盖只在 `global.css` 维护，不在业务组件里重复写全局覆盖；组件内仅写该组件特有的布局/装饰。
6. **响应式**：标准断点 `1024px`（多列→两列）与 `768px`（→单列、隐藏次要文字/装饰）。
7. **预览同步**：调整 token 或新增组件后，同步更新 [`style.html`](./style.html) 的展示，使其始终等于"活的"设计规范。

---

_本规范由 `frontend/src/styles/global.css` 与全部 `frontend/src/**/*.vue` 反向梳理而成。当二者冲突时，以 `global.css` 的 token 定义为准，并据此修订本文。_
