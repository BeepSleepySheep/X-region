package com.example.xregion.ui

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

/**
 * 与 XBlocker ui/MainActivity.kt:148 完全相同的用法：`LocalDarkTheme provides darkTheme`。
 * FloatingBottomBar.kt / Theme.kt 都靠它判断深浅色。
 */
val LocalDarkTheme = compositionLocalOf { false }

/**
 * 外观偏好。三个开关的键名沿用 KernelSU manager 与 XBlocker AppearanceSettings 的命名
 * （enable_blur / enable_floating_bottom_bar / enable_floating_bottom_bar_blur），
 * 由已有的 ThemeManager.java 持久化；这里只补一个浅/深/跟随系统的三态。
 */
object UiPrefs {

    private const val PREFS = "xiso_ui"
    private const val KEY_COLOR_MODE = "color_mode"

    /** 0 = 跟随系统，1 = 浅色，2 = 深色；配合 Monet 开关得到 XBlocker 的 0..5 六种模式。 */
    fun colorMode(c: Context): Int =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COLOR_MODE, 0)

    fun setColorMode(c: Context, value: Int) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_COLOR_MODE, value).apply()
    }
}
