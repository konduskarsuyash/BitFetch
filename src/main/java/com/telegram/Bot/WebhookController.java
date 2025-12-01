package com.telegram.Bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

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
        if (update.getMessage() != null) {

            Long chatId = update.getMessage().getChatId();
            String songName = update.getMessage().getText();

            log.info("Song requested: {}", songName);

            telegramService.sendMessage(chatId, "Searching... 🎵");

            try {
                // 1) Search YouTube
                String youtubeLink = youTubeService.searchOnYouTube(songName);

                // 2) Send thumbnail
                String thumbnailUrl = thumbnailService.getThumbnailUrl(youtubeLink);
                if (thumbnailUrl != null) {
                    telegramService.sendPhoto(chatId, thumbnailUrl);
                }

                telegramService.sendMessage(chatId, "Downloading audio... ⏳");

                // 3) Download MP3 via Python
                File mp3File = downloadService.downloadMp3(youtubeLink);

                // 4) Send MP3 WITH BUTTON
                telegramService.sendAudioWithButton(chatId, mp3File, songName);

            } catch (Exception e) {
                log.error("Error processing request", e);
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
