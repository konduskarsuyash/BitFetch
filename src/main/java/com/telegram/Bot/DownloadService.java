package com.telegram.Bot;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.*;

@Service
public class DownloadService {

    private static final int TIMEOUT_SECONDS = 120; // 2 minutes max

    public File downloadMp3(String url) {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));

            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    "/app/download.py",
                    url,
                    tempDir.getAbsolutePath()
            );

            // KEEP STREAMS SEPARATE - this is key!
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Read stderr in background (for logging only)
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        System.err.println("[yt-dlp] " + line);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // Read stdout (where JSON result is)
            BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                output.append(line).append("\n");
                // Also print for debugging
                System.out.println("[stdout] " + line);
            }

            // Wait with timeout
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Download timeout after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Python script failed with exit code: " + exitCode);
            }

            // Find the last JSON line in stdout
            String[] lines = output.toString().split("\n");
            String lastJsonLine = null;

            for (int i = lines.length - 1; i >= 0; i--) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    lastJsonLine = trimmed;
                    break;
                }
            }

            if (lastJsonLine == null) {
                throw new RuntimeException("No JSON response from Python script. Output: " + output);
            }

            JSONObject json = new JSONObject(lastJsonLine);

            if (!json.getString("status").equals("success")) {
                throw new RuntimeException(json.optString("message", "Unknown error"));
            }

            String filePath = json.getString("file_path");
            File file = new File(filePath);

            if (!file.exists()) {
                throw new RuntimeException("MP3 file not found: " + filePath);
            }

            if (file.length() < 10000) {
                throw new RuntimeException("MP3 file too small: " + file.length() + " bytes");
            }

            System.out.println("✅ Download successful: " + file.getName() + " (" + file.length() + " bytes)");
            return file;

        } catch (Exception e) {
            throw new RuntimeException("MP3 download failed: " + e.getMessage(), e);
        }
    }
}