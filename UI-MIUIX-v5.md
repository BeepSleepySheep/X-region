# XIsoSpoof 5.0（Miuix / XBlocker 风格界面）改造说明与验证记录

> **来源更正（2026-10-24，v7）**：下文 v5 时期的表格里曾把界面来源写成「XBlocker ui/...」，那不准确。按逐行比对与 XBlocker 自己的 `THIRD_PARTY_NOTICES.md`：这些组件（FloatingBottomBar、liquid/*、miuix/animation/*、BlurExt）的**原始来源是 SukiSU-Ultra v4.1.3（GPL-3.0）**，XBlocker 0.3.5 只做了 package/import 适配；Miuix 0.9.3 与 Kyant0/AndroidLiquidGlass 为 Apache-2.0。本工程以 GPL-3.0 分发，完整声明见 `THIRD_PARTY_NOTICES.md` 与 `LICENSE`。旧的「参考 XBlocker / KernelSU manager」表述一并作废。


## 1. 本次改了什么

主界面从原生 View/XML 重写为 Jetpack Compose + Miuix 0.9.3，结构与组件照搬 SukiSU-Ultra v4.1.3
（github.com/SukiSU-Ultra/SukiSU-Ultra）与 SukiSU-Ultra manager，功能与配置存储向后兼容。

- 代码：`app/src/main/java/com/example/xiospoof/MainActivity.kt`（全量重写，约 940 行）
- 新增：`ui/UiPrefs.kt`（`LocalDarkTheme` + 浅/深/跟随系统三态）
- 修改：`ThemeManager.java`（浮动底栏默认开启）、`app/src/main/AndroidManifest.xml`
  （`tools:overrideLibrary="top.yukonga.miuix.kmp.blur"`，因为 miuix-blur 声明 minSdk 33）
- 未改动：`RootBridge`、`ConfigStore`、`Countries` 的既有 API，Xposed hook 侧不变

## 2. 组件对应关系（可逐条回查源码）

| 界面元素 | 使用的组件 | 来源 |
| --- | --- | --- |
| 主题/深浅色/Monet | `ThemeController` + `ColorSchemeMode`（6 种模式） | SukiSU-Ultra v4.1.3 ui/MainActivity.kt |
| 顶栏 | `TopAppBar` + `BlurredBar` | SukiSU-Ultra v4.1.3 ui/util/BlurExt.kt |
| 底栏（实质） | `FloatingBottomBar` + `FloatingBottomBarItem` | SukiSU-Ultra v4.1.3 ui/component/FloatingBottomBar.kt（源自 SukiSU-Ultra → compose-miuix-ui IosLiquidGlassNavigationBar → Kyant0/AndroidLiquidGlass） |
| 底栏（兼容） | `NavigationBar` / `NavigationBarItem` | SukiSU-Ultra v4.1.3 ui/MainActivity.kt |
| 页面卡片 | `Card` + `CardDefaults.defaultColors` | SukiSU-Ultra v4.1.3 ui/ColorPaletteScreenMiuix.kt |
| 开关行 | `SuperSwitch` | KernelSU manager 系列 |
| 国家选择 | `OverlaySpinnerPreference` + `DropdownItem`（弹层，共 46 个常用国家/地区，取自 `Countries.LIST`） | SukiSU-Ultra v4.1.3 ui/ColorPaletteScreenMiuix.kt |
| 进入 LSPosed | `ArrowPreference` | SukiSU-Ultra v4.1.3 ui/SettingsMiuix.kt |
| 液态玻璃 | `miuix-blur`（`rememberLayerBackdrop` + `layerBackdrop`） | SukiSU-Ultra v4.1.3 ui/MainActivity.kt |

底部导航与 XBlocker 相同：4 个 tab（主页 / 应用 / 日志 / 设置），浮动胶囊 + 玻璃高光共用同一套组件。

## 3. 页面内容

- **主页**：左侧大卡为模块状态（170dp 水印图标 + 已启用/未启用/读取中 + 当前配置 + 最近写入），右侧两张指标卡（当前国家 / 配置大小）；
  下方为明细行：模块状态、作用域、当前国家地区、X 版本、Root、最近一次写入，以及刷新 / 打开 X。
  状态由一条 `su -M` 命令读取（`id` / `stat` / `xiso_log.txt`）。
- **应用**：目标包信息、国家/地区下拉、SIM 国家码与 ServiceState 开关、保存并应用、写入状态与配置文件路径。
- **日志**：X 侧 + App 侧合并视图、su -M 命令输入框与执行、3 条只读预置命令。
- **设置**：主题三态 TabRow、Monet、模糊、浮动底栏、液态玻璃、隐藏桌面图标/恢复入口、一键请求作用域、版本与来源说明。

## 4. 实机验证（Redmi，Android 16 / SDK 36）

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| 构建 | `:app:assembleDebug` BUILD SUCCESSFUL，APK 17.7 MB | gradle 9.6.1 输出 |
| 安装 | `adb install -r` Success，versionCode=5 / versionName=5.0 | dumpsys package |
| 四个 tab | 主页/应用/日志/设置均可切换并渲染 | uiautomator dump |
| 国家切换（正向） | 中国台湾 → 日本 → 保存 | X 侧文件 `iso=jp` |
| 国家切换（反向） | 日本 → 美国 → 保存，可逆 | X 侧文件 `iso=us` |
| 开关写盘 | SIM / ServiceState 开关切换后保存 | 文件里 `fake_sim` / `fake_service_state` 随之变化 |
| hook 生效 | 重启 X 后 8 个方法 hook 成功，`配置已更新：iso=jp` / `iso=us`，`已注入 com.twitter.android` | logcat（LSPosedFramework） |
| 日志页命令 | 视图尾部出现 `$ cat … → iso=… fake_sim=… fake_service_state=…` | 截图 + 应用日志 |
| 液态玻璃 | 开/关：底栏区域 19.6% 像素变化（平均差 7.7/765，最大 447），底栏上方与顶栏 0% | 截图像素比对 |
| 深浅主题 | 内容区平均亮度 243.0（浅） vs 43.2（深） | 截图像素统计 |
| 隐藏桌面图标 | `resolve-activity` → No activity found；`pm enable` 后恢复 | adb + 应用日志 |
| 恢复入口 | 点击后日志记 `桌面图标已恢复`，图标回到桌面 | 应用日志 |
| 一键请求作用域 | 弹层提示行出现「无法拉起 LSPosed（ActivityNotFoundException）；请手动打开 LSPosed 管理器 → 模块 → XIsoSpoof → 勾选 X」 | 界面 dump + 应用日志 |

收尾状态：X 侧配置已恢复为 `iso=us / fake_sim=false / fake_service_state=true`，App 侧 shared_prefs 与之一致。

## 5. 已知限制

1. **一键请求作用域在本机拉不起 LSPosed**：`am start -a org.lsposed.manager.LAUNCH_MANAGER -d module://com.example.xiospoof:0`
   返回 `unable to resolve Intent`，应用内调用抛 `ActivityNotFoundException`；已捕获并在页面上给出文字指引，不会闪退。
2. **主页“已注入”由 X 侧日志推出（非实时）**：若长期未重启 X，显示的是最后一次注入记录；日志页可看时间戳。
3. **开关行需要点右侧开关本体**（`SuperSwitch` 与 KernelSU 同款行为），点行空白处不会切换。
4. **模糊 / 液态玻璃需要 Android 13+**：低版本开关为禁用状态；miuix-blur 声明 minSdk 33，已用 `tools:overrideLibrary` 放宽。
5. **安装包位置**：`app/build/outputs/apk/debug/app-debug.apk`，另存一份到 `下载\xiso-v5.apk`；手机上安装的即同一份。

## 6. 隐藏桌面图标后的恢复方式

```
adb shell pm enable com.example.xiospoof/.MainActivity
```
或在“设置 → 恢复入口”点一下（需应用仍在前台 / 最近任务里）。

## 7. 补充验证（15:55-16:02）

- 反向下拉切换（日本 → 美国）成功，X 侧文件从 `iso=jp` 变为 `iso=us`，证明切换可逆。
- 应用页两个开关（SIM 国家码 / ServiceState）点击后保存，`fake_sim`、`fake_service_state` 随开关状态同步变化。
- 重启 X 后 logcat：`配置已更新：iso=us fake_sim=false fake_service_state=true`、`已注入 com.twitter.android，当前 iso=us`。
- 设置页“恢复入口”点击后日志：`[App] 桌面图标已恢复`。
- 设置页“一键请求作用域”点击后日志：`[App] 一键请求作用域：无法拉起 LSPosed（ActivityNotFoundException）；请手动打开 LSPosed 管理器 → 模块 → XIsoSpoof → 勾选 X`，页面上同步多出一行同样的提示。
- 清理：手机 `/sdcard` 下的截图与 dump 已全部删除；工程目录下的临时编译脚本与构建日志已删除。


---

# v6 更新（2026-10-24，versionCode 6 / 6.0）

## 1. 日志页：只读命令，不再允许自由输入
- 删除「命令行」Card 的 TextField 与「执行」按钮（`var cmd` 状态一并移除）。
- 新卡「验证命令（只读）」只有 3 行预置命令，点击即执行并把输出追加到下方日志卡：
  1. 「读取 X 侧配置」→ `cat /data/data/com.twitter.android/files/xiso_config.txt`
  2. 「查看 X 侧日志」→ `tail -n 30 /data/data/com.twitter.android/files/xiso_log.txt`
  3. 「查看最近一次注入」→ `grep -F '已注入' /data/data/com.twitter.android/files/xiso_log.txt | tail -n 1`
- 实测：点第 3 行，日志卡追加 `$ grep -F ... | tail -n 1` + `10-24 16:13:09 [XIsoSpoof] 已注入 com.twitter.android… iso=cn`；Uiautomator 节点中 EditText 数量 = 0。

## 2. 设置页：主题改为下拉选择
- 用 `OverlaySpinnerPreference` 替换原 TabRow：items = 跟随系统 / 浅色 / 深色，`selectedIndex` 绑定 `colorMode`，`onSelectedIndexChange` 回调 `onColorMode`。
- 实测：点「主题」弹出三项（跟随系统 / 浅色 / 深色）；选「深色」后行内文案与该行右侧选中值立即变为「深色」，`color_mode` 落盘为 2；杀进程重启后仍为「深色」；再选回「跟随系统」落盘为 0。
- 注：本机（Android 16 / HyperOS）SharedPreferences 实际落盘路径被重定向到 `/data/misc/apexdata/<uuid>/prefs/com.example.xiospoof/xiso_ui.xml`，`/data/data/.../shared_prefs/xiso_ui.xml` 是旧副本，校验时以 apexdata 路径为准。

## 3. 卡片标题被圆角裁掉
- 每张卡的标题 Text 加 `modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp)`（设置页「外观」卡、日志页「验证命令（只读）」卡）。
- 实测：标题「验证命令（只读）」节点由 x=36px 移到 x=79px；截图像素扫描首字左边缘 x=81px，已越过卡片圆角弧区，不再被裁。

## 4. LSPosed 作用域请求（重点项）

### 实现（照 XBlocker 0.3.5 的做法）
- 新增 `XposedServiceProvider`，authority = `com.example.xiospoof.XposedService`，`call()` 只接受 uid 0/1000 的 `SendBinder`，从 extras 取 `"binder"` 交给 `XposedServiceClient`。
- 新增 `XposedServiceClient`：按 libxposed AIDL 序号（`getScope` = 0x0B、`requestScope` = 0x0C，回调 `approved`/`failed`）transact；另提供 `attach(context)` 走 `getSystemService("LSPosed")` 备用路径。
- 设置页新增「请求作用域」行 + 「LSPosed 服务」状态行 + 「上次结果」行；每次点按都写应用日志（含 scope 列表与连接状态）。

### 本机排查结果（未能弹出 LSPosed 通知）
1. 点按后 provider 没有收到任何调用：logcat 无 `provider call:` 行（该行在 uid 校验之前就会打印）。
2. 应用进程内 `getSystemService("LSPosed")` 返回 null：`D XIsoSpoofScope: getSystemService(LSPosed) -> null`。
3. 解包 `/data/adb/modules/zygisk_lsposed` 核查：
   - `daemon.apk!classes.dex`：`sendbinder` = 0 次，`io.github.libxposed.service.IXposedService` / `IXposedScopeCallback` 各 1 次（服务端在）；
   - `manager.apk` 全部 dex：`sendbinder` / `xposedservice` / `scopecallback` 均 0 次；
   - `framework.dex`：参考 `io.github.libxposed.api.*` 共 25 处，但 `sendbinder` / `xposedservice` 均 0 次；
   - `zygisk/arm64-v8a.so`、`lib/libpreload64.so`：`SendBinder` 0 次。
4. 对照：同机安装的 XBlocker 0.3.2（本身就是这个机制）启动后 logcat 也没有任何 `XBlockerScope` 日志。

### 结论与现状
- 该 LSPosed（v2.1.1 / 7790）具备 IXposedService 服务端，但没有把 libxposed 服务 binder 投递给模块应用的代码路径（无 `SendBinder` 标识，且模块进程拿不到 "LSPosed" 系统服务），所以“点一下 → LSPosed 发通知确认 → 自动加入作用域”在本机无法完成；这不是 XIsoSpoof 的实现缺陷。
- 代码保留：一旦换到会投递 binder 的 LSPosed 构建，点「请求作用域」即可生效；届时界面会显示「已向 LSPosed 发送请求，请在通知栏确认…」。
- 目前作用域的可行做法仍是 LSPosed 管理器里手动勾选 `com.twitter.android`（本机已勾选，hook 日志 `[XIsoSpoof] 已注入 com.twitter.android` 正常）。

---

# v7 更新（2026-10-24，versionCode 7 / 7.0）

## 1. 移除「一键请求 LSPosed 作用域」
- 删除 `XposedServiceProvider.kt`（内含 `XposedServiceProvider` 与 `XposedServiceClient`）。
- `AndroidManifest.xml` 删除 `<provider android:name=".XposedServiceProvider" android:authorities="com.example.xiospoof.XposedService" …>`、对应的注释，以及 `<queries>` 中的 `org.lsposed.manager`（该条目仅作用于作用域判定，代码中再无引用）；`com.twitter.android` 的 queries 保留（`isInstalled()` 使用）。
- 设置页删除整张卡：『请求作用域』行、『LSPosed 服务』状态行、『上次结果』行，以及 `XposedServiceClient.attach()` 的两个调用点（onCreate 与点击处理）和相关的 ConfigStore 日志文案。

## 2. 署名与许可（按实际代码对照）
- 逐行比对本次工程与 `_ref/XBlocker` 中间名文件（Compare-Object，仅文件头/包名差异）：CombinedBackdrop 41/40、InnerShadow 162/161、Lens 218/217、Vibrancy 17/16、FloatingBottomBar 460/451、DampedDragAnimation 148/146、InteractiveHighlight 115/113、DragGestureInspector 86/85（前者为总行数，后者为完全相同行数）。
- XBlocker 自身的 `THIRD_PARTY_NOTICES.md` 写明：上述 UI 文件是从 **SukiSU-Ultra v4.1.3（commit 0ca744a，GPL-3.0）** 移植的，XBlocker 只做 package/import 适配与状态/控件拆分。因此：
  - **SukiSU-Ultra v4.1.3（GPL-3.0）** 是这些界面组件的原始来源（已核对 v4.1.3 的 LICENSE 首行 = GNU GPL-3.0）；
  - **XBlocker 0.3.5** 本身 `LICENSE` = MIT、`COPYING` = GPL-3.0，README 第 154 行声明「自身代码 MIT、整体 GPL-3.0」，本工程取用的是其适配后的版本，保留归属；
  - **Miuix（compose-miuix-ui）0.9.3** = Apache-2.0（依赖 POM `<licenses>` 与仓库 url 已核对）；
  - **Kyant0/AndroidLiquidGlass** = Apache-2.0（仓库页标注）；
  - **Xposed API 82** = Apache-2.0（artifact POM `<licenses>`），仅编译期依赖；
  - **KernelSU**：本地 `_ref/KernelSU` 为 GPL-3.0，但工程源码无任何引用，列入「参考但未使用」。
- 新增 `LICENSE`（GPL-3.0 全文，35723+ 字节）与 `THIRD_PARTY_NOTICES.md`；`README.md` 重写许可与署名段；「关于」页改为逐项列出上游项目名 / 仓库地址 / 许可证 / 用到的部分。
- 代码注释中「照搬 / 与 XBlocker 一致」类表述改为指向真实来源（MainActivity 头注释重写为 SukiSU-Ultra v4.1.3 → XBlocker 适配 → Miuix / AndroidLiquidGlass）。

## 3. 保留的行为
- 主页「已注入」仍由 X 侧日志 `xiso_log.txt` 推断（模块无法直接查询 LSPosed 注入状态），该限制保留。
- 日志页 3 个只读预置命令、主题下拉、卡片标题留白不变。
