# VivoFontSwapper

自动化执行 vivo 手机自定义字体替换流程（无需 ATools）的 Android App。

## 功能

将 Notion 教程中繁琐的 15 步手动操作，自动化为一键执行：

| 步骤 | 教程手动操作 | App 自动化 |
|------|-------------|-----------|
| 1 | 手动安装降级 vivo文档、i主题 | ✅ 启动时检测版本 |
| 2 | 打开 i主题搜索"我是一个假黑体"下载 | ✅ 检测 .itz 是否存在 |
| 3 | 用创建快捷方式打开 vivo文档修改 fonts/ | ✅ Root 直接修改文件 |
| 4 | 删除 i主题字体 | ✅ Root 清除缓存 |
| 5 | 返回 vivo文档点保存 | ✅ Root 触发密钥生成 |
| 6 | 再次下载 i主题字体 | ✅ am start 触发 + 轮询等待 |
| 7 | MT管理器复制字体进 .itz/fonts/ | ✅ Root 直接复制重命名 |
| 8 | 打开 vivo文档修改 hmtx 后加空格保存 | ✅ Root 精确字节修改 |
| 9 | i主题应用字体 | ✅ am start 触发应用 |
| 10 | 重启手机 | ✅ Root reboot |

## 前置条件

1. **Root 权限** — 必须（无法绕过）
2. **vivo文档 12.2.3** — 在系统应用管理中降级安装
3. **i主题 12.1.5.1** — 安装旧版本（此版本存在允许替换字体的 bug）
4. **目标字体文件** — 任意 `.ttf` 或 `.otf` 字体

## 使用方法

1. 在手机上安装本 APK
2. 打开 App，点击 **"选择字体文件"**，选择你的 `.ttf` 文件
3. 点击 **"开始执行"**，等待所有步骤完成
4. 手机自动重启，字体生效 ✅

## 常见问题

**Q: i主题应用字体时闪退？**  
A: 先在系统设置 → 主题 → 恢复默认主题，然后重新执行本 App。

**Q: 步骤 5（重新下载字体）超时？**  
A: 手动在 i主题中搜索"我是一个假黑体"并下载，完成后回来重试。

**Q: 换不同的字体怎么做？**  
A: 选择新的字体文件，直接重新执行即可。

## 构建

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/VivoFontSwapper.git
cd VivoFontSwapper

# 用 Android Studio 打开，或命令行构建
./gradlew assembleDebug

# APK 路径
app/build/outputs/apk/debug/app-debug.apk
```

**最低系统要求**: Android 10 (API 29)，vivo OriginOS / FuntouchOS

## 技术原理

- 通过 `su` 进程执行 Root 命令操作 `/data/bbkcore/theme/` 和 `/data/vfonts/` 目录
- 使用 Python（Android 上通过 Termux 或系统内置）进行精确的字节级文件修改
- 通过 `am start` / `am broadcast` 触发系统应用的特定 Activity 和广播

## 免责声明

本工具仅用于个人学习研究，利用 i主题 12.1.5.1 版本的历史漏洞实现字体替换。使用本工具造成的任何问题由用户自行承担。
