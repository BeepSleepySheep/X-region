package com.example.xregion;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ConfigStore {

    public static final String PREFS = "xiso_config";
    public static final String KEY_ISO = "iso";
    public static final String KEY_SIM = "fake_sim";
    public static final String KEY_SS = "fake_service_state";
    public static final String KEY_HIT = "hit_log";
    public static final String LOG_FILE = "xiso_log.txt";
    public static final int MAX_LINES = 300;
    public static final String DEFAULT_ISO = "us";

    private ConfigStore() {
    }

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getIso(Context c) {
        return normalize(prefs(c).getString(KEY_ISO, DEFAULT_ISO));
    }

    public static boolean getFakeSim(Context c) {
        return prefs(c).getBoolean(KEY_SIM, false);
    }

    public static boolean getFakeServiceState(Context c) {
        return prefs(c).getBoolean(KEY_SS, true);
    }

    public static boolean getHitLog(Context c) {
        return prefs(c).getBoolean(KEY_HIT, false);
    }

    public static void setHitLog(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_HIT, on).commit();
    }

    public static void save(Context c, String iso, boolean fakeSim, boolean fakeServiceState) {
        String normalized = normalize(iso);
        prefs(c).edit()
                .putString(KEY_ISO, normalized)
                .putBoolean(KEY_SIM, fakeSim)
                .putBoolean(KEY_SS, fakeServiceState)
                .commit();
        mirrorForXSharedPreferences(c, normalized, fakeSim, fakeServiceState);
    }

    /**
     * Android 16 把 SharedPreferences 搬到了 /data/misc/apexdata/...，
     * 传统 XSharedPreferences 读的 /data/data/<pkg>/shared_prefs 路径不再存在，
     * 所以这里额外镜像一份标准 XML 并放宽读权限。
     */
    private static void mirrorForXSharedPreferences(Context c, String iso, boolean fakeSim, boolean fakeSs) {
        try {
            File dir = new File(c.getDataDir(), "shared_prefs");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File f = new File(dir, PREFS + ".xml");
            String xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                    + "<map>\n"
                    + "    <string name=\"" + KEY_ISO + "\">" + iso + "</string>\n"
                    + "    <boolean name=\"" + KEY_SIM + "\" value=\"" + fakeSim + "\" />\n"
                    + "    <boolean name=\"" + KEY_SS + "\" value=\"" + fakeSs + "\" />\n"
                    + "</map>\n";
            try (FileOutputStream fos = new FileOutputStream(f, false);
                 OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8")) {
                w.write(xml);
                w.flush();
            }
            f.setReadable(true, false);
            dir.setReadable(true, false);
            dir.setExecutable(true, false);
        } catch (Throwable ignored) {
        }
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return DEFAULT_ISO;
        }
        String s = raw.trim();
        if (s.length() != 2) {
            return DEFAULT_ISO;
        }
        if (!Character.isLetter(s.charAt(0)) || !Character.isLetter(s.charAt(1))) {
            return DEFAULT_ISO;
        }
        return s.toLowerCase(Locale.ROOT);
    }

    public static synchronized void appendLog(Context c, String line) {
        if (line == null) {
            return;
        }
        List<String> lines = readLines(c);
        String ts = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
        lines.add(ts + "  " + line);
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
        File f = new File(c.getFilesDir(), LOG_FILE);
        try (FileOutputStream fos = new FileOutputStream(f, false);
             OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8")) {
            for (String l : lines) {
                w.write(l);
                w.write("\n");
            }
        } catch (Throwable ignored) {
        }
    }

    public static synchronized List<String> readLines(Context c) {
        List<String> out = new ArrayList<>();
        File f = new File(c.getFilesDir(), LOG_FILE);
        if (!f.exists()) {
            return out;
        }
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader r = new InputStreamReader(fis, "UTF-8");
             BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                out.add(line);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    public static synchronized String readLog(Context c) {
        StringBuilder sb = new StringBuilder();
        for (String l : readLines(c)) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    public static synchronized void clearLog(Context c) {
        File f = new File(c.getFilesDir(), LOG_FILE);
        if (f.exists()) {
            f.delete();
        }
    }
}
