###################################
# 1. BUILD STAGE
###################################
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

# Give permissions to mvnw (very important on Linux)
RUN chmod +x mvnw

# Build the Spring Boot project
RUN ./mvnw -q -DskipTests clean package


###################################
# 2. RUNTIME STAGE
###################################
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install curl first
RUN apt-get update && apt-get install -y curl

# Install Node.js (yt-dlp needs JS runtime)
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Install Python, pip, venv, ffmpeg
RUN apt-get install -y python3 python3-pip python3-venv ffmpeg && \
    apt-get clean

# Create Python venv
RUN python3 -m venv /app/venv

# Install python packages inside virtual env
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install yt-dlp mutagen requests

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy download.py
COPY download.py /app/download.py
RUN chmod +x /app/download.py

ENV PATH="/app/venv/bin:$PATH"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
