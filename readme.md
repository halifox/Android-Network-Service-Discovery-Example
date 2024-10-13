# Android Network Service Discovery (NSD) 示例项目

本项目是一个基于 Android 的 Network Service Discovery（NSD）示例应用程序，演示了如何使用 NSD 来在局域网内发现并注册服务。NSD 使用 mDNS（Multicast DNS）协议，可用于发现局域网中的其他设备服务，如打印机、文件共享、Web 服务等。

## 项目功能
本项目展示了以下内容：
- 注册服务（Service）以供局域网中的其他设备发现
- 搜索和发现局域网中已注册的服务
- 处理服务注册、发现、失效等事件

## 环境配置
- **Android Studio**：建议使用最新版本
- **最低 SDK**：API Level 16（Android 4.1）
- **编程语言**：Kotlin

## 目录结构
- `MainActivity.kt`：包含主要的 NSD 逻辑和监听器实现
- `AndroidManifest.xml`：添加了必要的网络权限
- `README.md`：本文件，提供了项目的详细说明

## 快速开始
### 1. 克隆本项目

### 2. 打开项目并添加必要的权限
在 `AndroidManifest.xml` 中添加网络和位置权限：
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```
> 注意：从 Android 6.0（API Level 23）开始，应用需要在运行时请求位置权限，以便扫描 Wi-Fi 网络。

### 3. 注册服务
在 `MainActivity.kt` 中初始化 NSD，并配置服务名称、类型和端口：
```kotlin
val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

val serviceInfo = NsdServiceInfo().apply {
    serviceName = "MyService" // 设置服务名称
    serviceType = "_http._tcp." // 设置服务类型
    port = 12345 // 设置端口号
}

val registrationListener = object : NsdManager.RegistrationListener {
    override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
        // 注册成功
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        // 注册失败
    }

    override fun onServiceUnregistered(arg0: NsdServiceInfo) {
        // 取消注册
    }

    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        // 取消注册失败
    }
}

nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
```

#### 服务配置要求
1. **`setServiceName(String serviceName)`**
    - **唯一性**: 服务名称在同一网络的同一服务类型下必须是唯一的。
    - **字符限制**: 推荐使用 0 到 15 个字符的名称，避免使用空格和特殊字符。
    - **示例**: 常见名称可以是 `MyService`、`ChatAppService` 等。

2. **`setServiceType(String serviceType)`**
    - **格式要求**: 必须以下划线 `_` 开头，格式通常是 `_service._protocol`，例如 `_http._tcp.`。
    - **服务类型**: 使用标准类型，如 `_http._tcp.`、`_ftp._tcp.`，或自定义服务类型。
    - **示例**: 常用类型如 `_http._tcp.` 表示 HTTP 服务、`_ftp._tcp.` 表示 FTP 服务。

3. **`setPort(Int port)`**
    - **端口号范围**: 必须在 1 到 65535 之间。
    - **未被占用**: 确保指定的端口未被其他服务使用，避免冲突。
    - **示例**: 常见端口号如 80（HTTP）或 443（HTTPS），但可以使用任意未被占用的端口。

### 4. 搜索服务
要发现网络中的服务，可以使用以下代码：
```kotlin
val discoveryListener = object : NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(serviceType: String) {
        // 开始发现服务
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        // 找到一个服务
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        // 一个服务丢失
    }

    override fun onDiscoveryStopped(serviceType: String) {
        // 停止发现
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        // 开始发现失败
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        // 停止发现失败
    }
}

nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
```

### 5. 停止服务注册和发现
在应用不再需要服务时，需要停止服务注册和发现：
```kotlin
// 停止注册
nsdManager.unregisterService(registrationListener)

// 停止发现
nsdManager.stopServiceDiscovery(discoveryListener)
```

## 注意事项
- **权限要求**：Android 6.0+ 需要在运行时请求位置权限。
- **端口号设置**：服务的端口号需在 1 到 65535 之间，且未被其他服务占用。
- **同一网络**：NSD 仅在同一 Wi-Fi 网络中有效。

## 相关链接
- [NSD 官方文档](https://developer.android.com/training/connect-devices-wirelessly/nsd)
- [NsdManager API 参考](https://developer.android.com/reference/android/net/nsd/NsdManager)
- [网络权限设置](https://developer.android.com/training/basics/network-ops/connecting)
- [运行时权限官方文档](https://developer.android.com/training/permissions/requesting)

## 总结
通过本示例，您可以了解 Android 中 NSD 的基本使用方法，包括如何注册、发现和停止服务。NSD 是一个非常方便的工具，可以帮助您在局域网内自动发现和连接设备，是构建 IoT（物联网）应用和本地设备网络的理想选择。