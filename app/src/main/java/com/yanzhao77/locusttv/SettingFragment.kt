package com.yanzhao77.locusttv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.yanzhao77.locusttv.databinding.DialogBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast


class SettingFragment : DialogFragment() {

    private var _binding: DialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext() // It‘s safe to get context here.
        _binding = DialogBinding.inflate(inflater, container, false)
        binding.versionName.text = "当前版本: v${context.appVersionName}"
        binding.version.text = "https://github.com/lizongying/my-tv"

        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
                (activity as MainActivity).settingActive()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                (activity as MainActivity).settingActive()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, isChecked ->
                SP.bootStartup = isChecked
                (activity as MainActivity).settingActive()
            }
        }

        updateManager = UpdateManager(context, this, context.appVersionCode)
        binding.checkVersion.setOnClickListener(OnClickListenerCheckVersion(updateManager))
        
        // 显示数据源状态
        lifecycleScope.launch {
            val hasExternalM3U = ChannelLoader.hasExternalM3U()
            val m3uInfo = ChannelLoader.getExternalM3UInfo()
            
            val statusText = if (hasExternalM3U) {
                val size = (m3uInfo["size"] as Long) / 1024
                val lastModified = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(java.util.Date(m3uInfo["lastModified"] as Long))
                "外部 M3U 文件\n大小: ${size} KB\n更新: $lastModified"
            } else {
                "内置频道列表\n(未配置外部 M3U 文件)"
            }
            binding.dataSourceStatus.text = statusText
        }
        
        // 显示缓存信息
        lifecycleScope.launch {
            val cacheStats = ChannelCache.getCacheStats(context)
            binding.cacheInfo.text = cacheStats
        }
        
        // 清除缓存按钮
        binding.clearCache.setOnClickListener {
            lifecycleScope.launch {
                ChannelCache.clearCache(context)
                Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                
                // 重新加载缓存信息
                val cacheStats = ChannelCache.getCacheStats(context)
                binding.cacheInfo.text = cacheStats
                
                (activity as MainActivity).settingActive()
            }
        }

        return binding.root
    }

    fun setVersionName(versionName: String) {
        binding.versionName.text = versionName
    }

    internal class OnClickListenerCheckVersion(private val updateManager: UpdateManager) :
        View.OnClickListener {
        override fun onClick(view: View?) {
            updateManager.checkAndUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
//        updateManager.destroy()
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

