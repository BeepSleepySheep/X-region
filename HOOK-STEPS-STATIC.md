# Hook 七步改造 · 纯静态验证记录（v11.0 / versionCode 11）

生成时间：2026-10-24（Asia/Shanghai）。本文档记录「按优先顺序推进 Hook 七步」在本轮**纯静态**条件下的实现与验证结果。

> 本轮未执行任何 adb / 设备命令，未安装、未启停 X、未读取手机上的配置/日志/截图，设备状态（iso=tw）未改动。
> 因此本文档中**没有任何一项可以称为「已在真机生效」**；凡须上机才能确认的内容集中列在第 9 节待验清单。

## 1. 交付物

| 项 | 值 |
| --- | --- |
| 本地 APK | `app/build/outputs/apk/debug/app-debug.apk` |
| 大小 | 17,832,706 字节（约 17.0 MB） |
| SHA-256 | `41B866F02E6331F0B64448668B6419B032F72DD51456DCD9F92D37254FA0870D` |
| versionCode / versionName | 11 / "11.0" |
| 构建命令 | `gradle :app:assembleDebug`（Gradle 9.6.1，Debug） |
| 构建结果 | `BUILD SUCCESSFUL`（日志 `build_s7.log`） |

## 2. versionCode 决策与理由

- 步骤 1/2/3 各自有过一次本地构建（v8 / v9 / v10），这三条记录保留，便于回溯「哪一步引入了哪些代码」。
- 步骤 4 只做判定、不改代码 → **不递增**。
- 步骤 5/6/7 在同一轮静态改造里一起完成、只有一次构建 → 合并为 **v11**，不按步各升一次。理由：三条改动都未经真机验证，若每步一个 versionCode 会在设备上留下三个从未验证过的版本号，反而让「哪个版本是真正测过的」变模糊；合并后只需在首次上机时一次性验证 v11，发现问题再按步拆版本。
- 应用内「关于」页显示的是运行期读取的真实 versionCode（早前已改为动态读取），因此文档与界面不会因合并递增而漂移。

## 3. 步骤状态总表

| 步骤 | 内容 | 代码状态 | 静态验证 | 上机验证 |
| --- | --- | --- | --- | --- |
| 1 | 命中探针（4 个 ISO + 2 个运营商名） | 已完成（改为签名注册，见步骤 5） | 编译通过 + dex 命中 + 注册表 18 条走查 | 未验证 |
| 2 | SubscriptionInfo / SubscriptionManager `getCountryIso`（fake_sim 语义） | 已完成 | 编译通过 + dex 命中 + 走查 | 未验证 |
| 3 | getNetworkOperator / getSimOperator 的 MCC 与伪装国家一致 | 已完成 | 编译通过 + dex 命中 + 走查 | 未验证 |
| 4 | `TelephonyManager#hasSystemFeature` 补 hook（埋点未覆盖分支） | **未实现**（条件不成立，见 3.1） | — | 需按清单复测条件 |
| 5 | 反射枚举候选签名注册 hook，缺失项明确落日志 | 已完成 | 编译通过 + dex 命中 + 18 条签名表走查 + javap 对照 | 未验证 |
| 6 | 免重启生效（配置文件 mtime 变化即重读） | 已完成 | 编译通过 + 代码走查（逻辑无分支歧义） | 未验证 |
| 7 | hit 日志开关，默认关闭 | 已完成（含 App 侧开关与 X 侧文件下发） | 编译通过 + dex 命中 + 走查 | 未验证 |

### 3.1 步骤 4 为何不实现

步骤 4 的触发条件是「第 1 步命中表证明埋点分支被走」。本轮禁用设备读取，因此只能引用**停止实机测试之前**已经采到的命中表（同一台设备、同一版本 X、同一驱动流程）：

- `getNetworkCountryIso()` 6 次、`getSimOperator` 6 次、`getNetworkOperator` 5 次、`getNetworkOperatorName` 4 次、`getSimOperatorName` 2 次、`getSimCountryIso` 2 次（`orig=jp`，fake_sim=false 时未改写，符合预期）。
- `SubscriptionInfo#getCountryIso`、`SubscriptionManager#getCountryIso`、`TelephonyManager#hasSystemFeature`：**0 次**。

该证据只能说明「在所驱动的流程里这些分支没被走到」，不能证明「永远不走」（曾尝试的服务条款/付费墙界面需要登录，未进入）。故按条件判定为**暂不实现**，并把「如何确认条件是否成立」写进待验清单（V6），而不是凭推断加代码。

## 4. 静态验证 A：编译

`gradle :app:assembleDebug` → `BUILD SUCCESSFUL in 24s`（v11，产出上表 APK）。首轮编译曾失败于两处：

1. 探针区块替换时误删了步骤 2/3 的方法定义（`hookSubscriptionIso` / `hookOperatorsMcc` / `hookOperator` 在第 89/90 行被调用但定义丢失）——已用 `_patch/step2_subscription.java`、`_patch/step3_operators.java` 重新插入；
2. `Config` 类新增 4 参构造时替换未应用，导致 4 处 `new Config(..., hitLog)` 报「无法将构造器应用到给定类型」——已补替换。

两处修复后编译通过；失败日志保留在 `build_s567.log`、`build_s567b.log`，通过日志在 `build_s7.log`。

## 5. 静态验证 B：dex 卸载式核对

对 APK 内全部 12 个 dex 做 ASCII 字符串检索（解包 → 逐 dex 计数）：

| 关键字 | classes6.dex | 其它 11 个 dex |
| --- | --- | --- |
| `installProbes` | 1 | 0 |
| `installProbeBySignature` | 1 | 0 |
| `paramTypes` | 1 | 0 |
| `sig MISSING` | 1 | 0 |
| `sig ok ` | 1 | 0 |
| `sig FAIL` | 1 | 0 |
| `caller=` | 1 | 0 |
| `hookSubscriptionIso` | 1 | 0 |
| `hookOperatorsMcc` | 1 | 0 |
| `hookOperator` | 2 | 0 |
| `configFileMtime` | 1 | 0 |
| `sConfigMtime` | 1 | 0 |
| `hit_log` | 3 | 0 |
| `KEY_HIT` | 1 | 0 |
| `getHitLog` / `setHitLog` | 1 / 1 | 0 |
| `mcc of iso` | 1 | 0 |
| `mcc-iso` | 2 | 0 |
| `probe ` | 2 | 0 |

结论（仅此结论）：七步涉及的字符串/方法名确实进入了本次 APK 的 dex（集中在 classes6.dex，即模块代码所在 dex），说明代码不是只改了源文件没进包。**这不证明任何 Hook 在运行时被成功注册或命中。**

## 6. 静态验证 C：目标签名在 API 36 平台包中的存在性

用 JDK 21 `javap` 对照 `C:\Android\Sdk\platforms\android-36\android.jar`（公开 SDK 存根）：

| 类 | 签名 | 结果 |
| --- | --- | --- |
| TelephonyManager | `getNetworkCountryIso()` | PRESENT |
| TelephonyManager | `getNetworkCountryIso(int)` | PRESENT |
| TelephonyManager | `getNetworkCountryIsoForPhone(int)` | ABSENT（@hide） |
| TelephonyManager | `getNetworkCountryIsoForSubscription(int)` | ABSENT（@hide） |
| TelephonyManager | `getNetworkCountryIsoForDefaultDataSubscription(int)` | ABSENT（@hide） |
| TelephonyManager | `getSimCountryIso()` | PRESENT |
| TelephonyManager | `getSimCountryIsoForPhone(int)` | ABSENT（@hide） |
| TelephonyManager | `getSimCountryIsoForSubscription(int)` | ABSENT（@hide） |
| TelephonyManager | `getNetworkOperator()` | PRESENT |
| TelephonyManager | `getNetworkOperator(int)` | ABSENT（@hide） |
| TelephonyManager | `getSimOperator()` | PRESENT |
| TelephonyManager | `getSimOperator(int)` | ABSENT（@hide） |
| TelephonyManager | `getSimOperatorName()` | PRESENT |
| TelephonyManager | `getNetworkOperatorName()` | PRESENT |
| TelephonyManager | `hasSystemFeature(String)` | ABSENT（@hide） |
| SubscriptionInfo | `getCountryIso()` | PRESENT |
| SubscriptionManager | `getCountryIso()` | ABSENT（@hide） |
| ServiceState | `getCountryIso()` | ABSENT（@hide） |

要点：

- `ABSENT` 只表示「不在公开 SDK 存根里」，不等于「设备上没有」。@hide 方法存在于设备 framework，LSPosed 进程内反射可及（停止实机测试前，`hookAllMethods` 对 `getNetworkCountryIsoForPhone` 等确实注册成功），但**具体某台设备是否存在、是否被隐藏 API 策略拦截，必须上机用 `sig ok / sig MISSING / sig FAIL` 日志确认**。
- `getNetworkCountryIso(int)` 出现在公开存根中，属可直接核对的公开重载。

## 7. 静态验证 D：注册覆盖走查（逐条）

### 7.1 改写型 hook（行为相关，不依赖探针）

| 目标 | 注册位置 | 覆盖方式 | 生效条件 |
| --- | --- | --- | --- |
| `TelephonyManager#getNetworkCountryIso()` | `handleLoadPackage`，`hookStringGetter(TelephonyManager.class, ...)` | `hookAllMethods` 按方法名（无参重载） | 无条件改写为配置 iso |
| `TelephonyManager#getNetworkCountryIso(int)`（隐藏重载） | 同上 | `hookAllMethods` 按方法名，不限参数表 → 隐藏重载一并覆盖 | 无条件 |
| `getNetworkCountryIsoForPhone / ForSubscription`（如存在） | 同上（同一调用里的方法名列表） | 同上 | 无条件 |
| `TelephonyManager#getSimCountryIso / ForPhone / ForSubscription` | `hookStringGetter(...)` 第二组 | 同上 | `fake_sim`（`SIM_METHODS` 三分支） |
| `ServiceState#getCountryIso` | 第 87 行 | 同上 | `fake_service_state` |
| `SubscriptionInfo#getCountryIso` | 第 89 行 `hookSubscriptionIso()` | `Class.forName` + `hookAllMethods("getCountryIso")`，仅拦返回 String 的成员 | `fake_sim` |
| `SubscriptionManager#getCountryIso` | 同上 | 同上 | `fake_sim` |
| `TelephonyManager#getNetworkOperator` | 第 90 行 `hookOperatorsMcc()` | `hookAllMethods`，仅拦返回 String | 无条件（与 iso 同步） |
| `TelephonyManager#getSimOperator` | 同上 | 同上 | `fake_sim` |
| `getNetworkOperatorName` / `getSimOperatorName` | 探针（步骤 1 列表）——**目前只记录、不改写** | `findMethodExactIfExists` + `hookMethod` | `hit_log` 开关 |
| `TelephonyManager#hasSystemFeature(String)` | 探针（同上），步骤 4 待定 | 同上 | `hit_log` 开关 |

结论：用户点名的 8 个目标里，`getNetworkCountryIso(int)` 由「按名 hookAllMethods」覆盖（不依赖参数表），`SubscriptionInfo/SubscriptionManager#getCountryIso` 由步骤 2 独立 hook 覆盖，`getNetworkOperator / getSimOperator` 由步骤 3 覆盖，`getSimOperatorName / getNetworkOperatorName / hasSystemFeature` 目前只被探针登记（只读不改）。

### 7.2 探针注册表（步骤 1 + 步骤 5）

`installProbes()` 遍历 18 行 `SIGS`（类名 / 方法名 / 参数表），逐行调用 `installProbeBySignature`：

1. `Class.forName` → `XposedHelpers.findMethodExactIfExists(className, cl, name, types...)`；
2. 命中 → `XposedBridge.hookMethod` + 打印 `sig ok <sig> decl=<真实声明类>`；
3. 未命中 → 打印 `sig MISSING <sig> (not present on this build)`；
4. 异常 → 打印 `sig FAIL <sig> : <throwable>`。

即「本机不存在的方法」一定会在日志里以 `sig MISSING` 明确出现，不会被静默跳过。

## 8. 静态验证 E：现有改写行为未被改动（逐段证据）

### 8.1 改写判定块（v11 现状，第 161–171 行）

```java
161: boolean shouldFake;
162: if (ServiceState.class.getName().equals(m.getDeclaringClass().getName())) {
163:     shouldFake = c.fakeServiceState;
164: } else if (SIM_METHODS.contains(m.getName())) {
165:     shouldFake = c.fakeSim;
166: } else {
167:     shouldFake = true;
168: }
169: if (shouldFake) {
170:     param.setResult(c.iso);
171: }
```

与改动前（步骤 1 之前那次源码导出，当时位于第 155–165 行）逐行一致：判定顺序（ServiceState → SIM_METHODS → 默认 true）、三个分支的取值、`param.setResult(c.iso)` 全部未变。`SIM_METHODS` 仍是三条：`getSimCountryIso`、`getSimCountryIsoForPhone`、`getSimCountryIsoForSubscription`（未新增成员）。

### 8.2 本轮全部改动的性质

本轮改动来自 `_patch/edits_steps567.txt`（17 个可机读的「旧文本 → 新文本」条目）+ 三个插入块，逐条性质：

| 改动 | 性质 |
| --- | --- |
| 新增 `sHitLog` / `sConfigMtime` 字段 | 纯新增 |
| `Config` 增加 `hitLog` 字段与 4 参构造，3 参构造委托为 `hitLog=false` | 纯新增（3 参行为不变） |
| `currentConfig` 增加 mtime 比较（步骤 6） | 语义新增：仅当配置文件 mtime 变化时跳过 2s TTL 立即重读；未变化时执行路径与原来一致 |
| `readFromXSideFile` 增加 `hit_log` 解析 | 纯新增（缺省 false） |
| 三处 `if (DEBUG_LOG)` → `if (sHitLog)`（hits 日志） | 仅日志开关，不参与改写判定 |
| `probeLog` 增加 `if (!sHitLog) return;` | 仅日志开关 |
| 步骤 2 / 3 的方法体 | 纯新增（新 hook，注册在改写 hook 之后） |
| 探针区块替换为签名注册表 | 仅探针（原探针同样只记录、不改写） |
| `RootBridge.pushToX` 增加 4 参版本并在 body 追加 `hit_log=` | 3 参版本保留（委托 4 参、hit_log=false），仅 X 侧配置文件内容多一行 |
| `ModuleProvider` 增加 `hit_log` 字段 | 纯新增 |
| 设置页新增「hit 日志（排查用）」开关 | 纯新增 UI |

结论：**改写值本身（`c.iso`）与判定条件没有改动**；新增的步骤 2/3 hook 只在各自明确条件下改写，且与既有 hook 的优先级关系是「后注册、覆盖前者结果」，对 fake_sim 关闭时的返回值为 no-change。

### 8.3 已知缺口（静态即可确认）

- `readFromProvider()` 与 `readFromXSharedPreferences()` 仍用 3 参构造 → 走这两条回退路径时 `hitLog` 恒为 false（即探针/hit 静默）。主路径（X 侧配置文件）已支持。
- 设置页开关只写 App 侧偏好并打印一行 `[App] hit_log=...`；要真正传到 X，必须在模块页点「保存到 X」把 `hit_log=` 写进 `xiso_config.txt`（hook 以该文件为主源）。这是本轮为避免改动下发链路而接受的取舍。

## 9. 待上机验证清单（用户回来后照做）

前置：`adb` 可用、设备已连、su 可用、LSPosed 中模块对 X 生效。

### V1 安装与模块加载（判据：注册表全量可见）

```
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell dumpsys package com.example.xiospoof | findstr versionCode      # 期望 versionCode=11
adb -s <serial> shell su -c "rm -f /data/data/com.twitter.android/files/xiso_log.txt"
adb -s <serial> shell am force-stop com.twitter.android
adb -s <serial> shell monkey -p com.twitter.android -c android.intent.category.LAUNCHER 1
adb -s <serial> shell su -c "cat /data/data/com.twitter.android/files/xiso_log.txt"
```

判据：出现 `sig ok ...` 若干条；缺失项以 `sig MISSING` 明确列出（记录哪些签名在本机不存在）；不应出现 `hook FAIL`。注意：探针与 hit 共用 `hit_log` 开关且默认关闭，先按 V8 打开再驱赶，否则只有 `sig ok/MISSING` 而没有 `hit/probe` 行。

### V2 三个无条件伪装（步骤 1 前提行为）

驱赶信息流/设置页几屏后看日志判据：`getNetworkCountryIso()` 与 `getNetworkCountryIso(int)` 的 hit 行均为 `-> tw`；`ForPhone/ForSubscription` 若 `sig ok` 存在，同样 `-> tw`。

### V3 fake_sim 语义（步骤 2）

判据：`fake_sim=false` 时 `getSimCountryIso` / `SubscriptionInfo#getCountryIso` 行为 `no-change`、`SubscriptionManager#getCountryIso` 行为 `no-change`；把「SIM 信息」开关打开并「保存到 X」后，同样调用应为 `-> tw`。

### V4 ServiceState（既有行为）

判据：`fake_service_state=true` → `ServiceState#getCountryIso` 为 `-> tw`；关闭后应为真实值（`no-change`）。

### V5 MCC 一致性（步骤 3）

判据：`getNetworkOperator` hit 行 `orig=46001 -> 46601`（iso=tw 时 MCC=466）；`getSimOperator` 在 `fake_sim=false` 时**不产生** `-> 46601`（应保持 `orig=46011`，或被显式跳过）；打开 fake_sim 后应变 `46601`。若出现 `orig=46001 -> 46601` 但 App 内实际显示异常，记录调用参数（`args=[0/1/...]`）。

### V6 步骤 4 的条件复测

判据：完整走一遍「设置 → 你的账号 → 手机号/验证 → 服务条款/付费墙」流程，统计 `hasSystemFeature` 的 `probe` 行数量与 `args=[...]` 内容。>0 且含 `android.hardware.telephony` 之类关键项 → 才实现改写（并注意关闭时返回 false 的隐蔽性）。若该签名 `sig MISSING`，则本机无此隐藏方法，步骤 4 失去意义。

### V7 免重启生效（步骤 6）

```
adb -s <serial> shell su -c "printf 'iso=jp\nfake_sim=false\nfake_service_state=true\nhit_log=true\n' > /data/data/com.twitter.android/files/xiso_config.txt"
# 不 force-stop，直接切回 X 再滑动几屏
```

判据：不重启 X 的情况下，后续 hit 行的 `->` 值变为 `jp`（或日志出现配置重载提示）。完成后务必改回 `iso=tw`。

### V8 hit 日志开关（步骤 7）

判据：开关关闭（默认）时日志中 `hit `/`probe ` 行为 0（只有模块加载类日志）；在设置页打开「hit 日志（排查用）」→ 模块页「保存到 X」→ 重开 X → 出现 `hit `/`probe ` 行。检查 `xiso_config.txt` 是否含 `hit_log=true`。

### V9 回退路径缺口（已知限制复核）

判据：删除 `xiso_config.txt` 后重启 X，日志应显示走了 provider/SharedPreferences 路径且 `hit_log` 不生效（预期现象，不是 bug）。

## 10. 明确未验证 / 静态无法得出结论的项

1. 任何 hook 是否在本机成功注册并改写返回值——**未验证**（需 V1–V5）。
2. 18 条签名里 10 条为 @hide 签名，本机是否存在、是否被隐藏 API 拦截——**未验证**（需 V1 的 `sig ok/MISSING/FAIL`）。
3. 步骤 6 的 mtime 重读是否真的绕过了进程内缓存（只在代码层成立，运行期行为未验证）。
4. 步骤 7 的开关经「保存到 X」下发后能否被 hook 读到（文件写入 + 解析链路未运行验证）。
5. 步骤 4 的判定依赖旧命中表 + 未覆盖的服务条款流程，**不构成“hasSystemFeature 永不命中”的结论**。
