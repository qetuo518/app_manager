# DCL 软件管理

基于 Shizuku / Root 权限的 Android 应用管理工具，支持强卸载、冻结管理、Shell 执行等功能。

## 功能特性

- **强卸载**：静默卸载应用，自动关闭无障碍服务与设备管理员权限
- **冻结管理**：冻结 / 解冻已安装应用
- **Shell 执行器**：支持 `sh` / `Shizuku` / `Root` 三种模式执行任意命令
- **双权限模式**：同时支持 Shizuku（uid=2000）和 Root（uid=0）两种方式

## 系统要求

- Android 7.0 (API 24) 及以上
- Shizuku 模式：需安装并运行 [Shizuku](https://github.com/RikkaApps/Shizuku)
- Root 模式：设备已获取 root 权限

## 构建

```bash
# 克隆仓库
git clone https://github.com/qetuo518/app_manager
cd app_manager

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/`。

## 依赖说明

- [Shizuku SDK](https://github.com/RikkaApps/Shizuku) (本地 AAR，放置于 `app/libs/`)
  - `api-12.2.0.aar`
  - `provider-12.2.0.aar`
  - `shared-12.2.0.aar`
  - `aidl-12.2.0.aar`

## 项目结构

```
app/src/main/java/dclnb/appmanager/
├── MainActivity.java           # 主入口，跳转至模式选择
├── ModeSelectionActivity.java  # Shizuku / Root 模式选择与权限申请
├── ManagerActivity.java        # 主管理界面（功能入口）
├── UninstallActivity.java      # 强卸载模块
├── FreezeActivity.java         # 冻结/解冻管理模块
└── ShellExecutorActivity.java  # Shell 命令执行器
```

## 许可

[MIT License](LICENSE)