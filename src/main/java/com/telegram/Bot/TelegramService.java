package com.telegram.Bot;

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
    //  SEND AUDIO FILE (MP3)
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

        // delete temp file
        audioFile.delete();
    }

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

    public void sendAudioWithButton(Long chatId, File audioFile, String songName) {

        String url = "https://api.telegram.org/bot" + botToken + "/sendAudio";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // ----- Build Inline Keyboard -----
        String keyboardJson = """
        {
          "inline_keyboard": [
            [
              { "text": "📖 Show Lyrics", "callback_data": "LYRICS:%s" }
            ]
          ]
        }
        """.formatted(songName.replace("\"", "'"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("audio", new FileSystemResource(audioFile));
        body.add("reply_markup", keyboardJson);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        restTemplate.postForObject(url, requestEntity, String.class);

        audioFile.delete();
    }




}
