package com.example.xregion

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.xregion.ui.LocalDarkTheme
import com.example.xregion.ui.PlainFloatingBar
import com.example.xregion.ui.SuperSwitch
import com.example.xregion.ui.UiPrefs
import com.example.xregion.ui.component.FloatingBottomBar
import com.example.xregion.ui.component.FloatingBottomBarItem
import com.example.xregion.ui.component.miuix.effect.BgEffectBackground
import com.example.xregion.ui.component.miuix.effect.ColorBlendToken
import com.example.xregion.ui.util.BlurredBar
import com.example.xregion.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * UI 栈：Miuix 0.9.3（miuix-ui / miuix-preference / miuix-blur / miuix-squircle，Apache-2.0）。
 * 主题 / 导航骨架与液体玻璃、悬浮底栏的原始实现来自 SukiSU-Ultra v4.1.3（GPL-3.0），
 * 经 XBlocker 0.3.5 的 package/import 适配后取用（逐行比对：仅文件头与包名不同）。
 * 其中 FloatingBottomBar 与 liquid 绘制同时参考了 Miuix 示例 IosLiquidGlassNavigationBar
 * 与 Kyant0/AndroidLiquidGlass（Apache-2.0）。完整署名与许可证见 THIRD_PARTY_NOTICES.md。
 */
class MainActivity : ComponentActivity() {

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        migrateLauncherState()
        enableEdgeToEdge()
        setContent { XisoApp() }
    }

    private fun bg(block: () -> Unit) {
        Thread(block).start()
    }

    private fun post(block: () -> Unit) {
        uiHandler.post(block)
    }

    private fun toast(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
    }

    // ------------------------------------------------------------------ 主题

    @Composable
    private fun XisoApp() {
        var colorMode by remember { mutableIntStateOf(UiPrefs.colorMode(this@MainActivity)) }
        var monet by remember { mutableStateOf(ThemeManager.isMonet(this@MainActivity)) }
        val systemDark = isSystemInDarkTheme()
        val darkTheme = when (colorMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }
        val monetActive = monet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val mode = if (monetActive) {
            when (colorMode) {
                1 -> ColorSchemeMode.MonetLight
                2 -> ColorSchemeMode.MonetDark
                else -> ColorSchemeMode.MonetSystem
            }
        } else {
            when (colorMode) {
                1 -> ColorSchemeMode.Light
                2 -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            }
        }
        val controller = ThemeController(
            colorSchemeMode = mode,
            darkColors = darkColorScheme(background = Color.Black),
            lightColors = lightColorScheme(),
            isDark = darkTheme,
        )
        LaunchedEffect(darkTheme) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
            MiuixTheme(controller = controller) {
                XisoScreen(
                    colorMode = colorMode,
                    monet = monet,
                    monetActive = monetActive,
                    onColorMode = {
                        colorMode = it
                        UiPrefs.setColorMode(this@MainActivity, it)
                    },
                    onMonet = {
                        monet = it
                        ThemeManager.setMonet(this@MainActivity, it)
                    },
                )
            }
        }
    }

    // ------------------------------------------------------------------ 骨架

    @Composable
    private fun XisoScreen(
        colorMode: Int,
        monet: Boolean,
        monetActive: Boolean,
        onColorMode: (Int) -> Unit,
        onMonet: (Boolean) -> Unit,
    ) {
        val pages = listOf("主页", "应用", "日志", "设置")
        val icons = listOf(
            Icons.Rounded.Cottage,
            Icons.Rounded.Extension,
            Icons.Rounded.History,
            Icons.Rounded.Settings,
        )
        var page by rememberSaveable { mutableIntStateOf(0) }
        var aboutOpen by rememberSaveable { mutableStateOf(false) }
        var aboutSection by rememberSaveable { mutableIntStateOf(0) }
        var appearanceOpen by rememberSaveable { mutableStateOf(false) }
        var predictiveBack by remember { mutableStateOf(ThemeManager.isPredictiveBack(this@MainActivity)) }
        var backProgress by remember { mutableFloatStateOf(0f) }
        val usePredictiveBack = predictiveBack && Build.VERSION.SDK_INT >= 34
        val handleBack = {
            when {
                aboutOpen && aboutSection != 0 -> aboutSection = 0
                aboutOpen -> aboutOpen = false
                appearanceOpen -> appearanceOpen = false
            }
        }
        if (usePredictiveBack) {
            PredictiveBackHandler(enabled = aboutOpen || appearanceOpen) { progress ->
                try {
                    progress.collect { backProgress = it.progress }
                    handleBack()
                    backProgress = 0f
                } catch (e: CancellationException) {
                    backProgress = 0f
                    throw e
                }
            }
        } else {
            BackHandler(enabled = aboutOpen || appearanceOpen) { handleBack() }
        }
        var blur by remember { mutableStateOf(ThemeManager.isBlur(this@MainActivity)) }
        var floatingBar by remember { mutableStateOf(ThemeManager.isFloatingBar(this@MainActivity)) }
        var liquidGlass by remember { mutableStateOf(ThemeManager.isFloatingBarBlur(this@MainActivity)) }

        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberBlurBackdrop(blur)
        val glassBackdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        val aboutBackdrop = rememberLayerBackdrop()
        val glassEnabled = blur && Build.VERSION.SDK_INT >= 33
        val scrollBehavior = MiuixScrollBehavior()
        val listState = rememberLazyListState()

        var snapshot by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }

        fun refreshSnapshot() {
            if (busy) return
            busy = true
            bg {
                val snap = try {
                    RootBridge.snapshot()
                } catch (t: Throwable) {
                    "FAIL:" + t
                }
                post {
                    snapshot = snap
                    busy = false
                }
            }
        }

        LaunchedEffect(Unit) { refreshSnapshot() }

        val aboutEffect = remember(blur) {
            blur &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                isRuntimeShaderSupported()
        }

        Box(Modifier.fillMaxSize()) {
            if (aboutOpen) {
                if (aboutEffect) {
                    BgEffectBackground(
                        dynamicBackground = true,
                        modifier = Modifier.fillMaxSize(),
                        bgModifier = Modifier.layerBackdrop(aboutBackdrop),
                        isFullSize = true,
                        effectBackground = true,
                        alpha = { 1f },
                    ) {}
                } else {
                    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface))
                }
            }

            Scaffold(
                containerColor = if (aboutOpen) Color.Transparent else surfaceColor,
                topBar = {
                    if (aboutOpen) {
                        TopAppBar(
                            title = when {
                                aboutSection == 1 -> "开源许可"
                                aboutSection == 2 -> "隐私说明"
                                else -> "关于"
                            },
                            color = Color.Transparent,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (aboutSection != 0) aboutSection = 0 else aboutOpen = false
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "返回",
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                }
                            },
                        )
                    } else {
                        BlurredBar(backdrop) {
                            TopAppBar(
                                title = if (appearanceOpen) "界面风格"
                                else if (page == 0) "XRegion" else pages[page],
                                color = if (backdrop != null) Color.Transparent else surfaceColor,
                                scrollBehavior = scrollBehavior,
                                navigationIcon = {
                                    if (appearanceOpen) {
                                        IconButton(onClick = { appearanceOpen = false }) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = "返回",
                                                tint = MiuixTheme.colorScheme.onBackground,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!aboutOpen && !appearanceOpen) {
                        if (floatingBar) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 26.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (Build.VERSION.SDK_INT < 33) {
                                    PlainFloatingBar(page, pages, icons) { page = it; aboutOpen = false }
                                } else {
                                    FloatingBottomBar(
                                        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                                        selectedIndex = { page },
                                        onSelected = { page = it; aboutOpen = false },
                                        backdrop = glassBackdrop,
                                        tabsCount = pages.size,
                                        isBlurEnabled = glassEnabled,
                                        isGlassEnabled = liquidGlass,
                                    ) {
                                        icons.forEachIndexed { index, icon ->
                                            FloatingBottomBarItem(
                                                onClick = { page = index; aboutOpen = false },
                                                modifier = Modifier.semantics { selected = page == index },
                                            ) {
                                                val tint = if (!glassEnabled && page == index) {
                                                    MiuixTheme.colorScheme.primary
                                                } else {
                                                    MiuixTheme.colorScheme.onSurface
                                                }
                                                Icon(icon, null, tint = tint)
                                                Text(
                                                    pages[index],
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp,
                                                    color = tint,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            BlurredBar(backdrop) {
                                NavigationBar(color = if (backdrop != null) Color.Transparent else surfaceColor) {
                                    icons.forEachIndexed { index, icon ->
                                        NavigationBarItem(
                                            modifier = Modifier.weight(1f),
                                            selected = page == index,
                                            onClick = { page = index; aboutOpen = false },
                                            icon = icon,
                                            label = pages[index],
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        .then(
                            if (floatingBar && glassEnabled) Modifier.layerBackdrop(glassBackdrop)
                            else Modifier,
                        ),
                ) {
                    if (aboutOpen) {
                        AboutPage(
                            section = aboutSection,
                            onSection = { aboutSection = it },
                            topPadding = padding.calculateTopPadding(),
                            bottomPadding = padding.calculateBottomPadding(),
                            backdrop = if (aboutEffect) aboutBackdrop else null,
                            backProgress = backProgress,
                        )
                    } else if (appearanceOpen) {
                        AppearancePage(
                            topPadding = padding.calculateTopPadding(),
                            bottomPadding = padding.calculateBottomPadding(),
                            monet = monet,
                            monetActive = monetActive,
                            onMonet = onMonet,
                            blur = blur,
                            floatingBar = floatingBar,
                            liquidGlass = liquidGlass,
                            glassEnabled = glassEnabled,
                            onBlur = {
                                blur = it
                                ThemeManager.setBlur(this@MainActivity, it)
                            },
                            onFloatingBar = {
                                floatingBar = it
                                ThemeManager.setFloatingBar(this@MainActivity, it)
                            },
                            onLiquidGlass = {
                                liquidGlass = it
                                ThemeManager.setFloatingBarBlur(this@MainActivity, it)
                            },
                            predictiveBack = predictiveBack,
                            onPredictiveBack = {
                                predictiveBack = it
                                ThemeManager.setPredictiveBack(this@MainActivity, it)
                            },
                            backProgress = backProgress,
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MiuixTheme.colorScheme.surface)
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 12.dp,
                                bottom = padding.calculateBottomPadding() + 24.dp,
                            ),
                        ) {
                            item {
                                when (page) {
                                    0 -> HomePage(
                                        snapshot = snapshot,
                                        busy = busy,
                                        onRefresh = { refreshSnapshot() },
                                        onOpenApps = { page = 1 },
                                    )

                                    1 -> AppsPage()
                                    2 -> LogsPage()
                                    else -> SettingsPage(
                                        colorMode = colorMode,
                                        monet = monet,
                                        monetActive = monetActive,
                                        onColorMode = onColorMode,
                                        onMonet = onMonet,
                                        blur = blur,
                                        floatingBar = floatingBar,
                                        liquidGlass = liquidGlass,
                                        glassEnabled = glassEnabled,
                                        onBlur = {
                                            blur = it
                                            ThemeManager.setBlur(this@MainActivity, it)
                                        },
                                        onFloatingBar = {
                                            floatingBar = it
                                            ThemeManager.setFloatingBar(this@MainActivity, it)
                                        },
                                        onLiquidGlass = {
                                            liquidGlass = it
                                            ThemeManager.setFloatingBarBlur(this@MainActivity, it)
                                        },
                                        onOpenAppearance = { appearanceOpen = true },
                                        onOpenAbout = {
                                            aboutSection = 0
                                            aboutOpen = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ 主页
    /** 关于：独立二级页面（XBlocker / KernelSU 风格：主列表 → 开源许可 / 隐私说明）。 */
    @Composable
    private fun AboutPage(
        section: Int,
        onSection: (Int) -> Unit,
        topPadding: Dp,
        bottomPadding: Dp,
        backdrop: LayerBackdrop?,
        backProgress: Float,
    ) {
        val context = this@MainActivity
        var selected by remember { mutableStateOf<String?>(null) }

        val licenses = listOf(
            "第三方来源与修改说明" to "THIRD_PARTY_NOTICES.md",
            "SukiSU · GNU GPL v3" to "LICENSE",
            "Miuix / AndroidX · Apache 2.0" to "licenses/Apache-2.0.txt",
        )

        fun readAsset(name: String): String = try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (t: Throwable) {
            "读取失败：$t"
        }

        val document = remember(selected) {
            selected?.let { readAsset(it) }.orEmpty()
        }

        val isDark = LocalDarkTheme.current
        val blendColors = remember(isDark) {
            if (isDark) ColorBlendToken.Overlay_Thin_Light
            else ColorBlendToken.Pured_Regular_Light
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = backProgress * size.width * 0.35f
                    alpha = 1f - backProgress * 0.25f
                },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topPadding + 20.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = bottomPadding + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (section == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                            Text(
                                "XRegion",
                                fontSize = 35.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 12.dp, bottom = 5.dp),
                            )
                            Text(
                                "v${appVersion()} (${appVersionCode()})",
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    item {
                        AboutCard(backdrop, blendColors) {
                            ArrowPreference(
                                title = "GitHub 开源仓库",
                                onClick = {
                                    if (PROJECT_GITHUB_URL.isBlank()) {
                                        toast("GitHub 仓库地址暂未配置")
                                    } else {
                                        startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_GITHUB_URL)),
                                        )
                                    }
                                },
                            )
                            ArrowPreference(
                                title = "开源许可",
                                onClick = { onSection(1) },
                            )
                            ArrowPreference(
                                title = "隐私说明",
                                onClick = { onSection(2) },
                            )
                        }
                    }
                } else if (section == 1) {
                    item {
                        AboutCard(backdrop, blendColors) {
                            licenses.forEach { (title, asset) ->
                                ArrowPreference(
                                    title = title,
                                    onClick = { selected = asset },
                                )
                            }
                        }
                    }
                } else {
                    item {
                        AboutCard(backdrop, blendColors) {
                            BasicComponent(
                                title = "本地处理",
                                summary = "国家码替换与日志都只在 X 进程和本机完成，不依赖远程服务。",
                            )
                            BasicComponent(
                                title = "无网络访问",
                                summary = "应用未声明 INTERNET 权限，不上传配置、日志或设备信息。",
                            )
                            BasicComponent(
                                title = "作用范围",
                                summary = "仅作用于 com.twitter.android，不修改其他应用。",
                            )
                            BasicComponent(
                                title = "数据位置",
                                summary = "配置与日志写入 X 私有目录；写入前需在 KernelSU 中授权 root。",
                            )
                            BasicComponent(
                                title = "账号数据",
                                summary = "只替换设备国家码返回值，不读取、不上传账号内容。",
                            )
                        }
                    }
                }
            }

            OverlayDialog(
                show = selected != null,
                title = licenses.firstOrNull { it.second == selected }?.first ?: "开源许可",
                onDismissRequest = { selected = null },
            ) {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        document,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                TextButton(
                    "关闭",
                    onClick = { selected = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }

    @Composable
    private fun AboutCard(
        backdrop: LayerBackdrop?,
        blendColors: List<BlendColorEntry>,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (backdrop != null) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(16.dp),
                            blurRadius = 60f,
                            colors = BlurColors(blendColors = blendColors),
                        )
                    } else Modifier,
                ),
            colors = CardDefaults.defaultColors(
                if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                Color.Transparent,
            ),
            content = content,
        )
    }


    @Composable
    private fun HomePage(
        snapshot: String,
        busy: Boolean,
        onRefresh: () -> Unit,
        onOpenApps: () -> Unit,
    ) {
        val idSection = section(snapshot, "##ID", "##STAT")
        val statSection = section(snapshot, "##STAT", "##TAIL")
        val injected = snapshot.contains("已注入")
        val rootOk = idSection.contains("uid=0")
        val iso = ConfigStore.getIso(this@MainActivity)
        val statTime = statSection.split("|").getOrNull(0)?.trim().orEmpty()
        val statSize = statSection.split("|").getOrNull(1)?.trim().orEmpty()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeStatusCard(
                injected = injected,
                waiting = snapshot.isEmpty(),
                monet = ThemeManager.isMonet(this@MainActivity),
                iso = iso,
                statTime = statTime,
                statSize = statSize,
                onOpenApps = onOpenApps,
            )
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "模块状态",
                    summary = when {
                        snapshot.isEmpty() -> "正在读取…"
                        injected -> "X 侧日志确认已注入 com.twitter.android"
                        else -> "未检测到注入记录，请确认 LSPosed 已启用本模块"
                    },
                    startAction = { RowIcon(Icons.Rounded.Extension) },
                    endActions = {
                        Text(
                            when {
                                snapshot.isEmpty() -> "…"
                                injected -> "已注入"
                                else -> "未检测"
                            },
                            color = if (injected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                )
                BasicComponent(
                    title = "作用域",
                    summary = if (injected) "包含 com.twitter.android"
                    else "未确认（需 X 至少启动一次并注入）",
                    startAction = { RowIcon(Icons.Rounded.Extension) },
                )
                BasicComponent(
                    title = "当前国家 / 地区",
                    summary = Countries.label(iso),
                    startAction = { RowIcon(Icons.Rounded.Public) },
                )
                BasicComponent(
                    title = "X 版本",
                    summary = xVersion(),
                    startAction = { RowIcon(Icons.Rounded.PhoneAndroid) },
                )
                BasicComponent(
                    title = "Root",
                    summary = if (rootOk) "已授权（uid=0）" else "不可用（su 未授权）",
                    startAction = { RowIcon(Icons.Rounded.Security) },
                )
                BasicComponent(
                    title = "最近一次写入",
                    summary = if (statTime.isBlank()) "尚未写入" else "$statTime  ·  ${statSize}B",
                    startAction = { RowIcon(Icons.Rounded.Save) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = if (busy) "刷新中…" else "刷新",
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                )
                TextButton(text = "打开 X", onClick = { openX() }, modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun HomeStatusCard(
        injected: Boolean,
        waiting: Boolean,
        monet: Boolean,
        iso: String,
        statTime: String,
        statSize: String,
        onOpenApps: () -> Unit,
    ) {
        val colors = MiuixTheme.colorScheme
        val dark = LocalDarkTheme.current
        val active = injected
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.defaultColors(
                    color = when {
                        waiting || !active -> colors.surfaceContainer
                        monet -> colors.secondaryContainer
                        dark -> Color(0xFF1A3825)
                        else -> Color(0xFFDFFAE4)
                    },
                ),
            ) {
                Box(Modifier.fillMaxSize().heightIn(min = 174.dp)) {
                    Box(
                        Modifier.matchParentSize().offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        Icon(
                            if (active) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(170.dp),
                            tint = when {
                                waiting -> colors.onSurfaceVariantActions.copy(alpha = 0.18f)
                                !active -> colors.onSurfaceVariantActions.copy(alpha = 0.22f)
                                monet -> colors.primary.copy(alpha = 0.8f)
                                else -> Color(0xFF36D167)
                            },
                        )
                    }
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            when {
                                waiting -> "读取中"
                                active -> "已启用"
                                else -> "未启用"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            when {
                                waiting -> "正在通过 su -M 读取 X 侧状态"
                                active -> ("X " + xVersion())
                                else -> "请到 LSPosed 启用模块并勾选 X"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (statTime.isBlank()) "尚未写入配置" else "最近写入 ${statTime.take(19)}",
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    "当前国家 / 地区",
                    Countries.flag(iso) + " " + iso.uppercase(),
                    Modifier.weight(1f),
                    onOpenApps,
                )
                MetricCard(
                    "配置大小",
                    if (statSize.isBlank()) "—" else "${statSize}B",
                    Modifier.weight(1f),
                    onOpenApps,
                )
            }
        }
    }

    /** KernelSU/XBlocker 式的“能力状态”：从模块日志读每个 hook 目标实际挂上了几个方法。 */
    private fun hookCoverage(): String {
        val log = try { RootBridge.readXLog() } catch (t: Throwable) { "" }
        if (log.isBlank()) return "未读到模块日志（X 尚未启动或日志被清）"
        val parts = LinkedHashSet<String>()
        Regex("#(\\w+) hooked=(\\d+)").findAll(log).forEach { m ->
            val n = m.groupValues[1]
            val c = m.groupValues[2]
            parts.add(if (c == "0") n + " ✗0" else n + " ✓" + c)
        }
        return if (parts.isEmpty()) "日志中无 hooked 计数（旧版本）" else parts.joinToString("  ")
    }

    @Composable
    private fun MetricCard(title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
        Card(modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp), onClick = onClick) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }

    // ------------------------------------------------------------------ 应用

    @Composable
    private fun AppsPage() {
        val context = this@MainActivity
        var iso by remember { mutableStateOf(ConfigStore.getIso(context)) }
        var fakeSim by remember { mutableStateOf(ConfigStore.getFakeSim(context)) }
        var fakeSs by remember { mutableStateOf(ConfigStore.getFakeServiceState(context)) }
        var status by remember { mutableStateOf("") }
        val isoList = remember { Countries.LIST.map { it[0] } }
        val countryItems = remember { Countries.labels().map { DropdownItem(it) } }
        val selectedIndex = isoList.indexOf(iso).coerceAtLeast(0)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = RootBridge.TARGET_PACKAGE,
                    summary = "X（Twitter）  ${xVersion()}",
                    startAction = { RowIcon(Icons.Rounded.PhoneAndroid) },
                    endActions = {
                        Text(
                            if (isInstalled()) "已安装" else "未安装",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                )
            }
            Card(Modifier.fillMaxWidth()) {
                OverlaySpinnerPreference(
                    title = "国家 / 地区",
                    summary = "写入 networkCountryIso，X 重启后生效",
                    startAction = { RowIcon(Icons.Rounded.Public) },
                    items = Countries.LIST.map { row -> DropdownItem(Countries.label(row[0])) },
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { index ->
                        iso = isoList[index]
                        status = "已选中 " + Countries.label(iso) + "，点下方保存并应用"
                    },
                )
                SuperSwitch(
                    title = "SIM 国家码",
                    summary = "同时伪装 SIM 卡所属国家",
                    checked = fakeSim,
                    startAction = { RowIcon(Icons.Rounded.SimCard) },
                    onCheckedChange = { fakeSim = it },
                )
                SuperSwitch(
                    title = "ServiceState",
                    summary = "伪装 ServiceState 里的国家码",
                    checked = fakeSs,
                    startAction = { RowIcon(Icons.Rounded.SignalCellularAlt) },
                    onCheckedChange = { fakeSs = it },
                )
            }
            TextButton(
                text = "保存并应用",
                onClick = {
                    val target = iso
                    val sim = fakeSim
                    val ss = fakeSs
                    status = "正在写入…"
                    ConfigStore.save(context, target, sim, ss)
                    ConfigStore.appendLog(
                        context,
                        "[App] 已保存：iso=$target fake_sim=$sim fake_service_state=$ss",
                    )
                    bg {
                        val r = try {
                            RootBridge.pushToX(target, sim, ss, ConfigStore.getHitLog(context))
                        } catch (t: Throwable) {
                            "失败：$t"
                        }
                        ConfigStore.appendLog(context, "[App] 写入 X 配置：$r")
                        post { status = r }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "写入状态",
                    summary = status.ifBlank { "尚未执行写入" },
                    startAction = { RowIcon(Icons.Rounded.Save) },
                )
                BasicComponent(
                    title = "配置文件",
                    summary = RootBridge.X_CONFIG_FILE,
                    startAction = { RowIcon(Icons.Rounded.Description) },
                )
            }
        }
    }

    // ------------------------------------------------------------------ 日志

    @Composable
    private fun LogsPage() {
        val context = this@MainActivity
        var text by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(true) }

        fun reload() {
            loading = true
            bg {
                val app = try {
                    ConfigStore.readLog(context).lineSequence()
                        .lastOrNull { it.isNotBlank() }.orEmpty()
                } catch (t: Throwable) {
                    "读取异常：$t"
                }
                val x = try {
                    RootBridge.readXLogLatest()
                } catch (t: Throwable) {
                    "读取异常：$t"
                }
                post {
                    text = "【X 侧 · 最近一次】\n" + x + "\n\n【App 侧 · 最近一条】\n" +
                        (if (app.isBlank()) "（空）" else app)
                    loading = false
                }
            }
        }

        LaunchedEffect(Unit) { reload() }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "合并视图",
                    summary = "只显示最近一次注入与最近一条记录",
                    startAction = { RowIcon(Icons.Rounded.History) },
                    endActions = {
                        Text(
                            if (loading) "加载中…" else "已加载",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                )
                Row(Modifier.fillMaxWidth()) {
                    TextButton(text = "刷新", onClick = { reload() }, modifier = Modifier.weight(1f))
                    TextButton(
                        text = "清空",
                        onClick = {
                            ConfigStore.clearLog(context)
                            reload()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "验证命令（只读）",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                )
                PRESET_COMMANDS.forEach { preset ->
                    BasicComponent(
                        title = preset.first,
                        summary = preset.second,
                        startAction = { RowIcon(Icons.Rounded.Terminal) },
                        onClick = {
                            bg {
                                val out = try {
                                    RootBridge.execCommand(preset.second)
                                } catch (t: Throwable) {
                                    "FAIL:$t"
                                }
                                ConfigStore.appendLog(
                                    context,
                                    "[App] preset: " + preset.second + " -> " + out.take(300).replace("\n", " | "),
                                )
                                post { text = text + "\n\n$ " + preset.second + "\n" + out }
                            }
                        },
                    )
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Text(
                    if (text.isBlank()) "（暂无日志）" else text,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }

    // ------------------------------------------------------------------ 设置

    @Composable
    private fun SettingsPage(
        colorMode: Int,
        monet: Boolean,
        monetActive: Boolean,
        onColorMode: (Int) -> Unit,
        onMonet: (Boolean) -> Unit,
        blur: Boolean,
        floatingBar: Boolean,
        liquidGlass: Boolean,
        glassEnabled: Boolean,
        onBlur: (Boolean) -> Unit,
        onFloatingBar: (Boolean) -> Unit,
        onLiquidGlass: (Boolean) -> Unit,
        onOpenAppearance: () -> Unit,
        onOpenAbout: () -> Unit,
    ) {
        val context = this@MainActivity
        var note by remember { mutableStateOf("") }
        var hitLog by remember { mutableStateOf(ConfigStore.getHitLog(context)) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                OverlaySpinnerPreference(
                    title = "主题",
                    summary = THEME_MODES.getOrElse(colorMode) { THEME_MODES[0] },
                    startAction = { RowIcon(Icons.Rounded.Palette) },
                    items = THEME_MODES.map { DropdownItem(it) },
                    selectedIndex = colorMode.coerceIn(0, THEME_MODES.lastIndex),
                    onSelectedIndexChange = { index -> onColorMode(index) },
                )
                ArrowPreference(
                    title = "界面风格",
                    summary = "模糊、动态取色、浮动底栏与液态玻璃",
                    startAction = { RowIcon(Icons.Rounded.Tune) },
                    onClick = onOpenAppearance,
                )
            }
            Card(Modifier.fillMaxWidth()) {
                SuperSwitch(
                    title = "hit 日志（排查用）",
                    summary = "关闭时不写 hit/probe 行；开启后重启 X 或等下一次命中生效",
                    checked = hitLog,
                    startAction = { RowIcon(Icons.Rounded.BugReport) },
                    onCheckedChange = { on ->
                        hitLog = on
                        ConfigStore.setHitLog(context, on)
                        ConfigStore.appendLog(context, "[App] hit_log=$on")
                    },
                )
            }
            Card(Modifier.fillMaxWidth()) {
                SuperSwitch(
                    title = "隐藏桌面图标",
                    summary = "只隐藏桌面入口，不影响 LSPosed 模块入口",
                    checked = !launcherEnabled(),
                    startAction = { RowIcon(Icons.Rounded.VisibilityOff) },
                    onCheckedChange = { hide ->
                        note = setLauncherEnabled(!hide)
                        ConfigStore.appendLog(context, "[App] $note")
                    },
                )
            }
            Card(Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "关于",
                    summary = "应用与许可、第三方组件声明",
                    startAction = { RowIcon(Icons.Rounded.Info) },
                    onClick = onOpenAbout,
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    @Composable
    private fun AppearancePage(
        topPadding: Dp,
        bottomPadding: Dp,
        monet: Boolean,
        monetActive: Boolean,
        onMonet: (Boolean) -> Unit,
        blur: Boolean,
        floatingBar: Boolean,
        liquidGlass: Boolean,
        glassEnabled: Boolean,
        onBlur: (Boolean) -> Unit,
        onFloatingBar: (Boolean) -> Unit,
        onLiquidGlass: (Boolean) -> Unit,
        predictiveBack: Boolean,
        onPredictiveBack: (Boolean) -> Unit,
        backProgress: Float,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = backProgress * size.width * 0.35f
                    alpha = 1f - backProgress * 0.25f
                },
            contentPadding = PaddingValues(
                top = topPadding + 12.dp,
                bottom = bottomPadding + 24.dp,
                start = 12.dp,
                end = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    SuperSwitch(
                        title = "Monet 动态取色",
                        summary = if (monetActive) "按壁纸取色（Material You）" else "需要 Android 12+",
                        checked = monet,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        startAction = { RowIcon(Icons.Rounded.Wallpaper) },
                        onCheckedChange = onMonet,
                    )
                    SuperSwitch(
                        title = "模糊",
                        summary = if (Build.VERSION.SDK_INT >= 33) "顶栏与底栏的背景模糊"
                        else "需要 Android 13 及以上",
                        checked = blur,
                        enabled = Build.VERSION.SDK_INT >= 33,
                        startAction = { RowIcon(Icons.Rounded.BlurOn) },
                        onCheckedChange = onBlur,
                    )
                    SuperSwitch(
                        title = "浮动底栏",
                        summary = "Apple 风格悬浮胶囊底栏",
                        checked = floatingBar,
                        startAction = { RowIcon(Icons.Rounded.CallToAction) },
                        onCheckedChange = onFloatingBar,
                    )
                    SuperSwitch(
                        title = "液态玻璃",
                        summary = "给浮动底栏叠加折射与高光",
                        checked = liquidGlass,
                        enabled = floatingBar && glassEnabled,
                        startAction = { RowIcon(Icons.Rounded.WaterDrop) },
                        onCheckedChange = onLiquidGlass,
                    )
                    SuperSwitch(
                        title = "预测性返回手势",
                        summary = if (Build.VERSION.SDK_INT >= 34) "启用 Android 14+ 的返回预览动画"
                        else "需要 Android 14 及以上",
                        checked = predictiveBack,
                        enabled = Build.VERSION.SDK_INT >= 34,
                        startAction = { RowIcon(Icons.AutoMirrored.Rounded.ArrowBack) },
                        onCheckedChange = onPredictiveBack,
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------ 工具

    private fun appVersionCode(): Long = try {
        val pi = packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
    } catch (t: Throwable) {
        -1L
    }

    private fun setLauncherEnabled(enabled: Boolean): String {
        return try {
            packageManager.setComponentEnabledSetting(
                launcherAlias(),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            // 旧版曾直接禁用 MainActivity，导致 LSPosed 模块入口一起消失；这里确保它始终启用。
            packageManager.setComponentEnabledSetting(
                ComponentName(this@MainActivity, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP,
            )
            if (enabled) "桌面图标已恢复"
            else "桌面图标已隐藏；LSPosed 模块入口保留"
        } catch (t: Throwable) {
            "操作失败：$t"
        }
    }

    private fun launcherEnabled(): Boolean = packageManager.getComponentEnabledSetting(
        launcherAlias(),
    ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    private fun launcherAlias(): ComponentName =
        ComponentName(packageName, "$packageName.LauncherAlias")

    /** 把旧版「禁用 MainActivity」的隐藏状态迁移到 alias，并恢复 MainActivity。 */
    private fun migrateLauncherState() {
        val main = ComponentName(this@MainActivity, MainActivity::class.java)
        if (packageManager.getComponentEnabledSetting(main) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        ) {
            packageManager.setComponentEnabledSetting(
                launcherAlias(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        packageManager.setComponentEnabledSetting(
            main,
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            PackageManager.DONT_KILL_APP,
        )
    }


    private fun section(source: String, start: String, end: String): String {
        val from = source.indexOf(start)
        if (from < 0) return ""
        val to = source.indexOf(end, from + start.length)
        if (to < 0) return ""
        return source.substring(from + start.length, to).trim()
    }

    private fun isInstalled(): Boolean = try {
        packageManager.getPackageInfo(RootBridge.TARGET_PACKAGE, 0)
        true
    } catch (t: Throwable) {
        false
    }

    private fun xVersion(): String = try {
        val info: PackageInfo = packageManager.getPackageInfo(RootBridge.TARGET_PACKAGE, 0)
        info.versionName ?: "未知"
    } catch (t: Throwable) {
        "未安装"
    }

    private fun appVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "5.0"
    } catch (t: Throwable) {
        "5.0"
    }

    private fun openX() {
        val intent = packageManager.getLaunchIntentForPackage(RootBridge.TARGET_PACKAGE)
        if (intent == null) {
            toast("未安装 X")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Composable
    private fun RowIcon(icon: ImageVector) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }

    private companion object {
        /** 关于页「GitHub 开源仓库」跳转地址。 */
        const val PROJECT_GITHUB_URL = "https://github.com/BeepSleepySheep/X-region"

        val THEME_MODES = listOf("跟随系统", "浅色", "深色")

        /** 只读预置命令：界面不提供自由输入，仅这三个按钮会调用它们。 */
        val PRESET_COMMANDS = listOf(
            "读取 X 侧配置" to "cat /data/data/com.twitter.android/files/xiso_config.txt",
            "查看 X 侧日志" to "tail -n 30 /data/data/com.twitter.android/files/xiso_log.txt",
            "查看最近一次注入" to "grep -F '已注入' /data/data/com.twitter.android/files/xiso_log.txt | tail -n 1",
        )
    }
}
