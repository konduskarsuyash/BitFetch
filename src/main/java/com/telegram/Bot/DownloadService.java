package com.telegram.Bot;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class DownloadService {

    public File downloadMp3(String url) {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));

            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    "/app/download.py",
                    url,
                    tempDir.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            String lastJsonLine = null;

            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line);

                // capture only the JSON (Python prints it last)
                if (line.startsWith("{") && line.endsWith("}")) {
                    lastJsonLine = line;
                }
            }

            process.waitFor();

            if (lastJsonLine == null) {
                throw new RuntimeException("Invalid response from Python script");
            }

            JSONObject json = new JSONObject(lastJsonLine);

            if (!json.getString("status").equals("success")) {
                throw new RuntimeException(json.optString("message", "Unknown error"));
            }

            String filePath = json.getString("file_path");
            File file = new File(filePath);

            if (!file.exists() || file.length() < 10000) {
                throw new RuntimeException("MP3 file invalid.");
            }

            return file;

        } catch (Exception e) {
            throw new RuntimeException("MP3 download failed: " + e.getMessage());
        }
    }
}
