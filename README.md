# VivoFontSwapper

用于简化 vivo 手机字体替换流程的 Android App。

## 功能

将手动教程中的关键操作收敛为一键流程：

| 步骤 | 教程手动操作 | App 自动化 |
|------|-------------|-----------|
| 1 | 获取 Shizuku 授权 | ✅ App 启动时请求 Shizuku 权限 |
| 2 | 前置条件检查 | ✅ 检查 Shizuku 授权并释放内置安装包 |
| 3 | 卸载当前 vivo文档/i主题 | ✅ Shizuku 执行 `pm uninstall` |
| 4 | 安装指定版本 | ✅ Shizuku 安装 vivo文档 12.2.3 / i主题 12.1.5.1 |
| 5 | MT 管理器替换 `.itz` 内字体 | ✅ App 直接改 `.itz`(zip) 里的 `fonts/我是一个假字体.ttf` |
| 6 | 写入 `/data/vfonts` + `hmtx` 补丁 | ✅ Shizuku 自动处理 |
| 7 | 拉起文档与 i主题 | ✅ App 自动执行 |
| 8 | 重启手机 | ✅ App 提示手动重启 |

## 前置条件

1. **Shizuku 已安装并运行** — 执行命令需要 Shizuku 授权
2. **目标字体文件** — 任意 `.ttf` 或 `.otf` 字体
3. **内置安装包** — App 已内置 vivo文档 12.2.3 与 i主题 12.1.5.1，执行时自动释放并安装

## 使用方法

1. 在手机上安装本 APK
2. 打开 App，点击 **"选择字体文件"**，选择你的 `.ttf` 文件
3. 点击 **"开始执行"**，等待流程完成
4. 根据提示在 i主题应用字体后，手动重启手机 ✅

## 常见问题

**Q: i主题应用字体时闪退？**  
A: 先在系统设置 → 主题 → 恢复默认主题，然后重新执行本 App。

**Q: 提示找不到 `.itz` 文件？**  
A: 先在 i主题中下载“我是一个假黑体”，然后重试。

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

## 免责声明

本工具仅用于个人学习研究。请自行评估风险，并对设备数据做好备份。
