# XRegion

XRegion 是一个面向 Android 端 X（原 Twitter）的国家码修改器，基于 Jetpack Compose 与 Miuix 构建，以 LSPosed 模块形式运行。

**所有修改仅针对 X（原 Twitter），不会影响其他应用。**

> 本项目仅用于个人学习、开发调试与 UI 研究，所有修改仅针对 X。请遵守当地法律法规及 Twitter 的服务条款，不要用于规避服务限制。

## 功能

- **作用范围**：仅对 X生效，不修改其他应用
- **地区选择**：内置完整的地区列表，方便选择
- **应用与状态**：一键应用选择，主页集中展示当前状态与模块信息
- **运行日志**：只显示最近一次运行记录，避免历史日志堆积
- **主题**：跟随系统 / 浅色 / 深色，支持 Monet 动态取色
- **界面风格**：整体采用 Miuix风格，界面参考 KernelSU 支持浮动底栏、液态玻璃、预测性返回手势

## 原理

XRegion 以 LSPosed 模块形式随 X进程加载，不修改 Twitter 的安装包：

1. **进程内加载**：X启动时，框架将模块加载到其进程；模块初始化后读取本机保存的地区配置。
2. **运行时替换**：模块拦截 X中以下读取地区字段的调用：
   - `TelephonyManager#getNetworkCountryIso`
   - `TelephonyManager#getNetworkCountryIsoForPhone`
   - `TelephonyManager#getNetworkCountryIsoForSubscription`
   - `TelephonyManager#getNetworkCountryIsoForDefaultDataSubscription`
   - `TelephonyManager#getSimCountryIso`
   - `TelephonyManager#getSimCountryIsoForPhone`
   - `TelephonyManager#getSimCountryIsoForSubscription`
   - `TelephonyManager#getNetworkOperator` / `TelephonyManager#getSimOperator`（同步返回的 MCC）
   - `ServiceState#getCountryIso`
   - `SubscriptionInfo#getCountryIso` / `SubscriptionManager#getCountryIso`（系统存在时）

   返回值替换为用户选择的地区代码；SIM 相关调用与 `ServiceState` 可由界面开关单独控制；缺少对应 MCC 时保持原值。
3. **配置共享**：模块界面负责写入配置，X进程通过本机共享通道读取；只对 LSPosed 中勾选的 X生效。
4. **日志**：仅保留最近一次会话的加载状态与生效值，便于本地排查，不写入远程服务。
5. **生效**：X 账户界面下的 “连接自” 不会即时生效，大概有约半个月的冷却期
6. **界面层**：基于 Jetpack Compose + Miuix；主题、模糊、浮动底栏、液态玻璃与预测性返回手势属于界面能力，不参与上述替换逻辑。

## 案例

本项目已在本人 X 账号 @beepsleepysheep 上得到验证：X 原先显示为“China Android App”，在修改国家码并等待约半个多月后，现显示为“Taiwan Android App”，尽管本人始终在中国大陆生活，从未到过台湾地区。

## 环境要求

- Android 8.0（API 26）及以上
- 已安装并启用 LSPosed
- 构建环境：JDK 17+、Android SDK 37、Gradle 9.x

## 安装

1. 下载 Release 中的 APK，或按下方说明自行构建
2. 在 Root 管理器（如KernelSU，SukiSU-Ultra等）中给予 XRegion Root 权限
3. 在 LSPosed 中启用 XRegion，并仅勾选 X
4. 重启 X
5. 打开 XRegion，选择地区并应用

## 构建

使用 Android Studio 打开工程，或在本机已安装 Gradle 9.x 时执行：

```bash
gradle :app:assembleDebug
```

产物位于 `app/build/outputs/apk/debug/`。

## 隐私

- 未申请网络权限，不收集、不上传任何数据
- 配置与日志仅保存在本机
- 应用内「关于 → 隐私说明」提供完整说明

## 许可 / License

本工程以 **GNU GPL-3.0** 分发，全文见 [`LICENSE`](LICENSE)。

开源许可只保留三项：

- **第三方来源与修改说明**：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- **SukiSU · GNU GPL v3**：[`LICENSE`](LICENSE)
- **Miuix / AndroidX · Apache 2.0**：[`app/src/main/assets/licenses/Apache-2.0.txt`](app/src/main/assets/licenses/Apache-2.0.txt)

按 GPL-3.0 要求，分发二进制时须一并提供对应源码。

## 致谢

- X @k1uchanQ
- Xposed API
- AndroidX / Jetpack Compose
- SukiSU-Ultra
- Miuix
- AndroidLiquidGlass


## 免责声明

本项目仅针对 X，用于学习、开发调试与界面研究。使用者应自行确保使用行为符合当地法律法规及 Twitter 的服务条款。作者不对因使用本项目产生的任何直接或间接损失负责。
