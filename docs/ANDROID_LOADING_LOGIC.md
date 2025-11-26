# locust-tv Android 加载逻辑说明

## 概述

locust-tv 实现了智能频道加载机制,优先使用外部自动更新的 M3U 文件,失败时自动回退到内置频道列表,确保用户始终有内容可看。

---

## 加载流程

```
┌─────────────────────────────────────────────────────────┐
│                    App 启动                              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  ChannelLoader.loadChannels()                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  尝试加载外部 M3U 文件                                   │
│  路径: /data/local/tmp/locust-tv/channels.m3u          │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
    ┌─────────┐            ┌─────────┐
    │ 成功    │            │ 失败    │
    └────┬────┘            └────┬────┘
         │                      │
         │                      ▼
         │            ┌──────────────────────┐
         │            │ 回退到内置频道列表    │
         │            │ (res/raw/channels.json)│
         │            └──────────┬───────────┘
         │                       │
         └───────────┬───────────┘
                     │
                     ▼
         ┌─────────────────────┐
         │   返回频道列表       │
         │   开始播放          │
         └─────────────────────┘
```

---

## 核心代码实现

### 1. ChannelLoader.kt (新增)

**位置**: `app/src/main/java/com/lizongying/mytv/ChannelLoader.kt`

**主要功能**:
- 优先加载外部 M3U 文件
- 解析 M3U 格式 (支持 `#EXTINF` 标签)
- 失败时回退到内置 JSON 频道列表
- 提供文件状态检查接口

**关键方法**:

```kotlin
/**
 * 加载频道列表 (主入口)
 */
suspend fun loadChannels(context: Context): List<TV> {
    return withContext(Dispatchers.IO) {
        try {
            // 1. 尝试加载外部 M3U 文件
            val externalChannels = loadExternalM3U()
            if (externalChannels.isNotEmpty()) {
                Log.i(TAG, "成功加载外部 M3U 文件: ${externalChannels.size} 个频道")
                return@withContext externalChannels
            }
            
            Log.w(TAG, "外部 M3U 文件不可用,回退到内置频道列表")
        } catch (e: Exception) {
            Log.e(TAG, "加载外部 M3U 文件失败: ${e.message}", e)
        }
        
        // 2. 回退到内置频道列表
        try {
            val builtinChannels = loadBuiltinChannels(context)
            Log.i(TAG, "成功加载内置频道列表: ${builtinChannels.size} 个频道")
            return@withContext builtinChannels
        } catch (e: Exception) {
            Log.e(TAG, "加载内置频道列表失败: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}
```

---

### 2. M3U 解析逻辑

**支持的 M3U 格式**:

```m3u
#EXTM3U
#EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="央视",CCTV1 综合
http://example.com/stream.m3u8
#EXTINF:-1 tvg-logo="https://example.com/logo2.png" group-title="卫视",湖南卫视
http://example.com/stream2.m3u8
```

**解析逻辑**:

```kotlin
private fun parseM3U(content: String): List<TV> {
    val channels = mutableListOf<TV>()
    val lines = content.trim().split('\n')
    
    var currentId = 0
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i].trim()
        
        // 匹配 #EXTINF 行
        if (line.startsWith("#EXTINF:")) {
            // 提取频道名称
            val nameMatch = Regex(",(.+)$").find(line)
            val channelName = nameMatch?.groupValues?.get(1)?.trim() ?: "Unknown"
            
            // 提取 tvg-logo
            val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(line)
            val logoUrl = logoMatch?.groupValues?.get(1) ?: ""
            
            // 提取 group-title
            val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
            val groupTitle = groupMatch?.groupValues?.get(1) ?: "其他"
            
            // 下一行是流媒体 URL
            i++
            if (i < lines.size) {
                val streamUrl = lines[i].trim()
                if (streamUrl.isNotEmpty() && !streamUrl.startsWith('#')) {
                    val tv = TV(
                        id = currentId++,
                        title = channelName,
                        vid = channelName.replace(" ", ""),
                        videoUrl = listOf(streamUrl),
                        channel = groupTitle,
                        logo = logoUrl,
                        pid = "",
                        sid = "",
                        programId = "",
                        needToken = false,
                        mustToken = false
                    )
                    channels.add(tv)
                }
            }
        }
        i++
    }
    
    return channels
}
```

---

### 3. 集成到现有代码

**修改位置**: `app/src/main/java/com/lizongying/mytv/MainActivity.kt` (或频道初始化位置)

**原有代码** (假设):
```kotlin
// 原有加载逻辑
val channels = ChannelUtils.getLocalChannel(context)
```

**修改为**:
```kotlin
// 使用新的 ChannelLoader
val channels = ChannelLoader.loadChannels(context)
```

---

## 容错机制

### 1. 文件存在性检查
```kotlin
if (!file.exists() || !file.canRead()) {
    Log.w(TAG, "外部 M3U 文件不存在或不可读")
    return emptyList()
}
```

### 2. 文件大小检查
```kotlin
if (file.length() < 10) {
    Log.w(TAG, "外部 M3U 文件过小,可能已损坏")
    return emptyList()
}
```

### 3. 异常捕获
```kotlin
try {
    val externalChannels = loadExternalM3U()
    // ...
} catch (e: Exception) {
    Log.e(TAG, "加载外部 M3U 文件失败: ${e.message}", e)
    // 自动回退到内置频道
}
```

---

## 权限要求

### AndroidManifest.xml

确保已添加以下权限:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
```

**注意**: `/data/local/tmp/` 目录在 Android 设备上通常无需额外权限即可读取 (ADB shell 可访问)。

---

## 测试方法

### 1. 测试外部 M3U 加载

通过 ADB 推送测试文件:

```bash
# 创建测试 M3U 文件
cat > test_channels.m3u << 'EOF'
#EXTM3U
#EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="测试",测试频道1
http://example.com/stream1.m3u8
#EXTINF:-1 tvg-logo="https://example.com/logo2.png" group-title="测试",测试频道2
http://example.com/stream2.m3u8
EOF

# 推送到设备
adb shell mkdir -p /data/local/tmp/locust-tv
adb push test_channels.m3u /data/local/tmp/locust-tv/channels.m3u

# 验证文件
adb shell cat /data/local/tmp/locust-tv/channels.m3u
```

### 2. 测试回退机制

删除外部文件,验证是否回退到内置频道:

```bash
adb shell rm /data/local/tmp/locust-tv/channels.m3u
```

### 3. 查看日志

```bash
adb logcat | grep ChannelLoader
```

预期输出:
```
I/ChannelLoader: 成功加载外部 M3U 文件: 500 个频道
```

或 (回退时):
```
W/ChannelLoader: 外部 M3U 文件不可用,回退到内置频道列表
I/ChannelLoader: 成功加载内置频道列表: 100 个频道
```

---

## 优势

1. **零侵入**: 不修改原有 `ChannelUtils.kt`,仅新增 `ChannelLoader.kt`
2. **容错性强**: 多重检查 (存在性、大小、解析异常)
3. **用户友好**: 即使自动更新失败,仍能观看内置频道
4. **灵活扩展**: 未来可支持更多格式 (如 JSON、XML)

---

## 注意事项

1. **文件路径**: `/data/local/tmp/` 在部分设备上可能需要 root 权限,建议在 README 中说明
2. **性能优化**: M3U 解析在 IO 线程中执行,避免阻塞主线程
3. **编码问题**: 使用 `Charsets.UTF_8` 确保中文频道名正确显示
4. **日志记录**: 所有关键步骤均有日志输出,便于调试

---

## 相关文件

- `ChannelLoader.kt`: 频道加载器 (新增)
- `ChannelUtils.kt`: 原有频道工具类 (保留)
- `MainActivity.kt`: 主活动 (需修改频道加载调用)
- `res/raw/channels.json`: 内置频道列表 (回退数据源)

---

## 后续优化

1. 支持多个 M3U 文件源 (优先级队列)
2. 添加频道缓存机制 (减少重复解析)
3. 支持 M3U8 嵌套播放列表
4. 提供用户界面显示当前数据源 (外部/内置)
