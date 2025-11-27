package com.yanzhao77.locusttv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 频道缓存管理器
 * 
 * 功能:
 * - 缓存已加载的频道列表到本地
 * - 记录数据源类型（外部 M3U / 内置频道）
 * - 记录最后更新时间
 * - 提供缓存有效性检查
 */
object ChannelCache {
    private const val TAG = "ChannelCache"
    
    // 缓存文件名
    private const val CACHE_FILE_NAME = "channels_cache.txt"
    private const val METADATA_FILE_NAME = "channels_metadata.txt"
    
    // 缓存有效期（24小时）
    private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L
    
    /**
     * 数据源类型
     */
    enum class SourceType {
        EXTERNAL_M3U,  // 外部 M3U 文件
        BUILTIN        // 内置频道列表
    }
    
    /**
     * 缓存元数据
     */
    data class CacheMetadata(
        val sourceType: SourceType,
        val timestamp: Long,
        val channelCount: Int,
        val sourcePath: String = ""
    )
    
    /**
     * 保存频道列表到缓存
     */
    suspend fun saveChannels(
        context: Context,
        channels: List<TV>,
        sourceType: SourceType,
        sourcePath: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val cacheFile = File(cacheDir, CACHE_FILE_NAME)
            val metadataFile = File(cacheDir, METADATA_FILE_NAME)
            
            // 保存频道列表
            cacheFile.writeText(channels.joinToString("\n") { tv ->
                "${tv.title}|${tv.videoUrl.firstOrNull() ?: ""}|${tv.channel}"
            })
            
            // 保存元数据
            val metadata = CacheMetadata(
                sourceType = sourceType,
                timestamp = System.currentTimeMillis(),
                channelCount = channels.size,
                sourcePath = sourcePath
            )
            metadataFile.writeText(
                "${metadata.sourceType.name}|${metadata.timestamp}|${metadata.channelCount}|${metadata.sourcePath}"
            )
            
            Log.i(TAG, "频道缓存已保存: ${channels.size} 个频道, 来源: $sourceType")
        } catch (e: IOException) {
            Log.e(TAG, "保存频道缓存失败", e)
        }
    }
    
    /**
     * 从缓存加载频道列表
     */
    suspend fun loadChannels(context: Context): List<TV>? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val cacheFile = File(cacheDir, CACHE_FILE_NAME)
            
            if (!cacheFile.exists()) {
                Log.w(TAG, "缓存文件不存在")
                return@withContext null
            }
            
            // 检查缓存是否有效
            val metadata = loadMetadata(context)
            if (metadata == null || !isCacheValid(metadata)) {
                Log.w(TAG, "缓存已过期或无效")
                return@withContext null
            }
            
            // 读取频道列表
            val channels = cacheFile.readLines().mapIndexed { index, line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    TV(
                        id = index,
                        title = parts[0],
                        vid = parts[0].replace(" ", ""),
                        videoUrl = listOf(parts[1]),
                        channel = parts[2],
                        logo = "",
                        pid = "",
                        sid = "",
                        programId = "",
                        needToken = false,
                        mustToken = false
                    )
                } else {
                    null
                }
            }.filterNotNull()
            
            Log.i(TAG, "从缓存加载频道: ${channels.size} 个频道")
            channels
        } catch (e: IOException) {
            Log.e(TAG, "加载频道缓存失败", e)
            null
        }
    }
    
    /**
     * 加载缓存元数据
     */
    suspend fun loadMetadata(context: Context): CacheMetadata? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val metadataFile = File(cacheDir, METADATA_FILE_NAME)
            
            if (!metadataFile.exists()) {
                return@withContext null
            }
            
            val line = metadataFile.readText().trim()
            val parts = line.split("|")
            
            if (parts.size >= 3) {
                CacheMetadata(
                    sourceType = SourceType.valueOf(parts[0]),
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    channelCount = parts[2].toIntOrNull() ?: 0,
                    sourcePath = parts.getOrNull(3) ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载缓存元数据失败", e)
            null
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(metadata: CacheMetadata): Boolean {
        val now = System.currentTimeMillis()
        val age = now - metadata.timestamp
        return age < CACHE_VALIDITY_MS
    }
    
    /**
     * 清除缓存
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            File(cacheDir, CACHE_FILE_NAME).delete()
            File(cacheDir, METADATA_FILE_NAME).delete()
            Log.i(TAG, "缓存已清除")
        } catch (e: IOException) {
            Log.e(TAG, "清除缓存失败", e)
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(context: Context): String = withContext(Dispatchers.IO) {
        val metadata = loadMetadata(context)
        if (metadata == null) {
            "无缓存"
        } else {
            val age = System.currentTimeMillis() - metadata.timestamp
            val ageHours = age / (60 * 60 * 1000)
            val sourceTypeText = when (metadata.sourceType) {
                SourceType.EXTERNAL_M3U -> "外部 M3U"
                SourceType.BUILTIN -> "内置频道"
            }
            "来源: $sourceTypeText\n频道数: ${metadata.channelCount}\n更新时间: ${ageHours} 小时前"
        }
    }
}
