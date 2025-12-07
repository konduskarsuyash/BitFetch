package com.telegram.Bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.time.Instant;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final int MESSAGE_TIMEOUT_SECONDS = 60; // Ignore messages older than 60 seconds

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
                log.warn("Ignoring old message ({}s old): {}", messageAge, songName);
                return ResponseEntity.ok("OK"); // Acknowledge but don't process
            }

            log.info("Song requested: {} (message age: {}s)", songName, messageAge);

            telegramService.sendMessage(chatId, "🔍 Searching... 🎵");

            try {
                // 1) Search YouTube
                String youtubeLink = youTubeService.searchOnYouTube(songName);
                log.info("YouTube URL found: {}", youtubeLink);

                // 2) Send thumbnail (with error handling)
                try {
                    String thumbnailUrl = thumbnailService.getThumbnailUrl(youtubeLink);
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        telegramService.sendPhoto(chatId, thumbnailUrl);
                    }
                } catch (Exception e) {
                    log.warn("Could not send thumbnail: {}", e.getMessage());
                    // Continue without thumbnail
                }

                telegramService.sendMessage(chatId, "⬇️ Downloading audio... ⏳");

                // 3) Download MP3 via Python
                File mp3File = downloadService.downloadMp3(youtubeLink);

                // 4) Send MP3 WITH BUTTON
                telegramService.sendAudioWithButton(chatId, mp3File, songName);

                log.info("✅ Successfully processed: {}", songName);

            } catch (Exception e) {
                log.error("Error processing request for '{}': {}", songName, e.getMessage(), e);
                telegramService.sendMessage(chatId, "❌ Error: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/webhook/{secret}")
    public ResponseEntity<String> webhookHealth(@PathVariable String secret) {
        if (!secret.equals(webhookSecret)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok("OK");
    }

}