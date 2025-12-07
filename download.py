import sys
import os
import requests
import yt_dlp
from mutagen.id3 import ID3, APIC, TIT2
from mutagen.mp3 import MP3
import json
import re
import time

import base64

# Setup cookies from environment variable
COOKIES_FILE = None
if os.getenv("YOUTUBE_COOKIES_BASE64"):
    try:
        cookies_data = base64.b64decode(os.getenv("YOUTUBE_COOKIES_BASE64"))
        COOKIES_FILE = "/tmp/cookies.txt"
        with open(COOKIES_FILE, "wb") as f:
            f.write(cookies_data)
        print("Cookies loaded from environment variable", file=sys.stderr)
    except Exception as e:
        print(f"Failed to load cookies: {e}", file=sys.stderr)
elif os.path.exists("/app/cookies.txt"):
    COOKIES_FILE = "/app/cookies.txt"
    print("Cookies loaded from /app/cookies.txt", file=sys.stderr)
else:
    print("WARNING: No cookies found - downloads may fail", file=sys.stderr)
def sanitize(name):
    return re.sub(r'[<>:"/\\|?*]', '_', name).strip()

if len(sys.argv) < 3:
    print(json.dumps({"status": "error", "message": "Usage: python download.py <url> <output_folder>"}))
    sys.exit(1)

url = sys.argv[1]
output_folder = sys.argv[2]

# Check if cookies file exists
COOKIES_FILE = "/app/cookies.txt"
use_cookies = os.path.exists(COOKIES_FILE)

# Client fallback order
PLAYER_CLIENTS = [
    ["android"],
    ["ios"],
    ["android", "web"],
]

def get_ydl_opts(output_folder, title, player_client, use_cookies=False):
    opts = {
        "format": "bestaudio[ext=m4a]/bestaudio/best",
        "outtmpl": os.path.join(output_folder, title),
        "ffmpeg_location": "/usr/bin/ffmpeg",
        "noplaylist": True,
        "nocheckcertificate": True,
        "forceipv4": True,
        "geo_bypass": True,
        "quiet": False,
        "no_warnings": False,

        "extractor_args": {
            "youtube": {
                "player_client": player_client,
                "skip_native_hls": True
            }
        },

        "http_headers": {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        },

        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": "mp3",
            "preferredquality": "192",
        }]
    }

    # Add cookies if available
    if use_cookies:
        opts["cookiefile"] = COOKIES_FILE

    return opts

# ============ 1. Extract Metadata with Retry ==============
info = None
for attempt, client in enumerate(PLAYER_CLIENTS, 1):
    try:
        info_opts = {
            "quiet": True,
            "skip_download": True,
            "extractor_args": {
                "youtube": {
                    "player_client": client,
                    "skip_native_hls": True
                }
            }
        }

        # Add cookies if available
        if use_cookies:
            info_opts["cookiefile"] = COOKIES_FILE

        with yt_dlp.YoutubeDL(info_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            break
    except Exception as e:
        if attempt < len(PLAYER_CLIENTS):
            time.sleep(1)
            continue
        else:
            error_msg = f"Failed to fetch video info: {str(e)}"
            if not use_cookies:
                error_msg += " | TIP: Add cookies.txt for authentication"
            print(json.dumps({"status": "error", "message": error_msg}))
            sys.exit(1)

if not info:
    print(json.dumps({"status": "error", "message": "Missing metadata"}))
    sys.exit(1)

raw_title = info.get("title", "song")
title = sanitize(raw_title)
thumbnail_url = info.get("thumbnail")

mp3_path = os.path.join(output_folder, title + ".mp3")

# ============ 2. Download MP3 with Retry ==============
download_success = False

for attempt, client in enumerate(PLAYER_CLIENTS, 1):
    try:
        ydl_opts = get_ydl_opts(output_folder, title, client, use_cookies)

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])

        # Verify file exists and has content
        if os.path.exists(mp3_path) and os.path.getsize(mp3_path) > 50000:
            download_success = True
            break

    except Exception as e:
        if attempt < len(PLAYER_CLIENTS):
            time.sleep(2)
            # Clean up partial file if exists
            if os.path.exists(mp3_path):
                os.remove(mp3_path)
            continue
        else:
            error_msg = f"Download failed: {str(e)}"
            if not use_cookies:
                error_msg += " | TIP: Add cookies.txt for authentication"
            print(json.dumps({"status": "error", "message": error_msg}))
            sys.exit(1)

if not download_success:
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
    # Thumbnail failure shouldn't stop the process
    pass

# ============ 4. SUCCESS JSON ==============
print(json.dumps({
    "status": "success",
    "file_path": mp3_path,
    "title": raw_title
}))