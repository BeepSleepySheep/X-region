# 第三方来源与修改说明

本工程（XRegion）整体以 **GNU GPL-3.0** 分发，全文见 `LICENSE`。开源许可只保留以下三项。

## 1. 第三方来源与修改说明

### SukiSU-Ultra v4.1.3 — 界面与组件原始来源
- 仓库：https://github.com/SukiSU-Ultra/SukiSU-Ultra （tag `v4.1.3`）
- 许可证：GNU GPL-3.0（已核对 `v4.1.3` 的 LICENSE 原文首行：`GNU GENERAL PUBLIC LICENSE / Version 3, 29 June 2007`）
- 本项目用到的部分（文件头均标注来源）：`ui/component/FloatingBottomBar.kt`、`ui/component/liquid/{CombinedBackdrop,InnerShadow,Lens,Vibrancy}.kt`、`ui/component/miuix/animation/{DampedDragAnimation,InteractiveHighlight}.kt`、`ui/component/miuix/modifier/DragGestureInspector.kt`、`ui/util/BlurExt.kt`，以及主题与导航骨架（`ThemeController` + `ColorSchemeMode`、`Scaffold` + `BlurredBar`）。
- 取用路径：上述文件的 package/import 适配版取自 **XBlocker 0.3.5**（自身代码 MIT、整体 GPL-3.0），本工程沿用其 GPL-3.0 分发方式。
- 修改说明：仅调整包名与 import、替换为本应用的品牌与本地入口；未修改组件逻辑。逐行比对（仅文件头/包名不同）：CombinedBackdrop 41 行中 40 行相同、InnerShadow 162/161、Lens 218/217、Vibrancy 17/16、FloatingBottomBar 460/451、DampedDragAnimation 148/146、InteractiveHighlight 115/113、DragGestureInspector 86/85。修改后的文件继续以 GPL-3.0 提供，源码随工程目录一并分发。

### Miuix / AndroidX 等 Apache-2.0 组件
- **Miuix（compose-miuix-ui）0.9.3**：https://github.com/compose-miuix-ui/miuix ，Apache-2.0（取自 `top.yukonga.miuix.kmp` 依赖 POM 的 `<licenses>`）。用作 `miuix-ui` / `miuix-preference` / `miuix-blur` / `miuix-squircle` 依赖；FloatingBottomBar 与 liquid 绘制参考其示例 `IosLiquidGlassNavigationBar`。
- **Kyant0/AndroidLiquidGlass**：https://github.com/Kyant0/AndroidLiquidGlass ，Apache-2.0。backdrop / 内阴影 / lens / vibrancy 的液态玻璃绘制参考。
- **Xposed API 82**：`de.robv.android.xposed:api:82`，Apache-2.0（artifact POM）。仅编译期 API，运行期由 LSPosed 提供，不打包进 APK。
- **AndroidX / Jetpack Compose**：Apache-2.0（`material-icons-extended:1.7.8`、`activity-compose:1.12.1` 等）。
- 修改说明：以上组件按原样作为依赖或参考实现使用，未改动其许可声明。

## 2. SukiSU — GNU GPL v3

SukiSU-Ultra v4.1.3 的界面组件及其 XBlocker 0.3.5 适配版以 **GNU GPL-3.0** 授权。许可证全文见工程根目录 `LICENSE`，并随 APK 打包为 `assets/LICENSE`。

## 3. Miuix / AndroidX — Apache 2.0

Miuix、AndroidX / Jetpack Compose、Kyant0/AndroidLiquidGlass 与 Xposed API 82 以 **Apache-2.0** 授权。许可证全文见 `app/src/main/assets/licenses/Apache-2.0.txt`，APK 内路径为 `licenses/Apache-2.0.txt`。