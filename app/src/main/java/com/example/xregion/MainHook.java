package com.example.xregion;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.twitter.android";
    private static final String MODULE_PACKAGE = "com.example.xregion";
    private static final String PREFS_NAME = "xiso_config";
    private static final String X_SIDE_CONFIG = RootBridge.X_CONFIG_FILE;
    private static final String X_SIDE_LOG = RootBridge.X_LOG_FILE;
    private static final int MAX_LOG_LINES = 600;
    private static final Uri CONFIG_URI = Uri.parse("content://com.example.xregion.config");
    private static final long TTL_MS = 2000L;
    private static final boolean DEBUG_LOG = true;

    private static final Set<String> SIM_METHODS = new HashSet<>();

    static {
        SIM_METHODS.add("getSimCountryIso");
        SIM_METHODS.add("getSimCountryIsoForPhone");
        SIM_METHODS.add("getSimCountryIsoForSubscription");
    }

    private static final Object LOCK = new Object();
    private static final Object LOG_LOCK = new Object();
    private static final List<String> PENDING = new ArrayList<>();

    private static volatile Application sApp = null;
    private static String sIso = ConfigStore.DEFAULT_ISO;
    private static boolean sFakeSim = false;
    private static boolean sFakeSs = true;
    private static long sLastLoad = 0L;
    private static String sAnnounced = null;
    private static String sSource = null;
    private static boolean sInjected = false;
    private static volatile boolean sHitLog = false;
    private static long sConfigMtime = -1L;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        installProbes();
        census();

        hookStringGetter(TelephonyManager.class,
                "getNetworkCountryIso",
                "getNetworkCountryIsoForPhone",
                "getNetworkCountryIsoForSubscription",
                "getNetworkCountryIsoForDefaultDataSubscription",
                "getSimCountryIso",
                "getSimCountryIsoForPhone",
                "getSimCountryIsoForSubscription");

        hookStringGetter(ServiceState.class, "getCountryIso");

        hookSubscriptionIso();
        hookOperatorsMcc();

        try {
            XposedBridge.hookAllMethods(Instrumentation.class, "callApplicationOnCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    onProcessReady(lpparam.processName);
                }
            });
        } catch (Throwable t) {
            log("监听 Application 创建失败：" + t);
            onProcessReady(lpparam.processName);
        }
    }

    private static Application currentApp() {
        Application app = sApp;
        if (app != null) {
            return app;
        }
        try {
            Object o = XposedHelpers.callStaticMethod(
                    Class.forName("android.app.AndroidAppHelper"), "currentApplication");
            if (o instanceof Application) {
                sApp = (Application) o;
                return sApp;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object o = XposedHelpers.callStaticMethod(
                    Class.forName("android.app.ActivityThread"), "currentApplication");
            if (o instanceof Application) {
                sApp = (Application) o;
                return sApp;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void onProcessReady(String processName) {
        synchronized (LOCK) {
            if (sInjected) {
                return;
            }
            sInjected = true;
            sLastLoad = 0L;
        }
        Config c = currentConfig(true);
        log("已注入 " + processName + "，当前 iso=" + c.iso);
    }

    private static void hookStringGetter(final Class<?> clazz, String... methodNames) {
        for (final String name : methodNames) {
            try {
                java.util.Set<XC_MethodHook.Unhook> un = XposedBridge.hookAllMethods(clazz, name, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.method instanceof Method)) {
                            return;
                        }
                        Method m = (Method) param.method;
                        if (m.getReturnType() != String.class) {
                            return;
                        }
                        Object res = param.getResult();
                        if (res != null && !(res instanceof String)) {
                            return;
                        }
                        Config c = currentConfig(false);
                        boolean shouldFake;
                        if (ServiceState.class.getName().equals(m.getDeclaringClass().getName())) {
                            shouldFake = c.fakeServiceState;
                        } else if (SIM_METHODS.contains(m.getName())) {
                            shouldFake = c.fakeSim;
                        } else {
                            shouldFake = true;
                        }
                        if (shouldFake) {
                            param.setResult(c.iso);
                        }
                        if (sHitLog) {
                            log("hit " + m.getDeclaringClass().getSimpleName() + "#" + m.getName()
                                    + " args=" + java.util.Arrays.toString(param.args)
                                    + " orig=" + res + " -> " + (shouldFake ? c.iso : "no-change")
                                    + " fake_sim=" + c.fakeSim + " fake_ss=" + c.fakeServiceState);
                        }
                    }
                });
                log("hook 成功 " + clazz.getSimpleName() + "#" + name + " hooked=" + un.size());
            } catch (Throwable t) {
                log("hook 失败 " + clazz.getSimpleName() + "#" + name + " : " + t);
            }
        }
    }

    /** Step5: explicit candidate signatures; a missing one is logged, never silently skipped. */
    private static final String[][] SIGS = {
            {"android.telephony.TelephonyManager", "getNetworkCountryIso", ""},
            {"android.telephony.TelephonyManager", "getNetworkCountryIso", "int"},
            {"android.telephony.TelephonyManager", "getNetworkCountryIsoForPhone", "int"},
            {"android.telephony.TelephonyManager", "getNetworkCountryIsoForSubscription", "int"},
            {"android.telephony.TelephonyManager", "getNetworkCountryIsoForDefaultDataSubscription", "int"},
            {"android.telephony.TelephonyManager", "getSimCountryIso", ""},
            {"android.telephony.TelephonyManager", "getSimCountryIsoForPhone", "int"},
            {"android.telephony.TelephonyManager", "getSimCountryIsoForSubscription", "int"},
            {"android.telephony.TelephonyManager", "getNetworkOperator", ""},
            {"android.telephony.TelephonyManager", "getNetworkOperator", "int"},
            {"android.telephony.TelephonyManager", "getSimOperator", ""},
            {"android.telephony.TelephonyManager", "getSimOperator", "int"},
            {"android.telephony.TelephonyManager", "getSimOperatorName", ""},
            {"android.telephony.TelephonyManager", "getNetworkOperatorName", ""},
            {"android.telephony.TelephonyManager", "hasSystemFeature", "java.lang.String"},
            {"android.telephony.SubscriptionInfo", "getCountryIso", ""},
            {"android.telephony.SubscriptionManager", "getCountryIso", ""},
            {"android.telephony.ServiceState", "getCountryIso", ""},
    };

    /** V1 follow-up: is each MISSING signature really absent, or hidden-API filtered? */
    private static void census() {
        String[] classes = {
                "android.telephony.ServiceState",
                "android.telephony.SubscriptionManager",
                "android.telephony.TelephonyManager",
                "android.telephony.SubscriptionInfo",
        };
        for (String cn : classes) {
            try {
                Class<?> c = Class.forName(cn);
                Method[] dm = c.getDeclaredMethods();
                Method[] pm = c.getMethods();
                log("census " + c.getSimpleName() + " declared=" + dm.length + " public=" + pm.length
                        + " super=" + c.getSuperclass().getName()
                        + " | declaredIso=" + matchNames(dm) + " | publicIso=" + matchNames(pm));
            } catch (Throwable t) {
                log("census FAIL " + cn + " : " + t);
            }
        }
    }

    private static String matchNames(Method[] ms) {
        StringBuilder sb = new StringBuilder();
        int hits = 0;
        for (Method m : ms) {
            String n = m.getName().toLowerCase(java.util.Locale.ROOT);
            if (n.contains("countryiso") || n.contains("hassystemfeature")) {
                hits++;
                sb.append(m.getName()).append('/').append(m.getParameterCount()).append('p')
                        .append(':').append(m.getReturnType().getSimpleName()).append(' ');
            }
        }
        return hits == 0 ? "-none-" : (hits + " [" + sb.toString().trim() + "]");
    }

    private static void installProbes() {
        for (String[] row : SIGS) {
            installProbeBySignature(row[0], row[1], row[2]);
        }
    }

    private static void installProbeBySignature(final String className, final String methodName,
                                               final String params) {
        String sig = className + "#" + methodName + "(" + params + ")";
        try {
            Class<?> clazz = Class.forName(className);
            Class<?>[] types = paramTypes(params);
            Method m = XposedHelpers.findMethodExactIfExists(className, clazz.getClassLoader(),
                    methodName, (Object[]) types);
            if (m == null) {
                log("sig MISSING " + sig + " (not present on this build)");
                return;
            }
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Method mm = (Method) param.method;
                        probeLog("probe " + mm.getDeclaringClass().getSimpleName() + "#" + mm.getName()
                                + " args=" + java.util.Arrays.toString(param.args)
                                + " ret=" + param.getResult() + " caller=" + callers());
                    } catch (Throwable ignored) {
                    }
                }
            });
            log("sig ok " + sig + " decl=" + m.getDeclaringClass().getName());
        } catch (Throwable t) {
            log("sig FAIL " + sig + " : " + t);
        }
    }

    private static Class<?>[] paramTypes(String params) {
        if (params == null || params.isEmpty()) {
            return new Class<?>[0];
        }
        String[] parts = params.split(",");
        Class<?>[] out = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if ("int".equals(p)) {
                out[i] = Integer.TYPE;
            } else if ("long".equals(p)) {
                out[i] = Long.TYPE;
            } else if ("boolean".equals(p)) {
                out[i] = Boolean.TYPE;
            } else if ("java.lang.String".equals(p)) {
                out[i] = String.class;
            } else {
                out[i] = Object.class;
            }
        }
        return out;
    }

    /** Up to 3 frames from the target app itself (X ships obfuscated but keeps com.x./com.braze./ads_mobile_sdk prefixes). */
    private static String callers() {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        boolean any = false;
        for (StackTraceElement e : new Throwable().getStackTrace()) {
            String cn = e.getClassName();
            if (!(cn.startsWith("com.x.") || cn.startsWith("com.twitter.")
                    || cn.startsWith("com.braze.") || cn.startsWith("ads_mobile_sdk."))) {
                continue;
            }
            any = true;
            if (n > 0) {
                sb.append(" < ");
            }
            sb.append(cn).append('#').append(e.getMethodName()).append(':').append(e.getLineNumber());
            if (++n >= 3) {
                break;
            }
        }
        return any ? sb.toString() : "-";
    }

    /** Probe lines bypass DEBUG_LOG but follow the hit_log switch (Step7, default off). */
    private static void probeLog(String msg) {
        if (!sHitLog) {
            return;
        }
        String line = "[XRegion] " + msg;
        XposedBridge.log(line);
        writeXSideLog(line);
    }

    /** Step6: config file mtime, so a changed xiso_config.txt is picked up without restarting X. */
    private static long configFileMtime() {
        try {
            File f = new File(X_SIDE_CONFIG);
            return f.exists() ? f.lastModified() : -1L;
        } catch (Throwable t) {
            return -1L;
        }
    }

    /** Step2: SubscriptionInfo/SubscriptionManager.getCountryIso (Sardine reads) — gated by fake_sim. */
    private static void hookSubscriptionIso() {
        for (String cn : new String[]{"android.telephony.SubscriptionInfo",
                "android.telephony.SubscriptionManager"}) {
            try {
                Class<?> c = Class.forName(cn);
                java.util.Set<XC_MethodHook.Unhook> un = XposedBridge.hookAllMethods(c, "getCountryIso", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.method instanceof Method)) {
                            return;
                        }
                        Method m = (Method) param.method;
                        if (m.getReturnType() != String.class) {
                            return;
                        }
                        Object res = param.getResult();
                        if (res != null && !(res instanceof String)) {
                            return;
                        }
                        Config c2 = currentConfig(false);
                        if (c2.fakeSim) {
                            param.setResult(c2.iso);
                        }
                        if (sHitLog) {
                            log("hit " + m.getDeclaringClass().getSimpleName() + "#" + m.getName()
                                    + " args=" + java.util.Arrays.toString(param.args)
                                    + " orig=" + res + " -> " + (c2.fakeSim ? c2.iso : "no-change")
                                    + " fake_sim=" + c2.fakeSim);
                        }
                    }
                });
                log("hook ok " + c.getSimpleName() + "#getCountryIso (fake_sim) hooked=" + un.size());
            } catch (Throwable t) {
                log("hook FAIL " + cn + "#getCountryIso : " + t);
            }
        }
    }


    /** Step3: keep the MCC inside getNetworkOperator / getSimOperator consistent with the faked ISO. */
    private static void hookOperatorsMcc() {
        hookOperator("getNetworkOperator", false);
        hookOperator("getSimOperator", true);
    }

    private static void hookOperator(final String name, final boolean needsFakeSim) {
        try {
            java.util.Set<XC_MethodHook.Unhook> un = XposedBridge.hookAllMethods(TelephonyManager.class, name, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.method instanceof Method)) {
                        return;
                    }
                    Method m = (Method) param.method;
                    if (m.getReturnType() != String.class) {
                        return;
                    }
                    Object res = param.getResult();
                    Config c = currentConfig(false);
                    if (needsFakeSim && !c.fakeSim) {
                        return;
                    }
                    String mcc = Countries.mcc(c.iso);
                    if (mcc.length() != 3) {
                        return;
                    }
                    String fake = mcc + "01";
                    param.setResult(fake);
                    if (sHitLog) {
                        log("hit TelephonyManager#" + m.getName() + " args="
                                + java.util.Arrays.toString(param.args) + " orig=" + res + " -> " + fake
                                + " (mcc of iso=" + c.iso + ", fake_sim=" + c.fakeSim + ")");
                    }
                }
            });
            log("hook ok TelephonyManager#" + name + (needsFakeSim ? " (fake_sim, mcc-iso)" : " (mcc-iso)") + " hooked=" + un.size());
        } catch (Throwable t) {
            log("hook FAIL TelephonyManager#" + name + " : " + t);
        }
    }

    private static Config currentConfig(boolean force) {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            long mtime = configFileMtime();
            boolean touched = mtime != sConfigMtime;
            if (touched) {
                sConfigMtime = mtime;
            }
            if (!force && !touched && now - sLastLoad < TTL_MS) {
                return new Config(sIso, sFakeSim, sFakeSs, sHitLog);
            }
            Config c = readConfig();
            sLastLoad = now;
            if (c == null) {
                return new Config(sIso, sFakeSim, sFakeSs, sHitLog);
            }
            String sig = c.iso + "|" + c.fakeSim + "|" + c.fakeServiceState + "|hit=" + c.hitLog;
            boolean changed = !sig.equals(sAnnounced);
            sIso = c.iso;
            sFakeSim = c.fakeSim;
            sFakeSs = c.fakeServiceState;
            sHitLog = c.hitLog;
            if (changed) {
                sAnnounced = sig;
                if (sInjected) {
                    log("配置已更新：iso=" + c.iso + " fake_sim=" + c.fakeSim
                            + " fake_service_state=" + c.fakeServiceState);
                }
            }
            return new Config(sIso, sFakeSim, sFakeSs, sHitLog);
        }
    }

    private static Config readConfig() {
        Config c = readFromXSideFile();
        if (c != null) {
            noteSource("X 数据目录配置文件");
            return c;
        }
        c = readFromProvider();
        if (c != null) {
            noteSource("模块 provider");
            return c;
        }
        c = readFromXSharedPreferences();
        if (c != null) {
            noteSource("XSharedPreferences");
            return c;
        }
        noteSource("默认值（三条通道都没读到）");
        return null;
    }

    private static void noteSource(String src) {
        if (!src.equals(sSource)) {
            sSource = src;
            log("配置来源：" + src);
        }
    }

    private static Config readFromXSideFile() {
        try {
            File f = new File(X_SIDE_CONFIG);
            if (!f.exists() || !f.canRead()) {
                return null;
            }
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            }
            String iso = p.getProperty(ConfigStore.KEY_ISO);
            if (iso == null) {
                return null;
            }
            return new Config(
                    ConfigStore.normalize(iso),
                    "true".equalsIgnoreCase(p.getProperty(ConfigStore.KEY_SIM, "false")),
                    !"false".equalsIgnoreCase(p.getProperty(ConfigStore.KEY_SS, "true")),
                    "true".equalsIgnoreCase(p.getProperty(ConfigStore.KEY_HIT, "false")));
        } catch (Throwable t) {
            return null;
        }
    }

    private static Config readFromProvider() {
        try {
            Application app = currentApp();
            if (app == null) {
                return null;
            }
            ContentResolver cr = app.getContentResolver();
            Bundle b = cr.call(CONFIG_URI, "get_config", null, null);
            if (b == null || b.getBoolean("denied", true)) {
                return null;
            }
            return new Config(
                    ConfigStore.normalize(b.getString("iso")),
                    b.getBoolean("fake_sim", false),
                    b.getBoolean("fake_service_state", true),
                    b.getBoolean("hit_log", false));
        } catch (Throwable t) {
            return null;
        }
    }

    private static Config readFromXSharedPreferences() {
        try {
            XSharedPreferences xsp = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
            xsp.reload();
            Map<String, ?> all = xsp.getAll();
            if (all == null || all.isEmpty()) {
                return null;
            }
            Object iso = all.get(ConfigStore.KEY_ISO);
            Object sim = all.get(ConfigStore.KEY_SIM);
            Object ss = all.get(ConfigStore.KEY_SS);
            return new Config(
                    ConfigStore.normalize(iso == null ? null : String.valueOf(iso)),
                    sim instanceof Boolean && (Boolean) sim,
                    !(ss instanceof Boolean) || (Boolean) ss);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void log(String msg) {
        if (!DEBUG_LOG) {
            return;
        }
        String line = "[XRegion] " + msg;
        XposedBridge.log(line);
        writeXSideLog(line);
        Application app = currentApp();
        if (app == null) {
            synchronized (PENDING) {
                if (PENDING.size() < 100) {
                    PENDING.add(line);
                }
            }
            return;
        }
        writeProviderLog(app, line);
        List<String> copy;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            copy = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (String l : copy) {
            writeProviderLog(app, l);
        }
    }

    /**
     * 把日志写进 X 自己的数据目录，App 侧用 root 就能读到并显示。
     * 只写我们自己的 xiso_log.txt，不动 X 的任何原有文件。
     */
    private static void writeXSideLog(String line) {
        synchronized (LOG_LOCK) {
            try {
                File f = new File(X_SIDE_LOG);
                List<String> lines = new ArrayList<>();
                if (f.exists()) {
                    try (FileInputStream in = new FileInputStream(f);
                         InputStreamReader r = new InputStreamReader(in, "UTF-8");
                         BufferedReader br = new BufferedReader(r)) {
                        String s;
                        while ((s = br.readLine()) != null) {
                            lines.add(s);
                        }
                    }
                }
                String ts = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
                lines.add(ts + "  " + line);
                while (lines.size() > MAX_LOG_LINES) {
                    lines.remove(0);
                }
                try (FileOutputStream fos = new FileOutputStream(f, false);
                     OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8")) {
                    for (String l : lines) {
                        w.write(l);
                        w.write("\n");
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void writeProviderLog(Application app, String line) {
        try {
            app.getContentResolver().call(CONFIG_URI, "log", line, null);
        } catch (Throwable ignored) {
        }
    }

    private static final class Config {
        final String iso;
        final boolean fakeSim;
        final boolean fakeServiceState;
        final boolean hitLog;

        Config(String iso, boolean fakeSim, boolean fakeServiceState) {
            this(iso, fakeSim, fakeServiceState, false);
        }

        Config(String iso, boolean fakeSim, boolean fakeServiceState, boolean hitLog) {
            this.iso = iso;
            this.fakeSim = fakeSim;
            this.fakeServiceState = fakeServiceState;
            this.hitLog = hitLog;
        }
    }
}
