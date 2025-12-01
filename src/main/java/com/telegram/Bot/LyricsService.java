package com.telegram.Bot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LyricsService {

    @Value("${genius.api.token}")
    private String geniusToken;

    private final RestTemplate rest = new RestTemplate();
    private static final String GENIUS_API_BASE = "https://api.genius.com";

    // ===============================
    // PUBLIC METHOD
    // ===============================
    public String getLyrics(String query) {
        try {
            String url = searchGenius(query);
            if (url == null) return "⚠️ Song not found on Genius.";

            String lyrics = scrapeLyrics(url);
            if (lyrics == null || lyrics.isBlank()) return "⚠️ Lyrics not found.";

            return lyrics;

        } catch (Exception e) {
            return "⚠️ Error: " + e.getMessage();
        }
    }

    // ===============================
    // 1. SEARCH SONG USING GENIUS API
    // ===============================
    private String searchGenius(String query) {
        try {
            String clean = query.replace("lyrics", "").trim();

            String apiUrl =
                    GENIUS_API_BASE + "/search?q=" + clean.replace(" ", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + geniusToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    rest.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());
            JSONArray hits = json
                    .getJSONObject("response")
                    .getJSONArray("hits");

            if (hits.isEmpty()) return null;

            return hits
                    .getJSONObject(0)
                    .getJSONObject("result")
                    .getString("url");

        } catch (Exception e) {
            return null;
        }
    }

    // ===============================
    // 2. SCRAPE LYRICS FROM PAGE
    // ===============================
    private String scrapeLyrics(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .referrer("https://genius.com")
                    .get();

            // New Genius structure — primary container
            Elements containers = doc.select("div[data-lyrics-container='true']");

            if (!containers.isEmpty()) {
                return extractFromContainers(containers);
            }

            // FALLBACK (older pages)
            Element oldLyrics = doc.selectFirst("div.lyrics");
            if (oldLyrics != null) {
                return oldLyrics.text();
            }

            // Newer React-based fallback
            Elements paragraphs = doc.select("div[class*=Lyrics__Container]");
            if (!paragraphs.isEmpty()) {
                return extractReactLyrics(paragraphs);
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    // Extract lyrics from data-lyrics-container="true"
    private String extractFromContainers(Elements divs) {
        StringBuilder sb = new StringBuilder();

        for (Element div : divs) {
            for (Element br : div.select("br")) {
                br.after("\n");
            }
            sb.append(div.text()).append("\n");
        }

        return sb.toString().trim();
    }

    // Extract from React container format
    private String extractReactLyrics(Elements divs) {
        StringBuilder sb = new StringBuilder();

        for (Element div : divs) {
            for (Element br : div.select("br")) br.after("\n");
            sb.append(div.text()).append("\n");
        }

        return sb.toString().trim();
    }
}
