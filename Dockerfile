# ============================
# 1. BUILD STAGE
# ============================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN ./mvnw -q -DskipTests clean package



# ============================
# 2. RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jdk

# Install Python, pip, ffmpeg
RUN apt-get update && apt-get install -y python3 python3-pip python3-venv ffmpeg

WORKDIR /app

# Create Python virtual environment
RUN python3 -m venv /app/venv

# Activate venv & install packages
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install yt-dlp mutagen requests

# Copy jar and Python script
COPY --from=build /app/target/Bot-0.0.1-SNAPSHOT.jar app.jar
COPY download.py /app/download.py

# Expose the port
EXPOSE 8080

# Ensure Python venv is used inside Spring Boot
ENV PATH="/app/venv/bin:$PATH"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
