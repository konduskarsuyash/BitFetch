# 🎵 BeatFetch Bot

![Telegram Bot](https://img.shields.io/badge/Telegram-Bot-blue?logo=telegram)
![Java](https://img.shields.io/badge/Java-17+-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?logo=springboot)
![Python](https://img.shields.io/badge/Python-3.x-blue?logo=python)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?logo=docker)

A powerful Telegram bot that downloads music from YouTube, provides lyrics, and delivers high-quality audio with embedded thumbnails.

**Bot:** [@BEAT_FETCH_BOT](https://t.me/BEAT_FETCH_BOT)

---

## 📋 Table of Contents

- [Features](#-features)
- [Demo](#-demo)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [Docker Deployment](#-docker-deployment)
- [Project Structure](#-project-structure)
- [API Endpoints](#-api-endpoints)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

- 🎵 **Music Download** – Download songs from YouTube as high-quality MP3 (192 kbps)
- 🔍 **Smart Search** – Auto YouTube search for song queries
- 🖼️ **Thumbnail Embedding** – Embeds artwork into MP3 metadata
- 📝 **Lyrics Fetching** – Gets lyrics using Genius API
- ☁️ **Cloudinary Integration** – Uploads thumbnails for sharing
- ⚡ **Async Processing** – Smooth background operations
- 🔒 **Secure Webhooks** – Webhook secret validation
- 🐳 **Docker Ready** – Easily deployable container setup
- 🍪 **Cookie Support** – Handles YouTube restricted content
- ⏱️ **Timeout Handling** – Prevents duplicate or long-running tasks
- 🛡️ **Error Handling** – Automatic retries and fallback logic

---

## 🎬 Demo

### Scan QR Code to Try the Bot

![Bot QR Code](qr-code.jpg)

**Telegram:** [@BEAT_FETCH_BOT](https://t.me/BEAT_FETCH_BOT)

### How to Use

1. Start the bot with `/start`
2. Send a song name or YouTube link
3. Receive MP3 with embedded artwork
4. Use `/lyrics <song>` for lyrics
5. Use `/thumbnail <url>` for the video thumbnail

---
---

## 🛠️ Tech Stack

### Backend
- Java 17+
- Spring Boot 3.3.5
- Spring Web
- Spring Async

### Python Layer
- Python 3.x
- yt-dlp
- Mutagen
- FFmpeg

### External Services
- Telegram Bot API
- Genius API
- Cloudinary

### DevOps
- Docker
- Docker Compose
- Maven

---

## 📦 Prerequisites

### Local
- Java 17+
- Maven 3.9+
- Python 3.x
- FFmpeg

### Docker
- Docker
- Docker Compose

---

## 🚀 Installation

### Option 1: Local Development

1. Clone the repository:

```bash
git clone https://github.com/konduskarsuyash/BitFetch.git
cd BitFetch
yaml


2. **Install Python dependencies**
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install --upgrade yt-dlp mutagen requests
```

3. **Install system dependencies**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y ffmpeg python3-pip

# macOS
brew install ffmpeg python3

# Windows
# Download FFmpeg from https://ffmpeg.org/download.html
```

4. **Build the project**
```bash
mvn clean package -DskipTests
```

5. **Run the application**
```bash
java -jar target/Bot-0.0.1-SNAPSHOT.jar
```

### Option 2: Docker (Recommended)

```bash
docker-compose up -d
```

---

## ⚙️ Configuration

### 1. Create .env file

Create a .env file in the project root:

```env
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_bot_token_here
WEBHOOK_SECRET=your_random_secret_string

# Genius API (for lyrics)
GENIUS_API_TOKEN=your_genius_api_token

# Cloudinary (for thumbnail hosting)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# YouTube Cookies (Base64 encoded, optional)
YOUTUBE_COOKIES_BASE64=your_base64_encoded_cookies
```

### 2. Obtain Required Tokens

#### Telegram Bot Token
1. Message [@BotFather](https://t.me/BotFather) on Telegram
2. Send `/newbot` and follow instructions
3. Copy the bot token

#### Genius API Token
1. Visit [Genius API](https://genius.com/api-clients)
2. Create a new API client
3. Generate an access token

#### Cloudinary Credentials
1. Sign up at [Cloudinary](https://cloudinary.com/)
2. Get your cloud name, API key, and API secret from dashboard

#### YouTube Cookies (Optional but Recommended)
For downloading age-restricted or members-only content:

1. Export cookies from your browser using an extension (e.g., "Get cookies.txt")
2. Encode the file to base64:
   ```bash
   # Linux/Mac
   base64 -w 0 cookies.txt > cookies_base64.txt
   
   # Windows PowerShell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("cookies.txt")) > cookies_base64.txt
   ```
3. Copy the content to `YOUTUBE_COOKIES_BASE64` in .env

### 3. Set Webhook URL

After deploying, set your webhook:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=https://your-domain.com/webhook/<YOUR_WEBHOOK_SECRET>"
```

---

## 💻 Usage

### Basic Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/start` | Start the bot | `/start` |
| `/help` | Get help message | `/help` |
| **Song name** | Download song by name | `Bohemian Rhapsody` |
| **YouTube URL** | Download from URL | `https://youtube.com/watch?v=...` |
| `/lyrics <song>` | Get song lyrics | `/lyrics shape of you` |
| `/thumbnail <url>` | Get video thumbnail | `/thumbnail https://youtube.com/...` |

### Example Workflow

1. **Download a song by name:**
   ```
   User: Imagine Dragons Believer
   Bot: 🔍 Searching on YouTube...
   Bot: ⏳ Downloading...
   Bot: [Sends MP3 file with embedded artwork]
   ```

2. **Download from YouTube URL:**
   ```
   User: https://www.youtube.com/watch?v=dQw4w9WgXcQ
   Bot: ⏳ Downloading...
   Bot: [Sends MP3 file]
   ```

3. **Get lyrics:**
   ```
   User: /lyrics never gonna give you up
   Bot: [Returns full lyrics]
   ```

---

## 🐳 Docker Deployment

### Build and Run

```bash
# Build image
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Docker Configuration

The Dockerfile uses a multi-stage build:
- **Build stage**: Compiles Java application with Maven
- **Runtime stage**: Runs application with Python, yt-dlp, and FFmpeg

### Environment Variables in Docker

Environment variables are loaded from .env file automatically by Docker Compose.

---

## 📁 Project Structure

```
Bot/
├── src/
│   ├── main/
│   │   ├── java/com/telegram/Bot/
│   │   │   ├── TelegramBotApplication.java   # Main application
│   │   │   ├── WebhookController.java        # Webhook endpoint
│   │   │   ├── YouTubeService.java           # YouTube search
│   │   │   ├── DownloadService.java          # MP3 download orchestration
│   │   │   ├── LyricsService.java            # Genius lyrics fetching
│   │   │   ├── ThumbnailService.java         # Thumbnail extraction
│   │   │   ├── TelegramService.java          # Telegram API interactions
│   │   │   ├── AsyncConfig.java              # Async configuration
│   │   │   └── HealthController.java         # Health check endpoint
│   │   └── resources/
│   │       └── application.properties        # Spring configuration
│   └── test/                                 # Test files
├── download.py                               # Python download script
├── cookies_base64.txt                        # Encoded YouTube cookies
├── cookies.txt                               # YouTube cookies (plain)
├── Dockerfile                                # Docker build instructions
├── docker-compose.yml                        # Docker Compose config
├── pom.xml                                   # Maven dependencies
├── .env                                      # Environment variables
└── README.md                                 # This file
```

---

## 🔌 API Endpoints

### Health Check
```
GET /health
```
Returns application health status.

### Webhook Endpoint
```
POST /webhook/{secret}
```
Receives updates from Telegram Bot API.

**Headers:**
- `Content-Type: application/json`

**Body:** Telegram Update object

---

## 🔧 Advanced Configuration

### Adjusting Timeouts

In DownloadService.java:
```java
private static final int TIMEOUT_SECONDS = 120; // Adjust as needed
```

In WebhookController.java:
```java
private static final int MESSAGE_TIMEOUT_SECONDS = 60; // Adjust as needed
```

### Audio Quality

In download.py, modify quality:
```python
"preferredquality": "320",  # Change from 192 to 320 for higher quality
```

### Player Client Selection

Modify `PLAYER_CLIENTS` in download.py to change fallback order:
```python
PLAYER_CLIENTS = [
    ["android"],
    ["ios"],
    ["mweb"],
]
```

---

## 🐛 Troubleshooting

### Common Issues

**1. Bot not responding**
- Check if webhook is set correctly
- Verify .env variables are loaded
- Check logs: `docker-compose logs -f`

**2. Download fails**
- Ensure FFmpeg is installed
- Check if YouTube cookies are properly encoded
- Verify Python dependencies: `pip list`

**3. "Sign in to confirm your age" error**
- Add YouTube cookies (see Configuration section)
- Ensure cookies are not expired

**4. Lyrics not found**
- Verify Genius API token is valid
- Check if song name is spelled correctly

**5. Docker build fails**
- Clear Docker cache: `docker system prune -a`
- Check Docker daemon is running

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👨‍💻 Author

**Suyash Konduskar**

- GitHub: [@konduskarsuyash](https://github.com/konduskarsuyash)
- Telegram Bot: [@BEAT_FETCH_BOT](https://t.me/BEAT_FETCH_BOT)

---

## 🙏 Acknowledgments

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - Excellent YouTube downloader
- [Telegram Bot API](https://core.telegram.org/bots/api) - Bot platform
- [Genius API](https://docs.genius.com/) - Lyrics database
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Cloudinary](https://cloudinary.com/) - Media hosting

---

## ⭐ Star History

If you find this project useful, please consider giving it a star! ⭐

---

<div align="center">

Made with ❤️ by [Suyash Konduskar](https://github.com/konduskarsuyash)

</div>
```

