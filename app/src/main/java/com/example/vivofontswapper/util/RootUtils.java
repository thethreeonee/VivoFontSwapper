package com.example.vivofontswapper.util;

import android.util.Log;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Root 命令执行工具类
 * 通过 su 进程执行需要 root 权限的命令
 */
public class RootUtils {

    private static final String TAG = "RootUtils";

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    /** 检查设备是否已 root */
    public static boolean isRooted() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains("uid=0");
        } catch (Exception e) {
            Log.e(TAG, "Root check failed: " + e.getMessage());
            return false;
        }
    }

    /** 执行单条 root 命令 */
    public static CommandResult exec(String command) {
        return execCommands(new String[]{command});
    }

    /** 批量执行 root 命令 */
    public static CommandResult execCommands(String[] commands) {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            for (String cmd : commands) {
                Log.d(TAG, "Executing: " + cmd);
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();

            // 读取输出
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "stdout read error", e);
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "stderr read error", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            exitCode = process.waitFor();
            stdoutThread.join(3000);
            stderrThread.join(3000);

        } catch (Exception e) {
            Log.e(TAG, "execCommands failed", e);
            stderr.append(e.getMessage());
        }

        return new CommandResult(exitCode, stdout.toString(), stderr.toString());
    }

    /** 以 root 身份复制文件 */
    public static boolean copyFile(String src, String dst) {
        CommandResult result = exec("cp -f \"" + src + "\" \"" + dst + "\"");
        return result.isSuccess();
    }

    /** 以 root 身份创建目录 */
    public static boolean mkdir(String path) {
        CommandResult result = exec("mkdir -p \"" + path + "\"");
        return result.isSuccess();
    }

    /** 检查文件/目录是否存在 */
    public static boolean exists(String path) {
        CommandResult result = exec("test -e \"" + path + "\" && echo yes || echo no");
        return result.stdout.trim().equals("yes");
    }

    /** 列出目录内容 */
    public static List<String> listDir(String path) {
        List<String> files = new ArrayList<>();
        CommandResult result = exec("ls \"" + path + "\"");
        if (result.isSuccess()) {
            for (String line : result.stdout.split("\n")) {
                if (!line.trim().isEmpty()) {
                    files.add(line.trim());
                }
            }
        }
        return files;
    }

    /** 读取文件内容（小文件）*/
    public static String readFile(String path) {
        CommandResult result = exec("cat \"" + path + "\"");
        return result.isSuccess() ? result.stdout : null;
    }

    /** 以 root 身份修改文件内容（将 searchStr 替换为 replaceStr）*/
    public static boolean sedReplace(String filePath, String searchStr, String replaceStr) {
        // 转义特殊字符
        String escaped = searchStr.replace("/", "\\/").replace(".", "\\.");
        String escapedReplace = replaceStr.replace("/", "\\/");
        String cmd = "sed -i 's/" + escaped + "/" + escapedReplace + "/g' \"" + filePath + "\"";
        CommandResult result = exec(cmd);
        return result.isSuccess();
    }

    /** 在文件指定字符串后追加内容（用于 itz 内文件修改）*/
    public static boolean insertAfter(String filePath, String afterStr, String insertContent) {
        // 使用 python 来处理二进制安全的文件修改
        String script = "python3 -c \"\n" +
                "import sys\n" +
                "path = sys.argv[1]\n" +
                "with open(path, 'rb') as f:\n" +
                "    data = f.read()\n" +
                "marker = sys.argv[2].encode()\n" +
                "insert = sys.argv[3].encode()\n" +
                "idx = data.find(marker)\n" +
                "if idx == -1:\n" +
                "    sys.exit(1)\n" +
                "new_data = data[:idx+len(marker)] + insert + data[idx+len(marker):]\n" +
                "with open(path, 'wb') as f:\n" +
                "    f.write(new_data)\n" +
                "\" \"" + filePath + "\" \"" + afterStr + "\" \"" + insertContent + "\"";
        CommandResult result = exec(script);
        return result.isSuccess();
    }
}
