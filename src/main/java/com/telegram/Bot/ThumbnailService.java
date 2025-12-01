package com.telegram.Bot;

import org.springframework.stereotype.Service;

@Service
public class ThumbnailService {

    public String getThumbnailUrl(String youtubeUrl) {
        try {
            String videoId = youtubeUrl.split("v=")[1].substring(0, 11);
            return "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
        }
        catch (Exception e) {
            return null;
        }
    }
}
