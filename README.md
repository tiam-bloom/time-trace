# 时迹 (TimeTrace)

一款增强 Android 系统「屏幕使用时间」功能的本地应用。

## 核心功能

### MVP 功能
- APP 使用时长统计（按日 / 周 / 月）
- 解锁次数统计
- 历史数据永久保存（不随系统清理）
- 已卸载 APP 数据保留（包名归档）

### 增强功能
- 每个 APP 的「窗口交互次数」（通过无障碍服务）
- 时间趋势分析
- 使用排行（Top N）
- 使用分布（时间段分析）

### 工具能力
- 数据导出（CSV / JSON）
- 本地数据库备份

## 技术栈

| 模块 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room |
| 使用统计 | UsageStatsManager |
| 点击统计 | AccessibilityService |
| 定时任务 | WorkManager |
| 依赖注入 | Hilt |

## 权限说明

- **使用统计权限** (`PACKAGE_USAGE_STATS`)：用于统计各应用使用时长
- **无障碍服务** (`AccessibilityService`)：用于统计 APP 点击次数（默认关闭）

## 数据安全

所有数据存储在本地数据库，不会上传至任何服务器。

## 构建

```bash
./gradlew assembleDebug
```

## License

MIT
