package com.example.xregion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 通过 root 读写 X 自己数据目录里的配置文件与日志文件。
 * 注意：必须用 su -M（全局挂载命名空间），否则 App 自己的命名空间里
 * 根本看不到 /data/data/com.twitter.android，写入会落到影子路径。
 */
public final class RootBridge {

    public static final String TARGET_PACKAGE = "com.twitter.android";
    public static final String X_CONFIG_FILE =
            "/data/data/" + TARGET_PACKAGE + "/files/xiso_config.txt";
    public static final String X_LOG_FILE =
            "/data/data/" + TARGET_PACKAGE + "/files/xiso_log.txt";

    private RootBridge() {
    }

    public static String pushToX(String iso, boolean fakeSim, boolean fakeServiceState) {
        return pushToX(iso, fakeSim, fakeServiceState, false);
    }

    public static String pushToX(String iso, boolean fakeSim, boolean fakeServiceState, boolean hitLog) {
        String dir = "/data/data/" + TARGET_PACKAGE;
        String body = "iso=" + iso + "\\n"
                + "fake_sim=" + fakeSim + "\\n"
                + "fake_service_state=" + fakeServiceState
                + String.valueOf((char) 10) + "hit_log=" + hitLog + "\\n";
        String cmd = "U=$(stat -c '%u' " + dir + " 2>/dev/null); [ -z \"$U\" ] && U=0; "
                + "mkdir -p " + dir + "/files 2>&1; "
                + "printf '" + body + "' > " + X_CONFIG_FILE + "; echo write_rc=$?; "
                + "chown $U " + X_CONFIG_FILE + "; "
                + "chmod 600 " + X_CONFIG_FILE + "; "
                + "ls -l " + X_CONFIG_FILE + "; echo ---; cat " + X_CONFIG_FILE;
        String out = runSu(cmd, 20);
        if (out.startsWith("FAIL:")) {
            return "失败：" + out.substring(5).replace("\n", " | ");
        }
        boolean wrote = out.contains("iso=" + iso) && out.contains("write_rc=0");
        String flat = out.replace("\r", "").replace("\n", " | ").trim();
        return (wrote ? "成功：" : "失败：") + flat;
    }

    public static String readXLog() {
        String cmd = "if [ -f " + X_LOG_FILE + " ]; then tail -80 " + X_LOG_FILE
                + "; else echo __NOFILE__; fi";
        String out = runSu(cmd, 15);
        if (out.startsWith("FAIL:")) {
            return "[X 侧日志读取失败] " + out.substring(5).replace("\n", " | ");
        }
        if (out.trim().equals("__NOFILE__")) {
            return "[X 侧暂无日志文件] 重启 X 后才有记录";
        }
        return out.trim();
    }

    /** 只取最近一次注入与最后一条记录，供日志页精简展示。 */
    public static String readXLogLatest() {
        String cmd = "if [ -f " + X_LOG_FILE + " ]; then "
                + "grep -F '已注入' " + X_LOG_FILE + " | tail -n 1; "
                + "echo '__LAST__'; "
                + "tail -n 1 " + X_LOG_FILE + "; "
                + "else echo __NOFILE__; fi";
        String out = runSu(cmd, 15);
        if (out.startsWith("FAIL:")) {
            return "[X 侧日志读取失败] " + out.substring(5).replace("\n", " | ");
        }
        if (out.trim().equals("__NOFILE__")) {
            return "[X 侧暂无日志文件] 重启 X 后才有记录";
        }

        String inject = "";
        String last = "";
        boolean afterLast = false;
        for (String line : out.split("\n")) {
            if (line.equals("__LAST__")) {
                afterLast = true;
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            if (afterLast) {
                last = line;
            } else {
                inject = line;
            }
        }
        return "最近一次注入：\n" + (inject.isEmpty() ? "（无）" : inject)
                + "\n\n最近一条记录：\n" + (last.isEmpty() ? "（无）" : last);
    }

    /** 通用命令执行（日志页的命令行验证用）。 */
    public static String execCommand(String cmd) {
        return runSu(cmd, 30);
    }

    /** 一次性取回：root 身份 / 配置文件时间戳与大小 / X 侧日志尾部，带分隔标记。 */
    public static String snapshot() {
        String cmd = "echo '##ID'; id 2>&1; "
                + "echo '##STAT'; stat -c '%y|%s' " + X_CONFIG_FILE + " 2>&1; "
                + "echo '##TAIL'; tail -60 " + X_LOG_FILE + " 2>&1; "
                + "echo '##END'";
        return runSu(cmd, 20);
    }

    private static String runSu(String cmd, int timeoutSeconds) {
        String out = exec(new String[]{"su", "-M", "-c", cmd}, timeoutSeconds);
        if (out.startsWith("FAIL:")) {
            String flat = out.replace("\n", " ");
            boolean unsupported = flat.contains("invalid") || flat.contains("unknown")
                    || flat.contains("Unknown") || flat.contains("not supported");
            if (unsupported) {
                out = exec(new String[]{"su", "-c", cmd}, timeoutSeconds);
            }
        }
        return out;
    }

    private static String exec(String[] argv, int timeoutSeconds) {
        Process p = null;
        try {
            p = new ProcessBuilder(argv).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroy();
                return "FAIL:su 超时（可能需要在 KernelSU 里再点一次允许）";
            }
            int code = p.exitValue();
            String out = sb.toString().trim();
            if (code != 0) {
                return "FAIL:exit=" + code + " out=" + out.replace("\n", " | ");
            }
            return out;
        } catch (Throwable t) {
            return "FAIL:" + t;
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
