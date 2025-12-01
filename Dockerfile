###############################
# 2. RUNTIME STAGE
###############################
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install curl first
RUN apt-get update && apt-get install -y curl

# Install Node.js 18 (needed by yt-dlp for JS runtime)
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Install Python, pip, venv, ffmpeg
RUN apt-get install -y python3 python3-pip python3-venv ffmpeg && \
    apt-get clean

# Create Python venv
RUN python3 -m venv /app/venv

# Install python packages inside venv
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install yt-dlp mutagen requests

# Copy JAR + download script
COPY --from=build /app/target/*.jar app.jar
COPY download.py /app/download.py

RUN chmod +x /app/download.py

ENV PATH="/app/venv/bin:$PATH"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
