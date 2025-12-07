package com.telegram.Bot;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class YouTubeService {

    public String searchOnYouTube(String query) {
        try {
            String url = "https://www.youtube.com/results?search_query=" +
                    query.replace(" ", "+");

            String html = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get()
                    .html();

            // Extract videoId
            String videoId = html.split("watch\\?v=")[1].substring(0, 11);

            return "https://www.youtube.com/watch?v=" + videoId;

        } catch (Exception e) {
            throw new RuntimeException("YouTube search failed: " + e.getMessage());
        }
    }
}
