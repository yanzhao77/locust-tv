#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
locust-tv IPTV 自动更新脚本
功能: 从 iptv-org/iptv 拉取最新 M3U 播放列表,验证有效性,生成精简频道文件
适用环境: Termux / Linux / macOS
依赖: Python 3.6+
"""

import os
import sys
import time
import json
import tempfile
import shutil
import re
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError
from concurrent.futures import ThreadPoolExecutor, as_completed

# ==================== 配置参数 ====================

# jsDelivr 镜像地址 (确保中国大陆可直连)
JSDELIVR_M3U_URL = "https://cdn.jsdelivr.net/gh/iptv-org/iptv@master/index.m3u"

# 输出路径 (Android 可访问的共享目录)
OUTPUT_DIR = "/data/local/tmp/locust-tv"
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "channels.m3u")

# 验证参数
MAX_CHANNELS = 500          # 最多保留频道数
VALIDATION_TIMEOUT = 5      # HEAD 请求超时时间(秒)
MAX_WORKERS = 20            # 并发验证线程数
USER_AGENT = "locust-tv/1.0 (IPTV Updater)"

# 优先保留的频道关键词 (中文频道优先)
PRIORITY_KEYWORDS = [
    "CCTV", "卫视", "凤凰", "翡翠", "明珠", "本港",
    "HBO", "Discovery", "National Geographic", "BBC"
]

# ==================== 工具函数 ====================

def log(message, level="INFO"):
    """日志输出"""
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}", flush=True)


def download_m3u(url):
    """下载 M3U 文件"""
    log(f"开始下载 M3U 文件: {url}")
    try:
        req = Request(url, headers={"User-Agent": USER_AGENT})
        with urlopen(req, timeout=30) as response:
            content = response.read().decode('utf-8')
            log(f"下载成功, 大小: {len(content)} 字节")
            return content
    except (URLError, HTTPError) as e:
        log(f"下载失败: {e}", "ERROR")
        return None


def parse_m3u(content):
    """解析 M3U 文件,提取频道信息"""
    log("开始解析 M3U 文件")
    channels = []
    lines = content.strip().split('\n')
    
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        # 匹配 #EXTINF 行
        if line.startswith('#EXTINF:'):
            # 提取频道名称 (最后一个逗号后的内容)
            match = re.search(r',(.+)$', line)
            channel_name = match.group(1).strip() if match else "Unknown"
            
            # 提取 tvg-logo (如果存在)
            logo_match = re.search(r'tvg-logo="([^"]+)"', line)
            logo_url = logo_match.group(1) if logo_match else ""
            
            # 提取 group-title (如果存在)
            group_match = re.search(r'group-title="([^"]+)"', line)
            group_title = group_match.group(1) if group_match else ""
            
            # 下一行应该是流媒体 URL
            i += 1
            if i < len(lines):
                stream_url = lines[i].strip()
                if stream_url and not stream_url.startswith('#'):
                    channels.append({
                        "name": channel_name,
                        "url": stream_url,
                        "logo": logo_url,
                        "group": group_title
                    })
        i += 1
    
    log(f"解析完成, 共提取 {len(channels)} 个频道")
    return channels


def validate_stream(channel):
    """验证单个流媒体 URL 是否有效"""
    url = channel["url"]
    try:
        req = Request(url, headers={"User-Agent": USER_AGENT}, method='HEAD')
        with urlopen(req, timeout=VALIDATION_TIMEOUT) as response:
            # 检查 HTTP 状态码
            if response.status == 200:
                return channel, True
            else:
                return channel, False
    except Exception:
        return channel, False


def validate_channels(channels, max_workers=MAX_WORKERS):
    """并发验证频道有效性"""
    log(f"开始验证频道有效性 (并发数: {max_workers})")
    valid_channels = []
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(validate_stream, ch): ch for ch in channels}
        
        completed = 0
        for future in as_completed(futures):
            completed += 1
            channel, is_valid = future.result()
            
            if is_valid:
                valid_channels.append(channel)
                log(f"[{completed}/{len(channels)}] ✓ {channel['name']}")
            else:
                log(f"[{completed}/{len(channels)}] ✗ {channel['name']} (无效)")
            
            # 达到最大频道数后提前终止
            if len(valid_channels) >= MAX_CHANNELS:
                log(f"已达到最大频道数 ({MAX_CHANNELS}), 停止验证")
                executor.shutdown(wait=False, cancel_futures=True)
                break
    
    log(f"验证完成, 有效频道: {len(valid_channels)}/{len(channels)}")
    return valid_channels


def prioritize_channels(channels):
    """根据关键词对频道进行优先级排序"""
    def get_priority(channel):
        name = channel["name"]
        for i, keyword in enumerate(PRIORITY_KEYWORDS):
            if keyword.lower() in name.lower():
                return i
        return len(PRIORITY_KEYWORDS)
    
    return sorted(channels, key=get_priority)


def generate_m3u(channels, output_path):
    """生成 M3U 文件"""
    log(f"生成 M3U 文件: {output_path}")
    
    # 使用临时文件 + 原子写入,防止写入中断
    temp_fd, temp_path = tempfile.mkstemp(suffix='.m3u', dir=os.path.dirname(output_path))
    
    try:
        with os.fdopen(temp_fd, 'w', encoding='utf-8') as f:
            f.write("#EXTM3U\n")
            for ch in channels:
                # 写入 #EXTINF 行
                extinf = f'#EXTINF:-1'
                if ch.get('logo'):
                    extinf += f' tvg-logo="{ch["logo"]}"'
                if ch.get('group'):
                    extinf += f' group-title="{ch["group"]}"'
                extinf += f',{ch["name"]}\n'
                
                f.write(extinf)
                f.write(f'{ch["url"]}\n')
        
        # 原子移动到目标路径
        shutil.move(temp_path, output_path)
        log(f"文件生成成功: {output_path}")
        return True
    
    except Exception as e:
        log(f"文件生成失败: {e}", "ERROR")
        if os.path.exists(temp_path):
            os.remove(temp_path)
        return False


def ensure_output_dir():
    """确保输出目录存在"""
    if not os.path.exists(OUTPUT_DIR):
        try:
            os.makedirs(OUTPUT_DIR, mode=0o755)
            log(f"创建输出目录: {OUTPUT_DIR}")
        except Exception as e:
            log(f"创建目录失败: {e}", "ERROR")
            return False
    return True


# ==================== 主流程 ====================

def main():
    """主函数"""
    log("=" * 60)
    log("locust-tv IPTV 自动更新脚本启动")
    log("=" * 60)
    
    # 1. 确保输出目录存在
    if not ensure_output_dir():
        log("输出目录创建失败, 退出", "ERROR")
        return 1
    
    # 2. 下载 M3U 文件
    m3u_content = download_m3u(JSDELIVR_M3U_URL)
    if not m3u_content:
        log("M3U 文件下载失败, 退出", "ERROR")
        return 1
    
    # 3. 解析频道列表
    channels = parse_m3u(m3u_content)
    if not channels:
        log("未解析到任何频道, 退出", "ERROR")
        return 1
    
    # 4. 优先级排序
    channels = prioritize_channels(channels)
    log(f"频道优先级排序完成")
    
    # 5. 验证频道有效性
    valid_channels = validate_channels(channels[:MAX_CHANNELS * 3])  # 验证 3 倍数量以确保足够
    
    if not valid_channels:
        log("没有有效频道, 退出", "ERROR")
        return 1
    
    # 6. 限制频道数量
    final_channels = valid_channels[:MAX_CHANNELS]
    log(f"最终保留 {len(final_channels)} 个频道")
    
    # 7. 生成 M3U 文件
    if not generate_m3u(final_channels, OUTPUT_FILE):
        log("M3U 文件生成失败, 退出", "ERROR")
        return 1
    
    # 8. 生成统计信息
    stats = {
        "update_time": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total_channels": len(final_channels),
        "source": "iptv-org/iptv (via jsDelivr)"
    }
    
    stats_file = os.path.join(OUTPUT_DIR, "update_stats.json")
    try:
        with open(stats_file, 'w', encoding='utf-8') as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)
        log(f"统计信息已保存: {stats_file}")
    except Exception as e:
        log(f"统计信息保存失败: {e}", "WARN")
    
    log("=" * 60)
    log("更新完成!")
    log(f"频道文件: {OUTPUT_FILE}")
    log(f"频道数量: {len(final_channels)}")
    log("=" * 60)
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
