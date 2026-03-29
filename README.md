# IPCheck

一个功能完整的Android网络设备扫描应用，采用Material You 3设计风格。

## 功能特性

### 核心功能
- **网络设备扫描**：扫描局域网内的所有设备（支持IPv4和IPv6）
- **设备类型识别**：自动识别设备类型（Android、Mac、Windows、Linux、路由器、光猫、未知）
- **设备名称获取**：通过多种方法获取设备名称（DNS、NetBIOS、HTTP指纹识别）
- **实时在线状态**：实时监控设备的在线状态

### 用户界面
- **Material You 3设计**：采用最新的Material Design设计规范
- **引导流程**：
  - 欢迎页面（首次使用时显示）
  - 使用声明（15秒倒计时，10次点击可强制跳过）
  - 权限中心（卡片式布局）
  - 主界面
- **关于页面**：
  - 应用信息展示
  - 检查更新功能
  - GitHub仓库链接
  - 开发者信息
- **设备详情**：点击设备卡片查看详细信息
- **停止扫描**：支持强制停止扫描操作

### 权限管理
- 网络访问权限
- WiFi状态权限
- 网络状态权限
- 精确位置权限（用于获取WiFi信息）
- 获取设备信息权限
- 存储权限（用于下载更新）
- 忽略电池优化权限

### 更新功能
- **自动检查更新**：从GitHub获取最新版本信息
- **内置下载器**：
  - 实时下载进度显示
  - 下载速度显示（每秒更新）
  - 已下载大小/总大小显示
- **镜像站支持**：
  - 自动尝试多个GitHub镜像站
  - 重试功能
  - 浏览器下载备选
- **错误处理**：
  - 使用镜像站下载
  - 重试下载
  - 用浏览器下载
  - 复制下载链接
  - 复制错误信息

## 技术栈

### 开发语言
- **Kotlin**：主要开发语言

### UI框架
- **Jetpack Compose**：现代声明式UI框架
- **Material You 3**：最新Material Design系统

### 架构模式
- **MVVM**：Model-View-ViewModel架构
- **StateFlow**：响应式状态管理
- **协程**：异步编程

### 网络技术
- **子网扫描**：遍历局域网IP地址
- **端口检测**：检测常用端口识别设备类型
- **DNS解析**：反向DNS查询
- **NetBIOS**：Windows设备名称解析
- **HTTP指纹**：通过HTTP头识别设备

## 项目结构

```
app/
├── src/
│   └── main/
│       ├── java/com/timome/ipcheck/
│       │   ├── MainActivity.kt           # 主Activity
│       │   ├── model/
│       │   │   ├── AppScreen.kt          # 应用屏幕定义
│       │   │   ├── Device.kt             # 设备数据模型
│       │   │   ├── DeviceType.kt         # 设备类型枚举
│       │   │   └── Permission.kt         # 权限定义
│       │   ├── network/
│       │   │   └── NetworkScanner.kt     # 网络扫描器
│       │   ├── ui/
│       │   │   ├── screens/
│       │   │   │   ├── AboutScreen.kt         # 关于页面
│       │   │   │   ├── MainScreen.kt          # 主界面
│       │   │   │   ├── PermissionCenterScreen.kt  # 权限中心
│       │   │   │   ├── TermsOfUseScreen.kt      # 使用声明
│       │   │   │   └── WelcomeScreen.kt         # 欢迎页面
│       │   │   └── theme/
│       │   │       └── Color.kt         # 颜色主题
│       │   └── viewmodel/
│       │       ├── MainViewModel.kt      # 主界面ViewModel
│       │       ├── OnboardingViewModel.kt   # 引导流程ViewModel
│       │       └── PermissionViewModel.kt    # 权限ViewModel
│       ├── res/
│       │   ├── drawable/                 # 图标资源
│       │   ├── mipmap-*/                 # 应用图标
│       │   ├── values/                   # 资源值
│       │   └── xml/                     # XML配置
│       └── AndroidManifest.xml          # 应用清单
├── build.gradle.kts                    # 应用级构建配置
└── proguard-rules.pro                  # ProGuard规则
```

## 权限说明

| 权限 | 用途 | 必需 |
|------|------|------|
| INTERNET | 网络访问 | 是 |
| ACCESS_WIFI_STATE | 访问WiFi状态 | 是 |
| ACCESS_NETWORK_STATE | 访问网络状态 | 是 |
| ACCESS_FINE_LOCATION | 精确位置（获取WiFi信息） | 是 |
| READ_PHONE_STATE | 获取设备信息 | 是 |
| WRITE_EXTERNAL_STORAGE | 存储权限（下载更新） | 是 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 忽略电池优化 | 否 |

## GitHub镜像站

应用支持以下GitHub下载加速镜像站：

1. https://gh-proxy.com
2. https://mirror.ghproxy.com
3. https://ghps.cc
4. https://gh.api.99988866.xyz
5. https://ghdl.feizhuqwq.com

当下载失败时，会自动依次尝试这些镜像站。

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK 34 (Android 14)
- Gradle 9.2.1
- Kotlin 1.9.0

## 构建步骤

### 1. 克隆项目
```bash
git clone https://github.com/Timome-Sudo/IPcheck.git
cd IPcheck
```

### 2. 构建Debug版本
```bash
./gradlew assembleDebug
```

### 3. 构建Release版本
```bash
./gradlew assembleRelease
```

### 4. 安装到设备
```bash
./gradlew installDebug
```

## 依赖配置

项目使用以下镜像源以加速下载：

```kotlin
repositories {
    google()
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
}
```

## 开发者信息

- **开发者**：timome
- **GitHub**：https://github.com/Timome-Sudo
- **项目地址**：https://github.com/Timome-Sudo/IPcheck

## 许可证

本项目采用 MIT 许可证。详见 LICENSE 文件。

## 更新日志

### v1.0.0 (2026-03-29)
- 初始版本发布
- 实现基础网络设备扫描功能
- 支持设备类型识别
- 支持设备名称获取
- 实现Material You 3设计
- 完整的引导流程
- 权限管理系统
- 自动更新功能
- 镜像站下载支持

## 贡献指南

欢迎提交Issue和Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 联系方式

如有问题或建议，请通过以下方式联系：

- GitHub Issues：https://github.com/Timome-Sudo/IPcheck/issues
- 邮箱：[待补充]

## 免责声明

本应用仅用于个人网络设备管理，用户需对使用本应用的行为承担全部责任。请确保扫描的设备为自有或已获授权设备，未经授权扫描他人设备可能构成违法行为。所有扫描到的设备信息仅保存在本地设备上，不会上传到任何服务器。

---

**注意**：本应用按"现状"提供，不提供任何明示或暗示的保证。开发者不对使用本应用造成的任何损失负责。