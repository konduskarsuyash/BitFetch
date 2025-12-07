package com.telegram.Bot;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret}")
    private String cloudinaryApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        // Initialize Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryCloudName,
                "api_key", cloudinaryApiKey,
                "api_secret", cloudinaryApiSecret
        ));
        log.info("✅ Cloudinary initialized: {}", cloudinaryCloudName);
    }

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
    //  SEND AUDIO FILE WITH BUTTON (VIA CLOUDINARY)
    // =====================================
    public void sendAudioWithButton(Long chatId, File audioFile, String songName) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("📤 Starting file upload to Cloudinary for: {} ({} bytes)",
                    audioFile.getName(), audioFile.length());

            // Upload to Cloudinary
            String audioUrl = uploadToCloudinary(audioFile);
            long uploadTime = System.currentTimeMillis() - startTime;
            log.info("✅ Uploaded to Cloudinary in {}ms: {}", uploadTime, audioUrl);

            // Send audio URL to Telegram
            sendAudioByUrl(chatId, audioUrl, songName);
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ Total send time: {}ms", totalTime);

        } catch (Exception e) {
            log.error("❌ Cloudinary upload failed: {}", e.getMessage(), e);
            log.warn("⚠️ Falling back to direct Telegram upload...");
            sendAudioDirectly(chatId, audioFile, songName);

        } finally {
            if (audioFile.exists()) {
                audioFile.delete();
                log.info("🗑️ Deleted temp file: {}", audioFile.getName());
            }
        }
    }

    // =====================================
    //  UPLOAD TO CLOUDINARY
    // =====================================
    private String uploadToCloudinary(File file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "resource_type", "video",  // Use "video" for audio files in Cloudinary
                    "folder", "telegram-audio",
                    "use_filename", true,
                    "unique_filename", true
            ));

            String secureUrl = (String) uploadResult.get("secure_url");

            if (secureUrl == null || secureUrl.isEmpty()) {
                throw new RuntimeException("No URL returned from Cloudinary");
            }

            log.info("📦 Cloudinary public_id: {}", uploadResult.get("public_id"));
            return secureUrl;

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    // =====================================
    //  SEND AUDIO BY URL (TELEGRAM DOWNLOADS IT)
    // =====================================
    private void sendAudioByUrl(Long chatId, String audioUrl, String songName) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendAudio";

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
            throw new RuntimeException("Both Cloudinary and direct upload failed", e);
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