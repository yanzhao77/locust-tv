> [!WARNING]
> 本项目不托管、不存储任何视频内容，所有直播源均来自互联网公开资源，仅供个人学习与研究使用。请勿用于任何商业用途，否则后果自负。

# Locust TV

**Locust TV** 是一个专为 Android TV 设计的开源电视直播应用，旨在解决现有直播源频繁失效的问题。项目深度整合了 [my-tv](https://github.com/lizongying/my-tv) 的优秀播放器体验与 [iptv-org/iptv](https://github.com/iptv-org/iptv) 的全球公共直播源，通过自动化脚本每日更新有效频道，为您提供稳定、流畅的观看体验。

![App Screenshot](screenshots/screenshot.png)

## 核心特性

- **自动更新**：每日通过轻量级 Python 脚本从 `iptv-org/iptv` 仓库拉取最新直播源。
- **有效性验证**：脚本自动验证每个直播源的有效性，剔除失效链接，确保频道列表高质量。
- **jsDelivr 加速**：默认使用 [jsDelivr CDN](https://www.jsdelivr.com/) 镜像加速 M3U 文件下载，确保在中国大陆等地区稳定、快速地访问。
- **智能回退**：当自动更新失败或外部频道列表不可用时，应用会自动回退到内置的默认频道，保证始终有内容可看。
- **Termux 兼容**：更新脚本完美兼容 [Termux](https://termux.dev/en/) 环境，可在 Android 设备上直接运行并设置定时任务。
- **开源免费**：项目遵循 MIT 许可证，完全免费，并鼓励社区贡献。

---

## 安装与配置

### 1. 安装应用

1.  前往 [GitHub Releases](https://github.com/yanzhao77/locust-tv/releases) 页面。
2.  下载最新的 `locust-tv-alpha.apk` 文件。
3.  在您的 Android TV 或电视盒子上安装该 APK。

### 2. 配置自动更新 (Termux)

要在您的 Android 设备上启用自动更新，请按照以下步骤操作：

1.  **安装 Termux**：
    从 [F-Droid](https://f-droid.org/packages/com.termux/) 下载并安装 Termux 和 Termux:API。

2.  **运行配置脚本**：
    打开 Termux，执行以下命令一键配置定时更新任务。

    ```bash
    bash <(curl -sL https://raw.githubusercontent.com/yanzhao77/locust-tv/main/scripts/setup_cron.sh)
    ```

    该脚本会自动完成以下工作：
    - 安装 Python 和 cronie 依赖。
    - 下载 `update_iptv.py` 脚本。
    - 设置一个 cron 定时任务，在每天凌晨 3:00 自动执行更新。
    - 立即执行一次首次更新。

3.  **验证配置**：
    - 查看定时任务：`crontab -l`
    - 查看服务状态：`sv status crond`
    - 查看更新日志：`cat ~/.locust-tv/logs/update.log`

配置完成后，**Locust TV** 应用将在每次启动时自动加载由该脚本生成的最新频道列表。

---

## 数据源与合规性

- **直播源**：所有直播源均来自 [iptv-org/iptv](https://github.com/iptv-org/iptv) 项目，该项目使用 [Unlicense](https://unlicense.org/) 许可证，允许自由使用。
- **许可证**：本项目沿用 `my-tv` 的 [MIT License](LICENSE)，您可以自由修改、分发和使用。
- **免责声明**：本项目是一个学习与研究性质的工具，旨在探索 Android 开发和自动化脚本技术。所有内容均来自公开网络，版权归原内容提供方所有。开发者不承担任何因使用本项目而导致的法律责任。

---

## jsDelivr 加速优势

| 特性 | GitHub Raw | jsDelivr CDN |
| :--- | :--- | :--- |
| **访问速度** | 较慢，尤其在中国大陆 | **极快**，全球节点优化 |
| **稳定性** | 一般，可能受网络波动影响 | **非常高**，多节点冗余 |
| **中国大陆可用性** | 经常被屏蔽或连接不稳定 | **稳定可用**，有 ICP 备案 |
| **缓存** | 无 | 智能缓存，加速重复访问 |

使用 jsDelivr 可以确保无论您身在何处，都能快速、可靠地获取最新的频道列表，这是保障 **Locust TV** 稳定运行的关键。

---

## 贡献

欢迎您为本项目贡献代码、提出建议或报告问题。请通过 [GitHub Issues](https://github.com/yanzhao77/locust-tv/issues) 与我们联系。

## 致谢

- [lizongying/my-tv](https://github.com/lizongying/my-tv)
- [iptv-org/iptv](https://github.com/iptv-org/iptv)
