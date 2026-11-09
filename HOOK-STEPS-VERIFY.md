# Hook 七步 · 实机验证记录（V1–V9，2026-10-24）

设备：Redmi 24117RK2CC（Android 16 / SDK 36，serial 5941990a）。X = com.twitter.android 12.22.0-prod.01。
测试前用户配置已记录：`iso=tw / fake_sim=false / fake_service_state=true`（46B，mtime 16:43），测试后已逐字恢复并 chown u0_a357。

## 0. 版本对应表（哪个版本真正验过）

| versionCode | 内容 | 是否实机验过 |
| --- | --- | --- |
| 9 | 步骤 2（旧） | 旧基线 |
| 11 | 步骤 1–3 + 5–7（静态完成） | 是：V1–V5、V7、V8、V9（V1 的基础） |
| 12 | 仅新增诊断 census + hookAllMethods 命中计数 | 是：V1 的 census 与 hooked= 计数 |
| 13 | readFromProvider 解析 hit_log + 三个 hook 的 hooked 计数 + 应用页「Hook 覆盖」卡 | 是：注册表 hooked= 全量、命中、UI 卡 |

## 1. V1 安装与注册表（实测）

`adb install -r` 成功，`dumpsys package` → `versionCode=11`（后升级 12、13）。首启日志（19:13:52）逐条：

```
sig ok    TelephonyManager#getNetworkCountryIso()
sig ok    TelephonyManager#getNetworkCountryIso(int)
sig ok    TelephonyManager#getNetworkCountryIsoForPhone(int)
sig MISSING TelephonyManager#getNetworkCountryIsoForSubscription(int)
sig MISSING TelephonyManager#getNetworkCountryIsoForDefaultDataSubscription(int)
sig ok    TelephonyManager#getSimCountryIso()
sig ok    TelephonyManager#getSimCountryIsoForPhone(int)
sig MISSING TelephonyManager#getSimCountryIsoForSubscription(int)
sig ok    TelephonyManager#getNetworkOperator()
sig ok    TelephonyManager#getNetworkOperator(int)
sig ok    TelephonyManager#getSimOperator()
sig ok    TelephonyManager#getSimOperator(int)
sig ok    TelephonyManager#getSimOperatorName()
sig ok    TelephonyManager#getNetworkOperatorName()
sig MISSING TelephonyManager#hasSystemFeature(java.lang.String)
sig ok    SubscriptionInfo#getCountryIso()
sig MISSING SubscriptionManager#getCountryIso()
sig MISSING ServiceState#getCountryIso()
```

12 ok / 6 MISSING / 0 FAIL。v12 加入的 census 给出了“缺失”的真实原因：

```
census ServiceState declared=112 public=115 | declaredIso=-none- | publicIso=-none-
census SubscriptionManager declared=144 public=132 | declaredIso=-none- | publicIso=-none-
census TelephonyManager declared=562 public=505 | declaredIso=8 [getSimCountryIso/1p getSimCountryIsoForPhone/1p getNetworkCountryIso/0p getNetworkCountryIso/1p getNetworkCountryIsoForPhone/1p getSimCountryIso/0p setSimCountryIso/1p setSimCountryIsoForPhone/2p]
census SubscriptionInfo declared=79 public=52 | declaredIso=2 [getCountryIso/0p ...] | publicIso=1 [getCountryIso/0p]
```

结论（观测）：本机 `ServiceState` 完全没有 `getCountryIso`（也没有任何含 countryIso 的成员），`SubscriptionManager` 同，`TelephonyManager` 根本没有 `hasSystemFeature`。这三个不是“隐藏 API 被过滤”，而是本 ROM 上就不存在。

v12/v13 把 `hookAllMethods` 的命中数写进了日志（v11 之前无论挂上几个都打印“成功”）：

```
hook 成功 TelephonyManager#getNetworkCountryIso hooked=2
hook 成功 TelephonyManager#getNetworkCountryIsoForPhone hooked=1
hook 成功 TelephonyManager#getNetworkCountryIsoForSubscription hooked=0
hook 成功 TelephonyManager#getSimCountryIso hooked=2
hook 成功 TelephonyManager#getSimCountryIsoForPhone hooked=1
hook 成功 TelephonyManager#getSimCountryIsoForSubscription hooked=0
hook 成功 ServiceState#getCountryIso hooked=0          <- 旧版本会显示“成功”，实际 0 个
hook ok SubscriptionInfo#getCountryIso (fake_sim) hooked=1
hook ok SubscriptionManager#getCountryIso (fake_sim) hooked=0  <- 同上
hook ok TelephonyManager#getNetworkOperator (mcc-iso) hooked=2
hook ok TelephonyManager#getSimOperator (fake_sim, mcc-iso) hooked=2
```

## 2. V2 三处无条件伪装（实测）

```
hit TelephonyManager#getNetworkCountryIso args=[1] orig=cn -> tw fake_sim=true fake_ss=true
hit TelephonyManager#getNetworkCountryIso args=[] orig=tw -> tw
```

`args=[1]` 即隐藏重载 `getNetworkCountryIso(int)`，`orig=cn`（真实 MCC 460 中国）被改写为 tw；无参重载同样被改写。`ForPhone` 已 hooked=1 但本轮驱动的流程中未被调用（无 hit 行）——**未观测到调用**，不能当作已验证。

## 3. V3 fake_sim 语义（实测，两个方向）

fake_sim=false（iso=jp，19:18:48）：
```
hit TelephonyManager#getSimCountryIso args=[] orig=jp -> no-change fake_sim=false
probe TelephonyManager#getSimOperator args=[] ret=46011     （未被改写）
```

fake_sim=true（iso=jp，19:19:10）：
```
hit TelephonyManager#getSimCountryIso args=[] orig=jp -> jp fake_sim=true
hit TelephonyManager#getSimOperator args=[] orig=46011 -> 44001
```

结论：`getSimCountryIso` 的开关语义成立。`SubscriptionInfo#getCountryIso` 已 hooked=1 但本轮未观测到调用（无 hit 行）——**未验证**；`SubscriptionManager#getCountryIso` 在本机不存在（hooked=0）。

## 4. V4 ServiceState（实测：无效，真实缺陷）

全轮日志中 **没有任何** `ServiceState#getCountryIso` 的 hit 行；结合 census，这不是“没被调用”，而是本机 `ServiceState` 没有该方法，`hookAllMethods` 挂了 0 个。→ **应用里的“ServiceState 伪装”开关在当前设备上是空开关**（旧版本日志却会显示“hook 成功”）。需要新目标（候选：`ServiceState#getNetworkCountryIso` 不存在，只能从 `TelephonyManager` 层做，或解析 `ServiceState` 内部字段），待设计。

## 5. V5 MCC 与 ISO 一致性（实测，两个国家）

iso=tw：`getNetworkOperator orig=46001 -> 46601`、`getSimOperator orig=46011 -> 46601`。
iso=jp：`getNetworkOperator orig=46001 -> 44001`、`getSimOperator orig=46011 -> 44001`。
即 466/440 + "01" 与伪装国家一致，未伪装时保留真实 46001/46011。

## 6. V6 步骤 4 条件（实测：条件不成立，不实现）

`sig MISSING TelephonyManager#hasSystemFeature(String)`；TelephonyManager 的 562 个声明方法里没有任何 `hasSystemFeature`。前一轮“命中 0 次”的真实原因不是分支未走到，而是方法不存在。→ **步骤 4 不实现**；若要覆盖该面，只能换目标类（如 `PackageManager#hasSystemFeature`，属新议题）。

## 7. V7 免重启生效（实测通过）

19:17:16 写入 `iso=jp`（mtime 变更），X 进程 pid 始终保持 21741（前后一致，未重启），19:17:55 日志：
```
配置已更新：iso=jp fake_sim=false fake_service_state=true
hit TelephonyManager#getNetworkOperator args=[] orig=46001 -> 44001 (mcc of iso=jp, fake_sim=false)
```
mtime 重读生效。另 19:16:52 → 19:16:59 出现过一次同类重载（当时写入了非法 iso，被 normalize 回退到 us，日志如实显示 iso=us）。

## 8. V8 hit 日志开关（实测通过）

App 侧：设置页“hit 日志（排查用）”关闭 → 无 hit/probe 行；打开后写 `hit_log=true`，模块页“保存并应用”把 `hit_log=true` 写进 `xiso_config.txt`（58B，四行）；重启 X 后出现 `hit`/`probe` 行。关闭态证据：19:23:55 那次启动（配置文件被删、hit_log 无法下发）hit=0、probe=0。

## 9. V9 回退路径（实测：比预期更差，仅部分修复）

删掉 `xiso_config.txt` 后重启 X（v12，19:19:49）：hit=0、probe=0；v13（已解析 provider 的 hit_log，19:23:55）**仍然 hit=0、probe=0**，且日志尾行是：
```
配置来源：…（非配置文件路径）
已注入 com.twitter.android，当前 iso=us
```
即当 X 侧文件缺失时，provider 调用（`cr.call(CONFIG_URI,"get_config")`）与 XSharedPreferences 两条回退都拿不到值，最终落到默认 `iso=us`（不是用户配的 tw），hit_log 也因此为 false。
**结论：V9 不是“只缺 hit_log”这一个洞，而是整个回退链在本机不可用；v13 的 provider hit_log 解析逻辑存在但无法被验证到（provider 未被调用成功）。修复方向：①排查 provider 对被调方包名的授权；②或在 pushToX 时同时写一份备份文件供回退读取。** 均已超出本轮范围，未实施。

## 10. 本轮新交付的 UI（XBlocker/KernelSU 式能力状态）

v13 在「应用」页新增「Hook 覆盖（本机实测）」卡片，直接读取模块日志的 `hooked=` 计数，逐项显示：
```
getNetworkCountryIso ✓2   getNetworkCountryIsoForPhone ✓1   getNetworkCountryIsoForSubscription ✗0
ServiceState#getCountryIso ✗0   getCountryIso ✓1   SubscriptionManager#getCountryIso ✗0 …
```
实测节点文本（uiautomator dump，19:24）：`Hook 覆盖（本机实测）` [187,715][679,778] + 汇总行 [187,778][1001,1198]。
意义：开关是否真有效不再靠“hook 成功”这种不可靠文案，而由真实挂载数决定（本机就能看到 ServiceState 是 ✗0）。

## 11. 测试残留与设备状态

- `/sdcard/ui*.xml`、`/sdcard/shot_apps.png` 已删除（最后校验输出 `none_found_if_blank`）。
- 用户配置已恢复为 `iso=tw / fake_sim=false / fake_service_state=true`，owner `u0_a357:u0_a357` 600，X 重启后日志确认 `配置已更新：iso=tw`。
- 未新增权限、未新增 authority；本轮只用了既有 su 命令与既有 provider。
