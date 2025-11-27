# Locust TV v0.1.0-alpha

**发布日期**: 2024-11-26  
**版本类型**: Alpha 预览版

---

## 🎉 首个 Alpha 版本发布

**Locust TV** 是一个专为 Android TV 设计的开源电视直播应用，整合了 my-tv 播放器的优秀体验与 iptv-org/iptv 全球公共直播源的自动更新机制。

---

## ✨ 核心功能

- **自动更新机制**: 每日通过 Python 脚本从 iptv-org/iptv 拉取最新直播源
- **jsDelivr 加速**: 使用 CDN 镜像确保中国大陆稳定访问
- **有效性验证**: 自动验证并剔除失效链接，最多保留 500 个有效频道
- **智能回退**: 外部频道列表不可用时自动回退到内置频道
- **Termux 支持**: 提供一键配置脚本，支持 Android 设备上的定时任务

---

## 📦 安装说明

### 方法 1: 直接安装 APK

1. 下载 `locust-tv-alpha.apk`
2. 在 Android TV 或电视盒子上安装
3. 启动应用即可观看内置频道

### 方法 2: 配置自动更新（推荐）

在 Android 设备上安装 [Termux](https://f-droid.org/packages/com.termux/)，然后执行：

```bash
bash <(curl -sL https://raw.githubusercontent.com/yanzhao77/locust-tv/main/scripts/setup_cron.sh)
```

该脚本会自动配置定时任务，每天凌晨 3:00 更新频道列表。

---

## 🔧 技术亮点

### jsDelivr CDN 加速

| 特性 | GitHub Raw | jsDelivr CDN |
| :--- | :--- | :--- |
| **访问速度** | 较慢 | **极快** |
| **稳定性** | 一般 | **非常高** |
| **中国大陆可用性** | 不稳定 | **稳定可用** |

### 三层容错设计

1. **外部 M3U 文件** → 最新的自动更新频道
2. **内置频道列表** → 默认回退方案
3. **空列表处理** → 应用仍可正常启动

---

## ⚠️ 免责声明

> **重要**: 本应用不托管、不存储任何视频内容。所有直播源均来自互联网公开资源，版权归原内容提供方所有。
> 
> 本项目仅供个人学习与研究使用，请勿用于任何商业用途。开发者不承担任何因使用本项目而导致的法律责任。

---

## 📚 数据源

- **直播源**: [iptv-org/iptv](https://github.com/iptv-org/iptv) (Unlicense)
- **播放器基础**: [lizongying/my-tv](https://github.com/lizongying/my-tv) (MIT License)

---

## 🐛 已知问题

- ChannelLoader 尚未集成到 MainActivity（需手动修改代码）
- 部分直播源可能因地区限制无法播放
- 首次启动可能需要较长时间加载频道列表

---

## 🔮 后续计划

- [ ] 集成 ChannelLoader 到主应用流程
- [ ] 添加频道缓存机制
- [ ] 支持用户自定义 M3U 源
- [ ] 集成 EPG 电子节目指南
- [ ] 优化 UI 显示当前数据源状态

---

## 📞 反馈与支持

- **问题反馈**: [GitHub Issues](https://github.com/yanzhao77/locust-tv/issues)
- **项目主页**: [GitHub Repository](https://github.com/yanzhao77/locust-tv)

---

**感谢使用 Locust TV！**
