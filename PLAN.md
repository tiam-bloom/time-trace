# 📱 项目计划书：Android 使用行为统计增强工具

## 一、项目概述

项目名：时迹（TimeTrace）

### 1.1 项目目标

开发一款仅面向高版本 Android 的本地应用，用于增强系统“屏幕使用时间”功能，解决以下问题：

* ✅ 已卸载 APP 使用记录丢失
* ✅ 系统历史数据自动清理
* ✅ 数据分析维度不足
* ✅ 缺乏交互行为统计（点击次数）

---

### 1.2 核心功能

#### ✅ 基础能力（MVP 必须）

* APP 使用时长统计（按日 / 周 / 月）
* 解锁次数统计
* 历史数据永久保存（不随系统清理）
* 已卸载 APP 数据保留（包名归档）

#### ⭐ 增强能力

* 每个 APP 的“窗口交互次数”（可开关）
* 时间趋势分析（曲线图）
* 使用排行（Top N）
* 使用分布（时间段分析）

#### 📦 工具能力

* 数据导出（CSV / JSON）
* 本地数据库备份

---

## 二、技术架构设计

### 2.1 总体架构（分层）

```
UI层（Jetpack Compose）
    ↓
ViewModel层（状态管理）
    ↓
Domain层（统计/分析逻辑）
    ↓
Data层
    ├── UsageStats采集
    ├── Accessibility采集（点击）
    ├── 本地数据库（Room/SQLite）
```

---

### 2.2 核心技术选型

| 模块   | 技术                              |
| ---- | ------------------------------- |
| UI   | Jetpack Compose                 |
| 数据库  | Room（基于 SQLite）                 |
| 使用统计 | UsageStatsManager               |
| 点击统计 | AccessibilityService            |
| 图表   | MPAndroidChart / Compose Charts |
| 导出   | CSV / JSON                      |

---

## 三、核心模块设计

### 3.1 使用时长采集模块

基于：
👉 `UsageStatsManager`

#### 数据来源：

* `queryUsageStats()`
* `queryEvents()`

#### 推荐策略：

* 使用 `UsageEvents` 解析前后台切换
* 精确计算每个 APP 使用时长

#### 关键事件：

* `MOVE_TO_FOREGROUND`
* `MOVE_TO_BACKGROUND`

---

### 3.2 点击统计模块（关键难点）

基于：
👉 `AccessibilityService`

#### 监听事件：

* `TYPE_VIEW_CLICKED`
* `TYPE_WINDOW_STATE_CHANGED`

#### 数据策略：

* 仅记录：

  * 包名
  * 时间戳
* 不解析 UI 结构（避免复杂性和风险）

#### 开关设计：

```text
默认关闭 → 用户手动开启
```

---

### 3.3 解锁次数统计

两种方式：

#### ✅ 推荐方案

监听：

* `ACTION_USER_PRESENT`

#### 或：

* `KeyguardManager`

---

### 3.4 数据持久化模块（重点）

使用：
👉 Room（SQLite）

#### 表设计（核心）

##### 1️⃣ App信息表（解决卸载问题）

```sql
app_info
---------
id
package_name
app_name
first_seen_time
last_seen_time
is_uninstalled
```

---

##### 2️⃣ 使用记录表

```sql
usage_record
--------------
id
package_name
start_time
end_time
duration
date
```

---

##### 3️⃣ 点击记录表

```sql
click_record
--------------
id
package_name
timestamp
date
```

---

##### 4️⃣ 解锁记录

```sql
unlock_record
--------------
id
timestamp
date
```

---

### 关键设计点

👉 **永远用 package_name 做主键，不依赖系统**
👉 即使 APP 卸载：

* 仍保留历史数据
* 标记 `is_uninstalled = true`

---

## 四、数据采集策略（避免系统清理）

### 问题本质：

系统 `UsageStats` 数据会被清理（通常几天/几周）

---

### ✅ 解决方案

#### 定时任务（核心）

使用：

* `WorkManager`

#### 频率建议：

* 每 15 分钟 / 每小时

#### 工作内容：

1. 拉取最近一段时间 UsageEvents
2. 增量写入数据库
3. 去重（防重复记录）

---

### 去重策略

```text
(package_name + start_time) 唯一约束
```

---

## 五、数据分析模块

### 5.1 统计维度

* 日 / 周 / 月使用时长
* Top 使用 APP
* 使用时间分布（24h）
* 点击次数排行
* 解锁频率

---

### 5.2 分析方式

建议在：
👉 Domain 层完成（而不是 SQL 复杂聚合）

---

## 六、数据导出设计

支持：

### 格式

* CSV（Excel友好）
* JSON（可扩展）

---

### 示例

#### CSV

```csv
date,package,usage_time,click_count
2026-04-27,com.xxx,1200,45
```

---

### 实现方式

* 直接读 SQLite
* 写入文件
* 使用系统分享（Share Intent）

---

## 七、UI设计建议（重点）

你强调“UI要好看”，这里给你方向：

### 7.1 技术

👉 Jetpack Compose + Material 3

---

### 7.2 页面结构

#### 首页（Dashboard）

* 今日使用时间
* 解锁次数
* Top 3 APP
* 使用趋势图

---

#### APP详情页

* 使用时长曲线
* 点击次数
* 历史趋势

---

#### 统计页

* 排行榜
* 时间分布（热力图）

---

#### 设置页

* 点击统计开关
* 数据导出
* 数据清理

---

### 7.3 UI风格建议

* 深色模式优先
* 卡片式布局
* 动画（Compose Motion）

---

## 八、关键风险点

### ⚠️ 1. UsageStats 精度问题

* 有延迟
* 某些系统限制

👉 解决：接受误差 + 增量采集

---

### ⚠️ 2. Accessibility 权限

* 用户敏感
* 可能被系统限制

👉 解决：

* 默认关闭
* 明确说明用途

---

### ⚠️ 3. 电量消耗

* 频繁采集

👉 解决：

* WorkManager + 合理频率

---

## 九、迭代计划

### 🚀 Phase 1（MVP）

* 使用时长
* 解锁次数
* 本地存储
* 基础 UI

---

### 🚀 Phase 2

* 点击统计
* 排行榜
* 数据导出

---

### 🚀 Phase 3

* 图表分析
* UI优化
* 性能优化

---

## 十、可扩展方向（以后可以玩大的）

* AI 使用行为分析（比如预测沉迷）
* 使用习惯评分
* 自动生成报告（周报）

---

## ✅ 总结一句话

这个项目本质是：

> **“一个本地行为数据采集 + 轻量分析 + 可视化工具”**

技术难点不在“写代码”，而在：
👉 数据采集策略 + 权限处理 + 数据建模
