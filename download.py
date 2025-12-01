import sys
import os
import requests
import yt_dlp
from mutagen.id3 import ID3, APIC, TIT2
from mutagen.mp3 import MP3
import json
import re

def sanitize(name):
    return re.sub(r'[<>:"/\\|?*]', '_', name).strip()

if len(sys.argv) < 3:
    print(json.dumps({"status": "error", "message": "Usage: python download.py <url> <output_folder>"}))
    sys.exit(1)

url = sys.argv[1]
output_folder = sys.argv[2]

# ============ 1. Extract Metadata ==============
info_opts = {
    "quiet": True,
    "skip_download": True,
}

try:
    with yt_dlp.YoutubeDL(info_opts) as ydl:
        info = ydl.extract_info(url, download=False)
except:
    print(json.dumps({"status": "error", "message": "Failed to fetch video info"}))
    sys.exit(1)

if not info:
    print(json.dumps({"status": "error", "message": "Missing metadata"}))
    sys.exit(1)

raw_title = info.get("title", "song")
title = sanitize(raw_title)
thumbnail_url = info.get("thumbnail")

mp3_path = os.path.join(output_folder, title + ".mp3")

# ============ 2. Download MP3 ==============
ydl_opts = {
    "format": "bestaudio[ext=m4a]/bestaudio/best",
    "outtmpl": os.path.join(output_folder, title),

    # ✔ REMOVE WINDOWS PATH
    # "ffmpeg_location": "C:\\ffmpeg\\bin",

    "noplaylist": True,
    "nocheckcertificate": True,
    "forceipv4": True,

    "extractor_args": {
        "youtube": {
            "player_client": ["android"]
        }
    },

    "postprocessors": [{
        "key": "FFmpegExtractAudio",
        "preferredcodec": "mp3",
        "preferredquality": "192",
    }]
}

try:
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        ydl.download([url])
except Exception as e:
    print(json.dumps({"status": "error", "message": str(e)}))
    sys.exit(1)

if not os.path.exists(mp3_path) or os.path.getsize(mp3_path) < 50000:
    print(json.dumps({"status": "error", "message": "MP3 file empty or invalid"}))
    sys.exit(1)

# ============ 3. Embed Thumbnail ==============
try:
    if thumbnail_url:
        img_data = requests.get(thumbnail_url, timeout=10).content

        audio = MP3(mp3_path, ID3=ID3)
        try:
            audio.add_tags()
        except:
            pass

        audio.tags["APIC"] = APIC(
            encoding=3,
            mime="image/jpeg",
            type=3,
            desc="Cover",
            data=img_data,
        )

        audio.tags["TIT2"] = TIT2(encoding=3, text=raw_title)
        audio.save()

except Exception as e:
    print(json.dumps({
        "status": "warning",
        "message": str(e),
        "file_path": mp3_path,
        "title": raw_title
    }))
    sys.exit(0)

# ============ 4. SUCCESS JSON ==============
print(json.dumps({
    "status": "success",
    "file_path": mp3_path,
    "title": raw_title
}))
