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

def sanitize(name):
    return re.sub(r'[<>:"/\\|?*]', '_', name).strip()

# ===== SETUP COOKIES FROM ENVIRONMENT =====
COOKIES_FILE = None
if os.getenv("YOUTUBE_COOKIES_BASE64"):
    try:
        cookies_data = base64.b64decode(os.getenv("YOUTUBE_COOKIES_BASE64"))
        COOKIES_FILE = "/tmp/cookies.txt"
        with open(COOKIES_FILE, "wb") as f:
            f.write(cookies_data)

        # Debug: Check if file was created and has content
        if os.path.exists(COOKIES_FILE):
            file_size = os.path.getsize(COOKIES_FILE)
            print(f"✅ Cookies loaded from environment variable (size: {file_size} bytes)", file=sys.stderr)

            # Debug: Show first line of cookies file
            with open(COOKIES_FILE, "r") as f:
                first_line = f.readline().strip()
                print(f"Debug: First line: {first_line[:50]}...", file=sys.stderr)
        else:
            print("❌ Cookie file not created!", file=sys.stderr)
            COOKIES_FILE = None

    except Exception as e:
        print(f"❌ Failed to load cookies: {e}", file=sys.stderr)
        COOKIES_FILE = None
elif os.path.exists("/app/cookies.txt"):
    COOKIES_FILE = "/app/cookies.txt"
    file_size = os.path.getsize(COOKIES_FILE)
    print(f"✅ Cookies loaded from /app/cookies.txt (size: {file_size} bytes)", file=sys.stderr)
else:
    print("⚠️ WARNING: No cookies found - downloads may fail", file=sys.stderr)

if len(sys.argv) < 3:
    print(json.dumps({"status": "error", "message": "Usage: python download.py <url> <output_folder>"}))
    sys.exit(1)

url = sys.argv[1]
output_folder = sys.argv[2]

# Client fallback order - try more aggressive options
PLAYER_CLIENTS = [
    ["android"],
    ["android_creator"],
    ["ios"],
    ["mweb"],
    ["tv_embedded"],
]

def get_ydl_opts(output_folder, title, player_client):
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
                "skip_native_hls": True,
            }
        },

        "http_headers": {
            "User-Agent": "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
        },

        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": "mp3",
            "preferredquality": "192",
        }]
    }

    # Add cookies if available
    if COOKIES_FILE and os.path.exists(COOKIES_FILE):
        opts["cookiefile"] = COOKIES_FILE
        print(f"Using cookies file: {COOKIES_FILE}", file=sys.stderr)
    else:
        print("No cookies file available!", file=sys.stderr)

    return opts

# ============ 1. Extract Metadata with Retry ==============
info = None
last_error = None

for attempt, client in enumerate(PLAYER_CLIENTS, 1):
    try:
        print(f"Attempt {attempt}/{len(PLAYER_CLIENTS)} with client: {client}", file=sys.stderr)

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

        if COOKIES_FILE and os.path.exists(COOKIES_FILE):
            info_opts["cookiefile"] = COOKIES_FILE

        with yt_dlp.YoutubeDL(info_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            print(f"✅ Success with client: {client}", file=sys.stderr)
            break

    except Exception as e:
        last_error = str(e)
        print(f"❌ Failed with client {client}: {str(e)[:100]}", file=sys.stderr)
        if attempt < len(PLAYER_CLIENTS):
            time.sleep(2)
            continue
        else:
            error_msg = f"Failed to fetch video info: {last_error}"
            if not COOKIES_FILE:
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
        print(f"Download attempt {attempt}/{len(PLAYER_CLIENTS)} with client: {client}", file=sys.stderr)

        ydl_opts = get_ydl_opts(output_folder, title, client)

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])

        if os.path.exists(mp3_path) and os.path.getsize(mp3_path) > 50000:
            download_success = True
            print(f"✅ Download success with client: {client}", file=sys.stderr)
            break

    except Exception as e:
        print(f"❌ Download failed with client {client}: {str(e)[:100]}", file=sys.stderr)
        if attempt < len(PLAYER_CLIENTS):
            time.sleep(3)
            if os.path.exists(mp3_path):
                os.remove(mp3_path)
            continue
        else:
            error_msg = f"Download failed: {str(e)}"
            if not COOKIES_FILE:
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
        audio.tags["APIC"] = APIC(encoding=3, mime="image/jpeg", type=3, desc="Cover", data=img_data)
        audio.tags["TIT2"] = TIT2(encoding=3, text=raw_title)
        audio.save()
except:
    pass

# ============ 4. SUCCESS JSON ==============

print(json.dumps({
    "status": "success",
    "file_path": mp3_path,
    "title": raw_title
}))

# To:
result = json.dumps({
    "status": "success",
    "file_path": mp3_path,
    "title": raw_title
})
print(result, flush=True)  # Force immediate flush
sys.stdout.flush()