package com.example.vivofontswapper.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * vivo 字体替换流程助手
 */
public class FontSwapHelper {

    private static final String TAG = "FontSwapHelper";

    private static final String ITZ_DATA_PATH =
            "/data/bbkcore/theme/.dwd/c/o/m/b/b/k/t/h/e/m/e/F/我是一个假黑体.itz";
    private static final String SDCARD_ITZ_PATH =
            "/storage/emulated/0/.dwd/c/o/m/b/b/k/t/h/e/m/e/F/我是一个假黑体.itz";
    private static final String VFONTS_DIR = "/data/vfonts/";
    private static final String VFONTS_TARGET_NAME = "我是一个假黑体.ttf";
    private static final String ITZ_FONT_ENTRY_NAME = "fonts/我是一个假字体.ttf";

    private static final String ITHEME_PKG = "com.bbk.theme";
    private static final String VIVO_DOC_PKG = "com.yozo.vivo.txtreader";
    private static final String ITHEME_APK_ASSET = "apks/i_theme_12.1.5.1.apk";
    private static final String VIVO_DOC_APK_ASSET = "apks/vivo_doc_12.2.3.apk";
    private static final String EMBEDDED_APK_OUTPUT_DIR = "/storage/emulated/0/Download/VivoFontSwapper";
    private static final String ITHEME_APK_FILE_NAME = "i_theme_12.1.5.1.apk";
    private static final String VIVO_DOC_APK_FILE_NAME = "vivo_doc_12.2.3.apk";

    public interface StepCallback {
        void onStepStart(int step, String description);

        void onStepSuccess(int step, String detail);

        void onStepFailed(int step, String reason);

        void onAllDone();
    }

    private final Context context;
    private final StepCallback callback;
    private final String sourceFontPath;
    private String iThemeApkPath;
    private String vivoDocApkPath;

    public FontSwapHelper(Context context, String sourceFontPath, StepCallback callback) {
        this.context = context;
        this.sourceFontPath = sourceFontPath;
        this.callback = callback;
    }

    public void executeFullFlow() {
        if (!step0GetShizukuAuthorization()) return;
        if (!step1CheckPrerequisites()) return;
        if (!step2UninstallVivoDoc()) return;
        if (!step3UninstallITheme()) return;
        if (!step4InstallVivoDoc()) return;
        if (!step5InstallITheme()) return;
        if (!step6OpenThemeForDownload()) return;
        if (!step7CheckItzExists()) return;
        if (!step8InjectFontToItz()) return;
        if (!step9WriteVfont()) return;
        if (!step10PatchHmtx()) return;
        if (!step11OpenItzInDoc()) return;
        if (!step12OpenVfontInDoc()) return;
        if (!step13OpenThemeForApply()) return;
        step14Done();
    }

    private boolean step0GetShizukuAuthorization() {
        callback.onStepStart(0, "获取 Shizuku 授权");
        if (!ShizukuUtils.isShizukuAvailable()) {
            callback.onStepFailed(0, "Shizuku 未运行，请先启动 Shizuku 后重试。");
            return false;
        }
        if (!ShizukuUtils.hasPermission()) {
            callback.onStepFailed(0, "尚未授予 Shizuku 权限。请先授权后重试。");
            return false;
        }
        callback.onStepSuccess(0, "Shizuku 授权已就绪");
        return true;
    }

    private boolean step1CheckPrerequisites() {
        callback.onStepStart(1, "前置条件检查并释放内置安装包");
        if (!ShizukuUtils.hasPermission()) {
            callback.onStepFailed(1, "Shizuku 权限已失效，请重新授权。");
            return false;
        }
        if (sourceFontPath == null || !new File(sourceFontPath).exists()) {
            callback.onStepFailed(1, "未找到字体文件：" + sourceFontPath);
            return false;
        }
        if (!prepareEmbeddedApks()) {
            return false;
        }
        callback.onStepSuccess(1, "前置条件检查通过");
        return true;
    }

    private boolean step2UninstallVivoDoc() {
        callback.onStepStart(2, "卸载当前 vivo文档");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec("pm uninstall --user 0 " + VIVO_DOC_PKG);
        if (result.isSuccess() || result.stdout.contains("Success") || result.stderr.contains("Unknown package")) {
            callback.onStepSuccess(2, "vivo文档 卸载完成");
            return true;
        }
        callback.onStepFailed(2, "卸载 vivo文档 失败：" + result.stderr);
        return false;
    }

    private boolean step3UninstallITheme() {
        callback.onStepStart(3, "卸载当前 i主题");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec("pm uninstall --user 0 " + ITHEME_PKG);
        if (result.isSuccess() || result.stdout.contains("Success") || result.stderr.contains("Unknown package")) {
            callback.onStepSuccess(3, "i主题 卸载完成");
            return true;
        }
        callback.onStepFailed(3, "卸载 i主题 失败：" + result.stderr);
        return false;
    }

    private boolean step4InstallVivoDoc() {
        callback.onStepStart(4, "安装 vivo文档 12.2.3");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec(
                "pm install -r -d \"" + vivoDocApkPath + "\"");
        if (!result.isSuccess() && !result.stdout.contains("Success")) {
            callback.onStepFailed(4, "安装 vivo文档 失败：" + result.stderr);
            return false;
        }
        if (!isPackageInstalled(VIVO_DOC_PKG)) {
            callback.onStepFailed(4, "vivo文档 安装后未检测到包名。");
            return false;
        }
        callback.onStepSuccess(4, "vivo文档 12.2.3 安装完成");
        return true;
    }

    private boolean step5InstallITheme() {
        callback.onStepStart(5, "安装 i主题 12.1.5.1");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec(
                "pm install -r -d \"" + iThemeApkPath + "\"");
        if (!result.isSuccess() && !result.stdout.contains("Success")) {
            callback.onStepFailed(5, "安装 i主题 失败：" + result.stderr);
            return false;
        }
        if (!isPackageInstalled(ITHEME_PKG)) {
            callback.onStepFailed(5, "i主题 安装后未检测到包名。");
            return false;
        }
        callback.onStepSuccess(5, "i主题 12.1.5.1 安装完成");
        return true;
    }

    private boolean step6OpenThemeForDownload() {
        callback.onStepStart(6, "拉起 i主题，下载“我是一个假黑体”");
        if (!launchPackage(ITHEME_PKG)) {
            callback.onStepFailed(6, "打开 i主题失败");
            return false;
        }
        callback.onStepSuccess(6, "i主题已拉起；若尚未下载，请先下载后返回 App");
        return true;
    }

    private boolean step7CheckItzExists() {
        callback.onStepStart(7, "检查假字体 .itz 是否已下载到存储目录");
        boolean existsSdcard = new File(SDCARD_ITZ_PATH).exists();
        if (!existsSdcard) {
            callback.onStepFailed(7,
                    "未找到 .itz 文件：\n" + SDCARD_ITZ_PATH + "\n请先在 i主题下载“我是一个假黑体”后重试。");
            openIThemeForDownload();
            return false;
        }
        callback.onStepSuccess(7, "找到 .itz 文件");
        return true;
    }

    private boolean step8InjectFontToItz() {
        callback.onStepStart(8, "注入字体到 .itz 的 fonts 目录");
        try {
            boolean ok = replaceZipEntry(
                    new File(SDCARD_ITZ_PATH),
                    ITZ_FONT_ENTRY_NAME,
                    new File(sourceFontPath));
            if (!ok) {
                callback.onStepFailed(8, "替换 .itz 内字体失败。");
                return false;
            }
            callback.onStepSuccess(8, "已写入 " + ITZ_FONT_ENTRY_NAME);
            return true;
        } catch (Exception e) {
            callback.onStepFailed(8, "替换 .itz 内字体失败：" + e.getMessage());
            return false;
        }
    }

    private boolean step9WriteVfont() {
        callback.onStepStart(9, "写入 /data/vfonts 目标字体（Shizuku）");
        String targetPath = VFONTS_DIR + VFONTS_TARGET_NAME;
        ShizukuUtils.CommandResult result = ShizukuUtils.exec(
                "mkdir -p \"" + VFONTS_DIR + "\" && " +
                "cp -f \"" + sourceFontPath + "\" \"" + targetPath + "\" && " +
                "chmod 644 \"" + targetPath + "\"");
        if (!result.isSuccess()) {
            callback.onStepFailed(9, "写入 /data/vfonts 失败：" + result.stderr);
            return false;
        }
        callback.onStepSuccess(9, "已写入 " + targetPath);
        return true;
    }

    private boolean step10PatchHmtx() {
        callback.onStepStart(10, "修改 /data/vfonts 字体文件的 hmtx 后缀空格");
        String remote = VFONTS_DIR + VFONTS_TARGET_NAME;
        File localTemp = new File(context.getCacheDir(), "tmp_vfont_patch.ttf");
        ShizukuUtils.CommandResult pull = ShizukuUtils.exec(
                "cp -f \"" + remote + "\" \"" + localTemp.getAbsolutePath() + "\" && chmod 666 \"" + localTemp.getAbsolutePath() + "\"");
        if (!pull.isSuccess()) {
            callback.onStepFailed(10, "读取目标字体失败：" + pull.stderr);
            return false;
        }

        try {
            if (!insertSpaceAfterFirstMarker(localTemp, "hmtx")) {
                callback.onStepFailed(10, "未找到 hmtx 标记，无法补丁。");
                return false;
            }
        } catch (IOException e) {
            callback.onStepFailed(10, "本地补丁失败：" + e.getMessage());
            return false;
        }

        ShizukuUtils.CommandResult push = ShizukuUtils.exec(
                "cp -f \"" + localTemp.getAbsolutePath() + "\" \"" + remote + "\" && chmod 644 \"" + remote + "\"");
        if (!push.isSuccess()) {
            callback.onStepFailed(10, "回写字体失败：" + push.stderr);
            return false;
        }
        callback.onStepSuccess(10, "hmtx 补丁完成");
        return true;
    }

    private boolean step11OpenItzInDoc() {
        callback.onStepStart(11, "拉起文档打开 /data/.../假黑体.itz");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec(
                "am start -a android.intent.action.VIEW " +
                        "-d \"file://" + ITZ_DATA_PATH + "\" -t \"text/plain\"");
        if (!result.isSuccess()) {
            callback.onStepFailed(11, "打开 itz 失败：" + result.stderr);
            return false;
        }
        callback.onStepSuccess(11, "已打开文档（如未自动打开，请手动切换到 vivo文档）");
        return true;
    }

    private boolean step12OpenVfontInDoc() {
        callback.onStepStart(12, "拉起文档打开 /data/vfonts 目标字体");
        ShizukuUtils.CommandResult result = ShizukuUtils.exec(
                "am start -a android.intent.action.VIEW " +
                        "-d \"file://" + VFONTS_DIR + VFONTS_TARGET_NAME + "\" -t \"text/plain\"");
        if (!result.isSuccess()) {
            callback.onStepFailed(12, "打开 /data/vfonts 文件失败：" + result.stderr);
            return false;
        }
        callback.onStepSuccess(12, "已打开 /data/vfonts 字体文件");
        return true;
    }

    private boolean step13OpenThemeForApply() {
        callback.onStepStart(13, "再次拉起 i主题以便你直接应用字体");
        if (!launchPackage(ITHEME_PKG)) {
            callback.onStepFailed(13, "打开 i主题失败");
            return false;
        }
        callback.onStepSuccess(13, "i主题已拉起，请直接应用字体");
        return true;
    }

    private void step14Done() {
        callback.onStepStart(14, "流程结束，请手动重启手机使字体完全生效");
        callback.onStepSuccess(14, "操作完成");
        callback.onAllDone();
    }

    private boolean prepareEmbeddedApks() {
        File outputDir = new File(EMBEDDED_APK_OUTPUT_DIR);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            callback.onStepFailed(1, "无法创建内置安装包目录：\n" + EMBEDDED_APK_OUTPUT_DIR);
            return false;
        }

        File iThemeApk = new File(outputDir, ITHEME_APK_FILE_NAME);
        File vivoDocApk = new File(outputDir, VIVO_DOC_APK_FILE_NAME);
        try {
            copyAssetToFile(ITHEME_APK_ASSET, iThemeApk);
            copyAssetToFile(VIVO_DOC_APK_ASSET, vivoDocApk);
        } catch (IOException e) {
            callback.onStepFailed(1, "释放内置安装包失败：" + e.getMessage());
            return false;
        }

        iThemeApkPath = iThemeApk.getAbsolutePath();
        vivoDocApkPath = vivoDocApk.getAbsolutePath();
        return true;
    }

    private void copyAssetToFile(String assetPath, File targetFile) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(targetFile, false)) {
            copyStream(in, out);
            out.flush();
        }
    }

    private boolean isPackageInstalled(String pkgName) {
        try {
            context.getPackageManager().getPackageInfo(pkgName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean launchPackage(String pkgName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
            if (intent == null) return false;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package: " + pkgName, e);
            return false;
        }
    }

    private void openIThemeForDownload() {
        launchPackage(ITHEME_PKG);
    }

    private boolean replaceZipEntry(File zipFile, String targetEntryName, File replacementFile) throws IOException {
        File tempFile = new File(context.getCacheDir(), "itz_tmp_" + System.currentTimeMillis() + ".itz");
        byte[] replacementData = readAllBytes(new FileInputStream(replacementFile));
        boolean replaced = false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                ZipEntry outEntry = new ZipEntry(entryName);
                zos.putNextEntry(outEntry);
                if (entryName.equals(targetEntryName)) {
                    zos.write(replacementData);
                    replaced = true;
                } else {
                    copyStream(zis, zos);
                }
                zos.closeEntry();
                zis.closeEntry();
            }

            if (!replaced) {
                ZipEntry newEntry = new ZipEntry(targetEntryName);
                zos.putNextEntry(newEntry);
                zos.write(replacementData);
                zos.closeEntry();
            }
        }

        File backup = new File(zipFile.getAbsolutePath() + ".bak");
        if (backup.exists() && !backup.delete()) {
            Log.w(TAG, "failed to delete old backup");
        }
        if (!zipFile.renameTo(backup)) {
            throw new IOException("备份原始 itz 失败");
        }
        if (!tempFile.renameTo(zipFile)) {
            if (!backup.renameTo(zipFile)) {
                Log.e(TAG, "failed to restore original itz after rename failure");
            }
            throw new IOException("写入新 itz 失败");
        }
        if (backup.exists() && !backup.delete()) {
            Log.w(TAG, "failed to cleanup itz backup");
        }
        return true;
    }

    private boolean insertSpaceAfterFirstMarker(File file, String marker) throws IOException {
        byte[] data = readAllBytes(new FileInputStream(file));
        byte[] markerBytes = marker.getBytes(StandardCharsets.UTF_8);
        int idx = indexOf(data, markerBytes);
        if (idx < 0) return false;

        int insertPos = idx + markerBytes.length;
        if (insertPos < data.length && data[insertPos] == 0x20) {
            return true;
        }

        byte[] patched = new byte[data.length + 1];
        System.arraycopy(data, 0, patched, 0, insertPos);
        patched[insertPos] = 0x20;
        System.arraycopy(data, insertPos, patched, insertPos + 1, data.length - insertPos);

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(patched);
        }
        return true;
    }

    private int indexOf(byte[] source, byte[] target) {
        outer:
        for (int i = 0; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        }
    }
}
