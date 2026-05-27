# 提醒类 Android 应用 — 项目实现 Plan

> 适配澎湃OS/小米手机，轻量级单机提醒应用

---

## 1. 技术选型

| 项目 | 选型 | 理由 |
|------|------|------|
| 语言 | **Kotlin** | Android 首选，协程支持好 |
| 最低 SDK | **API 26 (Android 8.0)** | NotificationChannel 必须，覆盖小米主流机型 |
| 目标 SDK | **API 34 (Android 14)** | 适配澎湃OS |
| UI 框架 | **Jetpack Compose** | 声明式 UI，代码量少，2024+ 已成熟 |
| 数据库 | **Room** | 单机本地存储，SQLite 封装，生命周期感知 |
| 通知调度 | **AlarmManager + WorkManager** | AlarmManager 精确定时，WorkManager 兜底保障 |
| 异步 | **Kotlin Coroutines + Flow** | Room 原生支持，UI 响应式更新 |
| DI | **Hilt** | Google 推荐，轻量 Dagger 封装 |
| 图标变色 | **桌面 Widget + AppWidgetProvider** | Android 唯一可靠的动态图标方案 |
| 导出备份 | **Gson + FileProvider** | JSON 导出，FileProvider 安全分享 |
| 构建 | **Gradle Kotlin DSL** | 统一语言栈 |

**不引入的：** 云同步、账号系统、网络请求、小米澎湃 SDK — 保持单机轻量。

---

## 2. 项目结构

```
com.mason.reminder/
├── di/                     # Hilt 模块
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── WorkerModule.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── CategoryDao.kt
│   │   │   ├── TaskDao.kt
│   │   │   └── ReminderLogDao.kt
│   │   └── entity/
│   │   │   ├── CategoryEntity.kt
│   │   │   ├── TaskEntity.kt
│   │   │   ├── ReminderLogEntity.kt
│   │   │   └── ReminderLevelEntity.kt
│   ├── model/              # UI 层用的 domain model
│   │   ├── Category.kt
│   │   ├── Task.kt
│   │   ├── ReminderLevel.kt
│   │   ├── UrgencyState.kt  # 紧急度枚举（蓝绿/黄/橙/红）
│   ├── repository/
│   │   ├── CategoryRepository.kt
│   │   ├── TaskRepository.kt
│   │   ├── ReminderRepository.kt
│   ├── backup/
│   │   ├── BackupExporter.kt
│   │   ├── BackupImporter.kt
├── scheduler/
│   ├── AlarmScheduler.kt    # 设置/取消 AlarmManager 精确闹钟
│   ├── NotificationHelper.kt # 构建/更新通知
│   ├── NotificationChannelRegistrar.kt # 注册三个 Channel
│   ├── ReminderWorker.kt    # WorkManager 兜底 worker
│   ├── SnoozeHandler.kt     # Snooze 逻辑
├── widget/
│   ├── UrgencyWidget.kt     # 桌面 Widget Provider
│   ├── UrgencyWidgetLayout.kt # Widget 布局（XML，Compose 不支持 Widget）
│   ├── WidgetColorMapper.kt # 紧急度→颜色映射
│   ├── IconUpdater.kt       # 更新 Widget 颜色状态
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   ├── Routes.kt
│   ├── screen/
│   │   ├── HomeScreen.kt      # 分类列表（一级）
│   │   ├── TaskListScreen.kt   # 待办列表（二级）
│   │   ├── TaskDetailScreen.kt # 新建/编辑待办
│   │   ├── CategoryEditScreen.kt # 新建/编辑分类
│   │   ├── SettingsScreen.kt   # 设置页
│   │   ├── BackupScreen.kt     # 备份/恢复
│   ├── component/
│   │   ├── UrgencyBadge.kt     # 紧急度色块
│   │   ├── CountdownText.kt    # "还剩X天" 文字
│   │   ├── TaskCard.kt         # 任务卡片组件
│   │   ├── CategoryIconPicker.kt
│   │   ├── ReminderLevelPicker.kt
│   │   ├── SnoozeActionRow.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt            # 包含紧急度色值定义
│   │   ├── Type.kt
├── util/
│   ├── DateExt.kt            # 日期工具
│   ├── ReminderCalculator.kt # 计算下次到期时间
│   ├── UrgencyCalculator.kt  # 计算当前紧急度
├── MainActivity.kt
├── ReminderApp.kt            # @HiltAndroidApp Application
```

---

## 3. 数据模型

### 3.1 ER 关系

```
Category (1) ──── (N) Task
Task (1) ──── (N) ReminderLog
ReminderLevel (自定义) 存在 Task 字段内
```

### 3.2 表结构

#### CategoryEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 自增主键 |
| name | String | 分类名称 |
| icon_name | String | 图标名（Material Icon name） |
| color_hex | String | 分类颜色 |
| sort_order | Int | 排序序号 |
| created_at | Long | 创建时间戳 |
| updated_at | Long | 更新时间戳 |

#### TaskEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 自增主键 |
| title | String | 任务标题（必填） |
| description | String | 任务描述（可选） |
| category_id | Long (FK) | 所属分类 |
| reminder_type | String | "ONCE" / "RECURRING" |
| reminder_level | String | "LIGHT" / "MEDIUM" / "HEAVY" / "CUSTOM" |
| custom_advance_days | Int? | 自定义提前天数（null 表示非自定义） |
| custom_notify_freq | String? | 自定义通知频率："DAILY" / "EVERY_2_DAYS" / "WEEKLY" |
| interval_value | Int? | 循环间隔数值（null 表示一次性） |
| interval_unit | String? | "DAY" / "WEEK" / "MONTH"（null 表示一次性） |
| start_date | Long? | 循环起始日期 |
| due_date | Long? | 一次性到期日期 |
| next_due_date | Long | 下次到期时间（计算值，调度依赖此字段） |
| last_completed_at | Long? | 上次完成时间 |
| is_active | Boolean | 是否在活跃提醒中 |
| created_at | Long | 创建时间 |
| updated_at | Long | 更新时间 |

#### ReminderLogEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 自增主键 |
| task_id | Long (FK) | 关联任务 |
| notified_at | Long | 通知发送时间 |
| days_before_due | Int | 到期前多少天发的 |
| action_taken | String | "DISMISSED" / "COMPLETED" / "SNOOZED" |
| snoozed_until | Long? | Snooze 到什么时候 |

### 3.3 Domain Model 映射

```kotlin
enum class ReminderType { ONCE, RECURRING }
enum class ReminderLevel { LIGHT, MEDIUM, HEAVY, CUSTOM }
enum class IntervalUnit { DAY, WEEK, MONTH }
enum class UrgencyState {
    CALM,    // 无紧急 → 蓝绿色
    NOTICE,  // 3天内 → 黄色
    URGENT,  // 1天内 → 橙色
    CRITICAL // 今天 → 红色
}
```

### 3.4 紧急度计算规则

```kotlin
fun calculateUrgency(task: Task, now: LocalDate): UrgencyState {
    val daysLeft = task.nextDueDate.toLocalDate().daysUntil(now)
    return when {
        daysLeft <= 0  → CRITICAL
        daysLeft <= 1  → URGENT
        daysLeft <= 3  → NOTICE
        else           → CALM
    }
}
```

**全局紧急度**（决定 Widget/图标颜色）= 所有活跃任务中最高的 UrgencyState。

---

## 4. 核心流程

### 4.1 提醒调度流程

```
创建/编辑任务
    │
    ├─ 计算 next_due_date
    │   ONCE: due_date 即 next_due_date
    │   RECURRING: start_date + interval (或 last_completed_at + interval)
    │
    ├─ 根据 reminder_level 计算首次提醒时间
    │   LIGHT: next_due_date 当天 09:00
    │   MEDIUM: next_due_date - 3天 09:00
    │   HEAVY: next_due_date - 15天 09:00
    │   CUSTOM: next_due_date - custom_advance_days 09:00
    │
    ├─ AlarmScheduler.setExactAlarm(firstNotifyTime, taskId)
    │
    └─ 到时间 → BroadcastReceiver
        │
        ├─ 发送/更新通知
        │   LIGHT: 发一条，结束
        │   MEDIUM/HEAVY/CUSTOM:
        │     - 更新同一条通知（用相同 notificationId = taskId）
        │     - 通知内容: "还剩 X 天"
        │     - 如果还有后续提醒日，调度下一个
        │     - 到到期日当天，通知变为 "今天到期！"
        │
        ├─ 更新 Widget 颜色
        │
        └─ 用户操作:
            ├─ 点击通知 → 打开 TaskDetailScreen
            ├─ 标记完成 → 更新 last_completed_at, 计算新 next_due_date, 重新调度
            ├─ Snooze → SnoozeHandler: 延后 1h/3h/明天, 设置新 Alarm
            └─ Dismiss → 仅消掉通知，不改变任务状态
```

### 4.2 通知更新流程（中度/重度倒计时）

```
首次提醒日到达
    │
    ├─ 构建通知: NotificationCompat.Builder
    │   channelId = urgency对应的channel
    │   notificationId = taskId.toInt()  ← 关键：同一任务固定ID
    │   contentTitle = task.title
    │   contentText = "还剩 ${daysLeft} 天"
    │   setOngoing(true)  ← 不让用户轻易滑掉
    │
    ├─ 发出通知 notify(notificationId, notification)
    │
    └─ 调度次日提醒 AlarmScheduler.setExactAlarm(nextDay, taskId)
        │（循环直到到期日）
        │
        到期日当天:
        ├─ 更新通知: contentText = "今天到期！"
        ├─ setOngoing(false) ← 可以滑掉了
        ├─ 增加高优先级振动/声音
        └─ 不再调度后续提醒（等用户操作）
```

### 4.3 图标变色流程

```
每日 06:00 AlarmManager 触发 DailyUpdateReceiver
    │
    ├─ 查询所有活跃任务
    ├─ 计算全局最大 UrgencyState
    │
    ├─ 更新桌面 Widget:
    │   WidgetColorMapper.map(urgency) → 颜色值
    │   RemoteViews.setBackgroundColor / setImageViewBitmap
    │   AppWidgetManager.updateAppWidget
    │
    └─ Widget 布局:
        ┌─────────────────┐
        │  🌿 提醒助手     │ ← 背景色随 urgency 变
        │                 │
        │  ○ 浇花   今天！ │ ← 红色标记
        │  ○ 换床单 还剩2天 │ ← 黄色标记
        │  ○ 猫驱虫 还剩8天 │ ← 蓝绿色
        │                 │
        │  [标记完成] [+]  │
        └─────────────────┐
```

**颜色映射表：**

| UrgencyState | 颜色 | 色值 |
|--------------|------|------|
| CALM | 蓝绿色 | `#2E8B57` (SeaGreen) |
| NOTICE | 黄色 | `#FFC107` (Amber) |
| URGENT | 橙色 | `#FF9800` (Orange) |
| CRITICAL | 红色 | `#F44336` (Red) |

---

## 5. UI 页面清单

### 5.1 主页面

| 页面 | 路由 | 内容 | 交互 |
|------|------|------|------|
| **HomeScreen** | `/` | 分类列表（一级） | 点击分类→进入待办列表；长按→编辑/删除；右下 FAB→新建分类 |
| **TaskListScreen** | `/category/{id}` | 该分类下所有待办项（二级） | 每项显示标题+紧急度色块+倒计时文字；FAB→新建待办；点击项→详情；长按→批量操作 |
| **TaskDetailScreen** | `/task/{id}` 或 `/task/new?categoryId={id}` | 新建/编辑待办表单 | 表单字段见下方 |
| **CategoryEditScreen** | `/category/new` 或 `/category/{id}` | 新建/编辑分类 | 名称+图标选择+颜色 |
| **SettingsScreen** | `/settings` | 设置页 | 默认提醒时间、通知声音、备份/恢复入口、关于 |
| **BackupScreen** | `/backup` | 数据备份恢复 | 导出JSON到本地/导入JSON恢复 |

### 5.2 TaskDetailScreen 表单字段

```
┌─────────────────────────────────┐
│  新建待办                       │
│                                 │
│  标题 *  [________________]     │
│  描述    [________________]     │
│  分类 *  [下拉选择]             │
│                                 │
│  ── 提醒设置 ──                 │
│                                 │
│  提醒类型  ○一次性  ○循环       │
│                                 │
│  [一次性时] 到期日期 [日期选择] │
│  [循环时]   间隔 [数值]        │
│             单位 ○天○周○月      │
│             起始日期 [日期选择] │
│                                 │
│  ── 提醒级别 ──                 │
│                                 │
│  ○轻度  到期当天提醒一次        │
│  ○中度  提前3天开始倒计时        │
│  ○重度  提前15天开始倒计时       │
│  ○自定义                       │
│    提前 [___] 天                │
│    频率 ○每天○隔2天○每周        │
│                                 │
│  [保存]                         │
└─────────────────────────────────┘
```

### 5.3 通知交互设计

**通知样式：**

```
轻度通知:
┌─────────────────────────────┐
│ 🌿 提醒助手                  │
│ 浇花 — 今天到期              │
│ [标记完成] [稍后提醒]        │
└─────────────────────────────┘

中度/重度倒计时通知（更新式）:
┌─────────────────────────────┐
│ 🌿 提醒助手                  │
│ 换床单 — 还剩2天             │
│ [标记完成] [稍后提醒▼]       │
│   ▸ 1小时后                  │
│   ▸ 3小时后                  │
│   ▸ 明天                     │
└─────────────────────────────┘

到期当天通知:
┌─────────────────────────────┐
│ 🔴 提醒助手                  │
│ 换床单 — 今天到期！          │
│ [标记完成] [稍后提醒]        │
└─────────────────────────────┘
```

**通知 Action 按钮：**
- `ACTION_COMPLETE` → 直接标记完成（不用打开 app）
- `ACTION_SNOOZE_1H` → Snooze 1小时
- `ACTION_SNOOZE_3H` → Snooze 3小时
- `ACTION_SNOOZE_TOMORROW` → Snooze 到明天 09:00

### 5.4 桌面 Widget 设计

```
4×2 Widget:
┌──────────────────────────┐
│  🌿 提醒助手    [背景色] │ ← 整体背景随全局紧急度变色
│                          │
│  🔴 浇花      今天！     │ ← CRITICAL
│  🟡 换床单    还剩2天    │ ← NOTICE
│  🟢 猫驱虫    还剩8天    │ ← CALM
│                          │
│  [+新建]                  │
└──────────────────────────┘

点击任务行 → TaskDetailScreen
点击 [+新建] → TaskDetailScreen(new)
```

---

## 6. 开发里程碑

### Phase 1 — MVP（2周）

**目标：** 能创建分类和待办，能收到通知

- [ ] 项目初始化：Gradle、Hilt、Room、Compose 基础骨架
- [ ] 数据层：CategoryEntity + TaskEntity + DAO + Repository
- [ ] UI：HomeScreen（分类列表）+ CategoryEditScreen
- [ ] UI：TaskListScreen + TaskDetailScreen（基础表单，仅支持一次性+轻度提醒）
- [ ] 通知：AlarmManager + NotificationManager，单次通知
- [ ] 任务完成：点击通知标记完成，一次性任务归档

**交付物：** 能创建"浇花"待办，到期当天收到一条通知，点完就结束。

### Phase 2 — 循环提醒 + 提醒级别（1.5周）

**目标：** 循环任务、中度/重度倒计时通知

- [ ] 循环提醒：interval_value + interval_unit，完成后自动计算 next_due_date
- [ ] 提醒级别：LIGHT/MEDIUM/HEAVY，倒计时通知（更新式）
- [ ] NotificationChannel 注册（三个级别不同声音/振动）
- [ ] ReminderLogEntity 记录通知历史
- [ ] Snooze 功能：通知 Action 按钮

**交付物：** 创建"换床单 每2周 循环 中度提醒"，提前3天开始倒计时通知。

### Phase 3 — Widget + 图标变色（1周）

**目标：** 核心视觉特色

- [ ] 桌面 Widget：显示最近5条任务，紧急度颜色
- [ ] DailyUpdateReceiver：每日更新 Widget 颜色
- [ ] WidgetColorMapper：UrgencyState → 颜色
- [ ] Widget 交互：点击跳转详情、标记完成按钮

**交付物：** 桌面 Widget 背景色随紧急度变色，列表显示倒计时。

### Phase 4 — 自定义级别 + 备份 + 完善交互（1周）

**目标：** 功能补全

- [ ] 自定义提醒级别：custom_advance_days + custom_notify_freq
- [ ] 备份导出/导入：JSON 格式 + FileProvider 分享
- [ ] 批量标记完成
- [ ] 分类排序（拖拽）
- [ ] 自定义分类图标选择器
- [ ] Settings 页面：默认提醒时间、通知偏好

### Phase 5 — 澎湃OS 适配 + 优化 + 上架准备（0.5周）

- [ ] 澎湃OS 适配要点（见下节）
- [ ] 性能优化：Room 查询索引、批量通知避免卡顿
- [ ] UI polish：动画过渡、深色模式
- [ ] 小米应用商店上架准备（如果需要）

---

## 7. 澎湃OS 适配要点

### 7.1 通知权限

澎湃OS（Android 14+）**强制要求** `POST_NOTIFICATIONS` 运行时权限。

```kotlin
// Android 13+ 必须请求
if (Build.VERSION.SDK_INT >= 33) {
    requestPermission(Manifest.permission.POST_NOTIFICATIONS)
}
```

### 7.2 精确闹钟权限

Android 14+ 新增 `SCHEDULE_EXACT_ALARM` 权限，**默认不授予**。

澎湃OS 上：
- 如果用户没手动授权，`setExactAlarm()` 会抛 SecurityException
- **解决方案：** 使用 `canScheduleExactAlarms()` 检查，未授权时降级到 `setAndAllowWhileIdle()`（不精确但能用）
- 或引导用户去 设置 → 应用 → 提醒助手 → 精确闹钟 开启

```kotlin
val alarmManager = context.getSystemService(AlarmManager::class.java)
if (alarmManager.canScheduleExactAlarms()) {
    alarmManager.setExactAndAllowWhileIdle(...)
} else {
    // 降级：不精确闹钟，误差可能几分钟
    alarmManager.setAndAllowWhileIdle(...)
    // 提示用户去设置中开启精确闹钟
}
```

### 7.3 自启动权限

小米/澎湃OS **默认禁止应用自启动**。这意味着：
- 设备重启后，AlarmManager 注册的闹钟会丢失
- 必须处理 `BOOT_COMPLETED` 广播重新注册所有闹钟

```kotlin
// AndroidManifest.xml
<receiver android:name=".scheduler.BootReceiver">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

BootReceiver 内：查询所有活跃任务，重新调度所有闹钟。

**引导用户开启自启动：**
- 在设置页提示"为确保重启后提醒正常，请前往设置开启自启动"
- 可用 Intent 跳转小米自启动设置页（非公开 API，但实测可用）

```kotlin
// 小米自启动设置页（澎湃OS实测可用，但非官方API，可能随版本变化）
val intent = Intent().apply {
    component = ComponentName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity"
    )
}
```

### 7.4 后台省电限制

小米 MIUI/澎湃OS 的省电策略会杀后台进程：
- WorkManager 比 AlarmManager 更抗省电策略
- **建议：** AlarmManager 精确触发，WorkManager 作为 fallback
- 提醒用户将应用加入"不受省电限制"列表

### 7.5 NotificationChannel 最佳实践

澎湃OS 对 NotificationChannel 的处理和原生 Android 一致，但小米有自定义通知样式：
- 设置 Channel 时指定 `importance`：
  - 轻度：`IMPORTANCE_DEFAULT`
  - 中度：`IMPORTANCE_HIGH`
  - 重度：`IMPORTANCE_HIGH` + 振动
- 通知样式用小米兼容的 `NotificationCompat.Builder`，不要用小米私有 API

### 7.6 Widget 适配

- 澎湃OS 的 Widget 系统和原生 Android 一致
- 小米桌面支持 4×2、4×4 等尺寸，提供 `resizeMode`
- Widget 更新频率：Android 限制最低 30 分钟，但通过 AlarmManager 可以强制刷新

### 7.7 不要碰的坑

| 坑 | 说明 |
|----|------|
| `ShortcutManager.alternateIcon` | Android 不原生支持动态图标，小米也没有公开 API。**用 Widget 实现变色**，不要折腾 alternate icon |
| 小米私有 API | `com.miui.*` 包下的类可能随版本变化，不要依赖 |
| 前台服务通知 | Android 14+ 前台服务类型必须声明，提醒类应用不需要前台服务 |

---

## 附录：核心依赖清单

```kotlin
// build.gradle.kts
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Gson (备份)
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
```

---

## 紧急度配色示例

实际使用场景的颜色变化：

```
无任务到期（平时）:
┌──────────────────────┐
│ 🟢 提醒助手          │  ← 蓝绿色，安心
│ 一切正常，无待办到期  │
└──────────────────────┘

3天内有到期:
┌──────────────────────┐
│ 🟡 提醒助手          │  ← 黄色，注意
│ ○ 换床单 还剩3天     │
└──────────────────────┘

1天内到期:
┌──────────────────────┐
│ 🟠 提醒助手          │  ← 橙色，紧迫
│ ○ 浇花   还剩1天     │
└──────────────────────┘

今天到期:
┌──────────────────────┐
│ 🔴 提醒助手          │  ← 红色，必须处理
│ ○ 浇花   今天到期！  │
└──────────────────────┘
```

---

_Plan 版本: v1.0 | 日期: 2026-05-14_