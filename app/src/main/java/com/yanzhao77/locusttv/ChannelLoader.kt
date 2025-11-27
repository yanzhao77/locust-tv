package com.yanzhao77.locusttv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * locust-tv 频道加载器
 * 功能: 优先加载外部 M3U 文件,失败时回退到内置频道列表
 * 
 * 加载优先级:
 * 1. /data/local/tmp/locust-tv/channels.m3u (外部自动更新)
 * 2. app/src/main/res/raw/channels.json (内置默认频道)
 * 
 * @author locust-tv
 */
object ChannelLoader {
    private const val TAG = "ChannelLoader"
    
    // 外部 M3U 文件路径
    private const val EXTERNAL_M3U_PATH = "/data/local/tmp/locust-tv/channels.m3u"
    
    /**
     * 加载频道列表 (主入口)
     * 
     * @param context Context
     * @return List<TV> 频道列表
     */
    suspend fun loadChannels(context: Context, useCache: Boolean = true): List<TV> {
        return withContext(Dispatchers.IO) {
            // 1. 尝试从缓存加载
            if (useCache) {
                val cachedChannels = ChannelCache.loadChannels(context)
                if (cachedChannels != null && cachedChannels.isNotEmpty()) {
                    Log.i(TAG, "从缓存加载频道: ${cachedChannels.size} 个")
                    return@withContext cachedChannels
                }
            }
            
            try {
                // 2. 尝试加载外部 M3U 文件
                val externalChannels = loadExternalM3U()
                if (externalChannels.isNotEmpty()) {
                    Log.i(TAG, "成功加载外部 M3U 文件: ${externalChannels.size} 个频道")
                    // 保存到缓存
                    ChannelCache.saveChannels(context, externalChannels, ChannelCache.SourceType.EXTERNAL_M3U, EXTERNAL_M3U_PATH)
                    return@withContext externalChannels
                }
                
                Log.w(TAG, "外部 M3U 文件不可用,回退到内置频道列表")
            } catch (e: Exception) {
                Log.e(TAG, "加载外部 M3U 文件失败: ${e.message}", e)
            }
            
            // 3. 回退到内置频道列表
            try {
                val builtinChannels = loadBuiltinChannels(context)
                Log.i(TAG, "成功加载内置频道列表: ${builtinChannels.size} 个频道")
                // 保存到缓存
                ChannelCache.saveChannels(context, builtinChannels, ChannelCache.SourceType.BUILTIN)
                return@withContext builtinChannels
            } catch (e: Exception) {
                Log.e(TAG, "加载内置频道列表失败: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }
    
    /**
     * 加载外部 M3U 文件
     * 
     * @return List<TV> 频道列表
     * @throws Exception 文件不存在或解析失败
     */
    private fun loadExternalM3U(): List<TV> {
        val file = File(EXTERNAL_M3U_PATH)
        
        // 检查文件是否存在且可读
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "外部 M3U 文件不存在或不可读: $EXTERNAL_M3U_PATH")
            return emptyList()
        }
        
        // 检查文件大小 (避免空文件)
        if (file.length() < 10) {
            Log.w(TAG, "外部 M3U 文件过小,可能已损坏: ${file.length()} bytes")
            return emptyList()
        }
        
        // 读取并解析 M3U 文件
        val content = file.readText(Charsets.UTF_8)
        return parseM3U(content)
    }
    
    /**
     * 解析 M3U 文件内容
     * 
     * M3U 格式示例:
     * #EXTM3U
     * #EXTINF:-1 tvg-logo="..." group-title="央视",CCTV1 综合
     * http://example.com/stream.m3u8
     * 
     * @param content M3U 文件内容
     * @return List<TV> 频道列表
     */
    private fun parseM3U(content: String): List<TV> {
        val channels = mutableListOf<TV>()
        val lines = content.trim().split('\n')
        
        var currentId = 0
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // 匹配 #EXTINF 行
            if (line.startsWith("#EXTINF:")) {
                // 提取频道名称 (最后一个逗号后的内容)
                val nameMatch = Regex(",(.+)$").find(line)
                val channelName = nameMatch?.groupValues?.get(1)?.trim() ?: "Unknown"
                
                // 提取 tvg-logo
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(line)
                val logoUrl = logoMatch?.groupValues?.get(1) ?: ""
                
                // 提取 group-title
                val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
                val groupTitle = groupMatch?.groupValues?.get(1) ?: "其他"
                
                // 下一行应该是流媒体 URL
                i++
                if (i < lines.size) {
                    val streamUrl = lines[i].trim()
                    if (streamUrl.isNotEmpty() && !streamUrl.startsWith('#')) {
                        // 创建 TV 对象
                        val tv = TV(
                            id = currentId++,
                            title = channelName,
                            vid = channelName.replace(" ", ""),  // 简化 vid
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
        
        Log.d(TAG, "M3U 解析完成: ${channels.size} 个频道")
        return channels
    }
    
    /**
     * 加载内置频道列表 (从 res/raw/channels.json)
     * 
     * @param context Context
     * @return List<TV> 频道列表
     */
    private fun loadBuiltinChannels(context: Context): List<TV> {
        // 复用原有的 ChannelUtils 逻辑
        val jsonContent = context.resources.openRawResource(R.raw.channels)
            .bufferedReader()
            .use { it.readText() }
        
        val type = object : com.google.gson.reflect.TypeToken<List<TV>>() {}.type
        return com.google.gson.Gson().fromJson(jsonContent, type)
    }
    
    /**
     * 检查外部 M3U 文件是否存在
     * 
     * @return Boolean 是否存在
     */
    fun hasExternalM3U(): Boolean {
        val file = File(EXTERNAL_M3U_PATH)
        return file.exists() && file.canRead() && file.length() > 10
    }
    
    /**
     * 获取外部 M3U 文件信息
     * 
     * @return Map<String, Any> 文件信息 (path, size, lastModified)
     */
    fun getExternalM3UInfo(): Map<String, Any> {
        val file = File(EXTERNAL_M3U_PATH)
        return if (file.exists()) {
            mapOf(
                "path" to EXTERNAL_M3U_PATH,
                "size" to file.length(),
                "lastModified" to file.lastModified(),
                "readable" to file.canRead()
            )
        } else {
            mapOf(
                "path" to EXTERNAL_M3U_PATH,
                "exists" to false
            )
        }
    }
}
