package com.telegram.Bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final int MESSAGE_TIMEOUT_SECONDS = 60;

    // Prevent duplicate processing
    private final Set<String> processingMessages = ConcurrentHashMap.newKeySet();

    @Value("${telegram.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private LyricsService lyricsService;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private TelegramService telegramService;


    @PostMapping("/webhook/{secret}")
    public ResponseEntity<String> onUpdateReceived(
            @PathVariable("secret") String secret,
            @RequestBody Update update) {

        // Validate secret
        if (!secret.equals(webhookSecret)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        // ============================
        // 🔥 CALLBACK QUERY HANDLER
        // ============================
        if (update.getCallbackQuery() != null) {

            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (data.startsWith("LYRICS:")) {

                String songName = data.substring(7);

                try {
                    String lyrics = lyricsService.getLyrics(songName);

                    if (lyrics == null || lyrics.isEmpty()) {
                        telegramService.sendMessage(chatId, "❌ No lyrics found.");
                        return ResponseEntity.ok("OK");
                    }

                    if (lyrics.length() > 4000) {
                        lyrics = lyrics.substring(0, 3990) + "...";
                    }

                    telegramService.sendMessage(
                            chatId,
                            "🎼 *Lyrics for:* " + songName + "\n\n" + lyrics
                    );

                } catch (Exception e) {
                    telegramService.sendMessage(chatId, "❌ Lyrics unavailable.");
                }
            }

            return ResponseEntity.ok("OK");
        }

        // ============================
        // 🔥 NORMAL MESSAGE HANDLER
        // ============================
        if (update.getMessage() != null && update.getMessage().hasText()) {

            Long chatId = update.getMessage().getChatId();
            String songName = update.getMessage().getText();
            Integer messageDate = update.getMessage().getDate();

            // ✅ FILTER OLD MESSAGES
            long currentTime = Instant.now().getEpochSecond();
            long messageAge = currentTime - messageDate;

            if (messageAge > MESSAGE_TIMEOUT_SECONDS) {
                log.warn("⏭️ Ignoring old message ({}s old): {}", messageAge, songName);
                return ResponseEntity.ok("OK");
            }

            log.info("🎵 Song requested: {} (message age: {}s)", songName, messageAge);

            // 🔒 PREVENT DUPLICATE PROCESSING
            String messageKey = chatId + ":" + messageDate;

            if (!processingMessages.add(messageKey)) {
                log.warn("🔄 Duplicate message detected, ignoring: {}", songName);
                return ResponseEntity.ok("OK");
            }

            // 🚀 PROCESS ASYNCHRONOUSLY - Don't wait for download!
            processDownloadAsync(chatId, songName, messageKey);

            // ✅ IMMEDIATELY RETURN - Don't let Telegram timeout!
            return ResponseEntity.ok("OK");
        }

        return ResponseEntity.ok("OK");
    }
    // ============================
    // 🤖 COMMAND HANDLER
    // ============================
    private void handleCommand(Long chatId, String command) {
        String cmd = command.toLowerCase().split(" ")[0]; // Get command without parameters

        switch (cmd) {
            case "/start":
                String welcomeMessage = """
                        👋 *Welcome to Music Download Bot!*
                        
                        🎵 Just send me any song name and I'll:
                        • Find it on YouTube
                        • Download it as MP3
                        • Send it to you with lyrics option
                        
                        *Example:*
                        Just type: `Shape of You`
                        
                        Ready to download some music? 🎶
                        """;
                telegramService.sendMessage(chatId, welcomeMessage);
                log.info("✅ Sent welcome message to chatId: {}", chatId);
                break;

            case "/help":
                String helpMessage = """
                        ℹ️ *How to use this bot:*
                        
                        1️⃣ Send me a song name
                        2️⃣ I'll search YouTube for it
                        3️⃣ Download and send you the MP3
                        4️⃣ Click "Show Lyrics" button for lyrics
                        
                        *Commands:*
                        /start - Start the bot
                        /help - Show this help message
                        /about - About this bot
                        
                        Just send a song name to get started! 🎵
                        """;
                telegramService.sendMessage(chatId, helpMessage);
                log.info("✅ Sent help message to chatId: {}", chatId);
                break;

            case "/about":
                String aboutMessage = """
                        ℹ️ *About Music Download Bot*
                        
                        This bot helps you download music from YouTube as MP3 files.
                        
                        *Features:*
                        🎵 High-quality MP3 downloads
                        📖 Lyrics support
                        🖼️ Thumbnail preview
                        ⚡ Fast delivery via Cloudinary CDN
                        
                        Made with ❤️
                        """;
                telegramService.sendMessage(chatId, aboutMessage);
                log.info("✅ Sent about message to chatId: {}", chatId);
                break;

            default:
                telegramService.sendMessage(chatId, "❓ Unknown command. Use /help to see available commands.");
                log.info("⚠️ Unknown command: {} from chatId: {}", command, chatId);
                break;
        }
    }
    @Async("taskExecutor")
    public void processDownloadAsync(Long chatId, String songName, String messageKey) {
        try {
            telegramService.sendMessage(chatId, "🔍 Searching... 🎵");

            // 1) Search YouTube
            String youtubeLink = youTubeService.searchOnYouTube(songName);
            log.info("✅ YouTube URL found: {}", youtubeLink);

            // 2) Send thumbnail (with error handling)
            try {
                String thumbnailUrl = thumbnailService.getThumbnailUrl(youtubeLink);
                if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    telegramService.sendPhoto(chatId, thumbnailUrl);
                }
            } catch (Exception e) {
                log.warn("Could not send thumbnail: {}", e.getMessage());
            }

            telegramService.sendMessage(chatId, "⬇️ Downloading audio... ⏳");

            // 3) Download MP3 via Python
            File mp3File = downloadService.downloadMp3(youtubeLink);

            // 4) Send MP3 WITH BUTTON
            telegramService.sendAudioWithButton(chatId, mp3File, songName);

            log.info("✅ Successfully processed: {}", songName);

        } catch (Exception e) {
            log.error("❌ Error processing request for '{}': {}", songName, e.getMessage(), e);
            telegramService.sendMessage(chatId, "❌ Error: " + e.getMessage());
        } finally {
            // 🔓 Remove from processing set
            processingMessages.remove(messageKey);
        }
    }

    @GetMapping("/webhook/{secret}")
    public ResponseEntity<String> webhookHealth(@PathVariable String secret) {
        if (!secret.equals(webhookSecret)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok("OK");
    }

}