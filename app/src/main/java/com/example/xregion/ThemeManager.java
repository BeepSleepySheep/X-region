package com.example.xregion;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.TypedValue;

/** UI 偏好：MiuiX 观感 / Monet 动态取色 / 模糊 / 浮动底栏（FloatingBottomBar）/ 底栏液态玻璃。 */
public final class ThemeManager {

    private static final String PREF = "xiso_ui";
    private static final String KEY_MONET = "monet";
    private static final String KEY_BLUR = "enable_blur";
    private static final String KEY_RADIUS = "glass_radius";
    private static final String KEY_FLOATING_BAR = "enable_floating_bottom_bar";
    private static final String KEY_FLOATING_BAR_BLUR = "enable_floating_bottom_bar_blur";
    private static final String KEY_PREDICTIVE_BACK = "predictive_back";

    private ThemeManager() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isMonet(Context c) {
        return prefs(c).getBoolean(KEY_MONET, false);
    }

    public static void setMonet(Context c, boolean monet) {
        prefs(c).edit().putBoolean(KEY_MONET, monet).apply();
    }

    /** 全局模糊开关；沿用 KernelSU 的默认值：true。 */
    public static boolean isBlur(Context c) {
        return prefs(c).getBoolean(KEY_BLUR, true);
    }

    public static void setBlur(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_BLUR, on).apply();
    }

    /** 浮动底栏开关；沿用 KernelSU 的默认值：false（默认是普通底栏）。 */
    public static boolean isFloatingBar(Context c) {
        return prefs(c).getBoolean(KEY_FLOATING_BAR, true);
    }

    public static void setFloatingBar(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_FLOATING_BAR, on).apply();
    }

    /** 浮动底栏的液态玻璃（backdrop）开关；沿用 KernelSU 的默认值：true。 */
    public static boolean isFloatingBarBlur(Context c) {
        return prefs(c).getBoolean(KEY_FLOATING_BAR_BLUR, true);
    }

    public static void setFloatingBarBlur(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_FLOATING_BAR_BLUR, on).apply();
    }

    /** 预测性返回手势；沿用 Android 14+ 的默认行为：开启。 */
    public static boolean isPredictiveBack(Context c) {
        return prefs(c).getBoolean(KEY_PREDICTIVE_BACK, true);
    }

    public static void setPredictiveBack(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_PREDICTIVE_BACK, on).apply();
    }

    public static int getBlurRadius(Context c) {
        return prefs(c).getInt(KEY_RADIUS, 30);
    }

    public static void setBlurRadius(Context c, int px) {
        prefs(c).edit().putInt(KEY_RADIUS, px).apply();
    }

    public static boolean monetSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static boolean blurSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /** 兼容旧调用：返回一组基础色（Compose 层现在用 Miuix 的 colorScheme）。 */
    public static Palette palette(Context c, boolean monet) {
        boolean night = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Palette p = new Palette();
        if (night) {
            p.bg = 0xFF111214;
            p.card = 0xFF1C1E21;
            p.card2 = 0xFF24272B;
            p.text = 0xFFF3F4F6;
            p.sub = 0xFF9AA0A6;
            p.divider = 0xFF2C2F34;
            p.navOverlay = 0x661A1C1F;
            p.navSolid = 0xFF1A1C1F;
            p.accent = 0xFF8AB4FF;
            p.onAccent = 0xFF0A1220;
        } else {
            p.bg = 0xFFF3F4F7;
            p.card = 0xFFFFFFFF;
            p.card2 = 0xFFF7F8FA;
            p.text = 0xFF14161A;
            p.sub = 0xFF7C828A;
            p.divider = 0xFFEBEDF0;
            p.navOverlay = 0x80FFFFFF;
            p.navSolid = 0xFFFBFCFE;
            p.accent = 0xFF3482FF;
            p.onAccent = 0xFFFFFFFF;
        }
        if (monet && monetSupported()) {
            p.accent = dynamic(c, android.R.color.system_accent1_500, p.accent);
            p.navSolid = dynamic(c, android.R.color.system_neutral1_50, p.navSolid);
        }
        return p;
    }

    public static Palette palette(Context c) {
        return palette(c, isMonet(c));
    }

    private static int dynamic(Context c, int resId, int fallback) {
        try {
            TypedValue tv = new TypedValue();
            if (c.getTheme().resolveAttribute(resId, tv, true) && tv.resourceId != 0) {
                return c.getColor(tv.resourceId);
            }
            return c.getColor(resId);
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static final class Palette {
        public int bg;
        public int card;
        public int card2;
        public int text;
        public int sub;
        public int divider;
        public int accent;
        public int onAccent;
        public int navOverlay;
        public int navSolid;
    }
}
