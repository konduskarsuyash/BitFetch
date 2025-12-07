package com.telegram.Bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // =====================================
    //  SEND NORMAL TEXT MESSAGE
    // =====================================
    public void sendMessage(Long chatId, String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, entity, String.class);
    }

    // =====================================
    //  SEND PHOTO
    // =====================================
    public void sendPhoto(Long chatId, String photoUrl) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendPhoto";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("photo", photoUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, entity, String.class);
    }

    // =====================================
    //  SEND AUDIO FILE WITH BUTTON (VIA FILE.IO)
    // =====================================
    public void sendAudioWithButton(Long chatId, File audioFile, String songName) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("📤 Starting file upload process for: {} ({} bytes)",
                    audioFile.getName(), audioFile.length());

            // Step 1: Upload to file.io (fast CDN)
            String fileUrl = uploadToFileIo(audioFile);
            long uploadTime = System.currentTimeMillis() - startTime;
            log.info("✅ Uploaded to file.io in {}ms: {}", uploadTime, fileUrl);

            // Step 2: Send audio URL to Telegram
            sendAudioByUrl(chatId, fileUrl, songName);
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ Total send time: {}ms", totalTime);

        } catch (Exception e) {
            log.error("❌ Failed to send audio via file.io: {}", e.getMessage(), e);

            // Fallback: Try direct upload to Telegram
            log.warn("⚠️ Falling back to direct Telegram upload...");
            sendAudioDirectly(chatId, audioFile, songName);

        } finally {
            // Clean up temp file
            if (audioFile.exists()) {
                audioFile.delete();
                log.info("🗑️ Deleted temp file: {}", audioFile.getName());
            }
        }
    }

    // =====================================
    //  UPLOAD TO FILE.IO
    // =====================================
    private String uploadToFileIo(File file) {
        String uploadUrl = "https://file.io";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    uploadUrl,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                throw new RuntimeException("Empty response from file.io");
            }

            Boolean success = (Boolean) responseBody.get("success");
            if (success == null || !success) {
                String message = (String) responseBody.get("message");
                throw new RuntimeException("file.io upload failed: " + message);
            }

            String link = (String) responseBody.get("link");
            if (link == null || link.isEmpty()) {
                throw new RuntimeException("No link returned from file.io");
            }

            return link;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to file.io: " + e.getMessage(), e);
        }
    }

    // =====================================
    //  SEND AUDIO BY URL (TELEGRAM DOWNLOADS IT)
    // =====================================
    private void sendAudioByUrl(Long chatId, String audioUrl, String songName) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendAudio";

        // Build inline keyboard with lyrics button
        String keyboardJson = String.format(
                "{\"inline_keyboard\":[[{\"text\":\"📖 Show Lyrics\",\"callback_data\":\"LYRICS:%s\"}]]}",
                songName.replace("\"", "'")
        );

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("audio", audioUrl);
        body.put("reply_markup", keyboardJson);
        body.put("title", songName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            log.info("✅ Audio URL sent to Telegram successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to send audio URL to Telegram: " + e.getMessage(), e);
        }
    }

    // =====================================
    //  FALLBACK: DIRECT UPLOAD TO TELEGRAM
    // =====================================
    private void sendAudioDirectly(Long chatId, File audioFile, String songName) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendAudio";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String keyboardJson = String.format(
                "{\"inline_keyboard\":[[{\"text\":\"📖 Show Lyrics\",\"callback_data\":\"LYRICS:%s\"}]]}",
                songName.replace("\"", "'")
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("audio", new FileSystemResource(audioFile));
        body.add("reply_markup", keyboardJson);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, requestEntity, String.class);
            log.info("✅ Audio uploaded directly to Telegram");
        } catch (Exception e) {
            log.error("❌ Direct upload also failed: {}", e.getMessage(), e);
            throw new RuntimeException("Both file.io and direct upload failed", e);
        }
    }

    // =====================================
    //  LEGACY METHOD (KEPT FOR COMPATIBILITY)
    // =====================================
    public void sendAudio(Long chatId, File audioFile) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendAudio";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("audio", new FileSystemResource(audioFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        restTemplate.postForObject(url, requestEntity, String.class);

        audioFile.delete();
    }
}