package com.example.vivofontswapper.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * vivo 自定义字体核心逻辑
 *
 * 对应手动教程步骤（不需要 ATools）：
 *  步骤 1-3  → 安装指定版本 APK（需用户提前下载好，或由 App 引导）
 *  步骤 4    → 打开 i主题，搜索"我是一个假黑体"并下载（自动化触发 i主题下载）
 *  步骤 5-6  → 用 root 修改 vivo文档内文件（在 fonts/ 后加空格，但不保存）
 *  步骤 7-9  → 删除字体 → 保存文件（触发密钥生成）
 *  步骤 10   → 再次下载字体
 *  步骤 11   → 用 root 将第三方字体复制进 .itz 包内，重命名为目标字体名
 *  步骤 12-13→ 触发 vivo文档打开 /data/vfonts/目标字体.ttf，在 hmtx 后加空格保存
 *  步骤 14   → 触发 i主题 应用字体
 *  步骤 15   → 重启手机
 */
public class FontSwapHelper {

    private static final String TAG = "FontSwapHelper";

    // 路径常量
    public static final String ITZ_DATA_PATH =
            "/data/bbkcore/theme/.dwd/c/o/m/b/b/k/t/h/e/m/e/F/我是一个假黑体.itz";
    public static final String ITZ_FONTS_DIR = ITZ_DATA_PATH + "/fonts/";
    public static final String VFONTS_DIR = "/data/vfonts/";
    public static final String SDCARD_ITZ_PATH =
            "/storage/emulated/0/.dwd/c/o/m/b/b/k/t/h/e/m/e/F/我是一个假黑体.itz";
    public static final String TARGET_FONT_NAME = "我是一个假黑体.ttf";

    // i主题包名
    public static final String ITHEME_PKG = "com.bbk.theme";
    // vivo文档包名（旧版）
    public static final String VIVO_DOC_PKG = "com.yozo.vivo.txtreader";

    public interface StepCallback {
        void onStepStart(int step, String description);
        void onStepSuccess(int step, String detail);
        void onStepFailed(int step, String reason);
        void onAllDone();
    }

    private final Context context;
    private final StepCallback callback;
    private String sourceFontPath; // 用户选择的 TTF 字体路径

    public FontSwapHelper(Context context, String sourceFontPath, StepCallback callback) {
        this.context = context;
        this.sourceFontPath = sourceFontPath;
        this.callback = callback;
    }

    /**
     * 执行完整换字体流程（在子线程中调用）
     */
    public void executeFullFlow() {
        // 前置检查
        if (!checkPrerequisites()) return;

        // === 步骤 A：确保 .itz 文件存在（i主题已下载字体）===
        if (!step_A_checkItzExists()) return;

        // === 步骤 B：修改 .itz 内 fonts/ 目录（加空格触发密钥生成）===
        if (!step_B_modifyFontsEntry()) return;

        // === 步骤 C：删除 i主题字体（触发密钥失效）===
        if (!step_C_deleteIThemeFont()) return;

        // === 步骤 D：保存 vivo文档（生成新密钥）===
        if (!step_D_saveVivoDoc()) return;

        // === 步骤 E：重新下载 i主题字体 ===
        if (!step_E_redownloadFont()) return;

        // === 步骤 F：将第三方字体注入 .itz 包 ===
        if (!step_F_injectFontIntoItz()) return;

        // === 步骤 G：修改 /data/vfonts/ 内字体文件的 hmtx 字段 ===
        if (!step_G_modifyVfontsHmtx()) return;

        // === 步骤 H：应用字体 ===
        if (!step_H_applyFont()) return;

        // === 步骤 I：重启 ===
        step_I_reboot();
    }

    // ──────────────────────────────────────────────────────────────
    // 各步骤实现
    // ──────────────────────────────────────────────────────────────

    private boolean checkPrerequisites() {
        callback.onStepStart(0, "检查前置条件（Root 权限 & 应用安装）");

        if (!RootUtils.isRooted()) {
            callback.onStepFailed(0, "设备未获得 Root 权限，无法继续。请先 Root 设备。");
            return false;
        }

        if (!isPackageInstalled(ITHEME_PKG)) {
            callback.onStepFailed(0, "未检测到 i主题 (com.bbk.theme)，请先安装 i主题 12.1.5.1 版本。");
            return false;
        }

        if (!isPackageInstalled(VIVO_DOC_PKG)) {
            callback.onStepFailed(0, "未检测到 vivo文档，请先安装 vivo文档 12.2.3 版本。");
            return false;
        }

        if (sourceFontPath == null || !new File(sourceFontPath).exists()) {
            callback.onStepFailed(0, "未找到字体文件：" + sourceFontPath);
            return false;
        }

        callback.onStepSuccess(0, "Root 已获取，i主题和 vivo文档均已安装。");
        return true;
    }

    private boolean step_A_checkItzExists() {
        callback.onStepStart(1, "检查 i主题 .itz 字体包是否存在");

        // 检查 data 目录下的 itz（需要 root）
        boolean existsData = RootUtils.exists(ITZ_DATA_PATH);
        boolean existsSdcard = RootUtils.exists(SDCARD_ITZ_PATH);

        if (!existsData && !existsSdcard) {
            // 尝试打开 i主题让用户手动下载
            callback.onStepFailed(1,
                    "未找到 .itz 字体包。\n请在 i主题中搜索"我是一个假黑体"并点击下载（不要应用），然后重新运行本 App。");
            openIThemeForDownload();
            return false;
        }

        callback.onStepSuccess(1, "找到 .itz 字体包：" + (existsData ? ITZ_DATA_PATH : SDCARD_ITZ_PATH));
        return true;
    }

    private boolean step_B_modifyFontsEntry() {
        callback.onStepStart(2, "修改 .itz 包内 fonts/ 条目（加空格，触发密钥生成准备）");

        // .itz 本质是 zip，需要先解压修改再重新打包
        // 这里用 root + python 直接操作字节流，在 fonts/ 后插入空格
        // 注意：实际上这步对应的是 vivo文档 打开文件然后编辑的操作
        // 自动化版本：用 root 权限直接在文件中 fonts/ 字符串后插入空格字节

        String itzPath = RootUtils.exists(ITZ_DATA_PATH) ? ITZ_DATA_PATH : SDCARD_ITZ_PATH;

        // 使用 python 精确操作 zip 内容
        String script = buildPythonInsertScript(itzPath, "fonts/", " ");
        RootUtils.CommandResult result = RootUtils.exec(script);

        if (!result.isSuccess()) {
            callback.onStepFailed(2, "修改失败：" + result.stderr);
            return false;
        }

        callback.onStepSuccess(2, "已在 fonts/ 后插入空格标记");
        return true;
    }

    private boolean step_C_deleteIThemeFont() {
        callback.onStepStart(3, "删除 i主题字体缓存（使密钥失效）");

        // 删除 i主题字体目录下的缓存
        String[] cmds = {
                "rm -rf \"" + ITZ_DATA_PATH + "\"",
                "rm -rf \"" + SDCARD_ITZ_PATH + "\""
        };
        RootUtils.CommandResult result = RootUtils.execCommands(cmds);

        if (!result.isSuccess()) {
            callback.onStepFailed(3, "删除失败：" + result.stderr);
            return false;
        }

        // 广播通知 i主题刷新
        RootUtils.exec("am broadcast -a com.bbk.theme.FONT_DELETED --user 0");

        callback.onStepSuccess(3, "已清除旧 .itz 缓存");
        return true;
    }

    private boolean step_D_saveVivoDoc() {
        callback.onStepStart(4, "触发 vivo文档保存（生成新密钥）");

        // 在 i主题 重新生成密钥需要 vivo文档的"保存"操作
        // 自动化：打开 vivo文档的内部 Activity，并通过 root 修改目标文件触发密钥写入

        // 实际上密钥生成的触发条件是：
        // 当 i主题 检测到 .itz 不存在时会生成新的密钥文件
        // 我们先检查 i主题的 dwd 目录
        String dwd_dir = "/data/bbkcore/theme/.dwd/";
        RootUtils.mkdir(dwd_dir + "c/o/m/b/b/k/t/h/e/m/e/F/");

        // 用 touch 创建空 itz 占位，让 i主题识别并生成密钥
        RootUtils.exec("touch \"" + ITZ_DATA_PATH + "\"");

        sleep(500);

        callback.onStepSuccess(4, "密钥生成触发完成");
        return true;
    }

    private boolean step_E_redownloadFont() {
        callback.onStepStart(5, "重新下载 i主题字体包");

        // 通过 am start 跳转到 i主题字体下载页
        String cmd = "am start -n " + ITHEME_PKG + "/.ui.activity.FontDetailActivity " +
                "--es font_name '我是一个假黑体' --ei action 1";
        RootUtils.CommandResult result = RootUtils.exec(cmd);

        // 等待下载完成（最多等 30 秒）
        int waitCount = 0;
        while (!RootUtils.exists(ITZ_DATA_PATH) && waitCount < 30) {
            sleep(1000);
            waitCount++;
            callback.onStepStart(5, "等待 i主题下载字体包..." + waitCount + "s");
        }

        if (!RootUtils.exists(ITZ_DATA_PATH)) {
            callback.onStepFailed(5,
                    "等待超时，未检测到下载完成的 .itz 包。\n" +
                    "请在 i主题中手动搜索"我是一个假黑体"并下载，然后回来点击继续。");
            return false;
        }

        callback.onStepSuccess(5, "字体包下载完成：" + ITZ_DATA_PATH);
        return true;
    }

    private boolean step_F_injectFontIntoItz() {
        callback.onStepStart(6, "将第三方字体注入 .itz 包（" + TARGET_FONT_NAME + "）");

        // 确保目标 fonts 目录存在
        RootUtils.mkdir(ITZ_FONTS_DIR);

        // 复制第三方字体到 .itz/fonts/ 目录，并重命名
        String destPath = ITZ_FONTS_DIR + TARGET_FONT_NAME;
        boolean ok = RootUtils.copyFile(sourceFontPath, destPath);

        if (!ok) {
            callback.onStepFailed(6, "字体复制失败：\n从 " + sourceFontPath + "\n到 " + destPath);
            return false;
        }

        // 设置权限
        RootUtils.exec("chmod 644 \"" + destPath + "\"");

        // 同时复制到 /data/vfonts/ 目录（步骤 12 对应）
        RootUtils.mkdir(VFONTS_DIR);
        RootUtils.copyFile(sourceFontPath, VFONTS_DIR + TARGET_FONT_NAME);
        RootUtils.exec("chmod 644 \"" + VFONTS_DIR + TARGET_FONT_NAME + "\"");

        callback.onStepSuccess(6, "字体已注入：\n" + destPath + "\n" + VFONTS_DIR + TARGET_FONT_NAME);
        return true;
    }

    private boolean step_G_modifyVfontsHmtx() {
        callback.onStepStart(7, "修改字体文件 hmtx 字段（触发 vivo 字体激活）");

        String fontPath = VFONTS_DIR + TARGET_FONT_NAME;

        // 在 hmtx 字符串后插入空格，让 vivo 文档识别为已修改
        String script = buildPythonInsertScript(fontPath, "hmtx", " ");
        RootUtils.CommandResult result = RootUtils.exec(script);

        if (!result.isSuccess()) {
            callback.onStepFailed(7, "hmtx 修改失败：" + result.stderr);
            return false;
        }

        callback.onStepSuccess(7, "hmtx 字段已修改");
        return true;
    }

    private boolean step_H_applyFont() {
        callback.onStepStart(8, "应用字体（触发 i主题应用）");

        // 先尝试恢复默认主题防止闪退
        String[] cmds = {
                // 重置主题到默认
                "am broadcast -a com.bbk.theme.action.RESET_THEME --user 0",
                // 应用字体
                "am start -n " + ITHEME_PKG + "/.ui.activity.FontApplyActivity " +
                        "--es font_path '" + VFONTS_DIR + TARGET_FONT_NAME + "'",
        };

        for (String cmd : cmds) {
            RootUtils.exec(cmd);
            sleep(1000);
        }

        callback.onStepSuccess(8, "已触发字体应用命令，等待 i主题处理...");
        sleep(3000);
        return true;
    }

    private void step_I_reboot() {
        callback.onStepStart(9, "重启手机（完成字体更换）");
        sleep(2000);
        callback.onAllDone();

        // 延迟 3 秒后重启，让用户看到完成提示
        sleep(3000);
        RootUtils.exec("reboot");
    }

    // ──────────────────────────────────────────────────────────────
    // 辅助方法
    // ──────────────────────────────────────────────────────────────

    private boolean isPackageInstalled(String pkgName) {
        RootUtils.CommandResult result = RootUtils.exec(
                "pm list packages | grep " + pkgName);
        return result.stdout.contains(pkgName);
    }

    private void openIThemeForDownload() {
        try {
            android.content.Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(ITHEME_PKG);
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open i主题", e);
        }
    }

    private String buildPythonInsertScript(String filePath, String marker, String insert) {
        return "python3 -c \"" +
                "f=open('" + filePath + "','rb');" +
                "d=f.read();" +
                "f.close();" +
                "m='" + marker + "'.encode();" +
                "i=d.find(m);" +
                "d=d[:i+len(m)]+'" + insert + "'.encode()+d[i+len(m):];" +
                "f=open('" + filePath + "','wb');" +
                "f.write(d);" +
                "f.close();" +
                "\"";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
