###################################
# 1. BUILD STAGE
###################################
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy everything
COPY . .

# Build the project
RUN mvn clean package -DskipTests


###################################
# 2. RUNTIME STAGE
###################################
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean

# Create Python venv
RUN python3 -m venv /app/venv

# Install python packages
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install --upgrade yt-dlp mutagen requests

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy download.py
COPY download.py /app/download.py
RUN chmod +x /app/download.py

ENV PATH="/app/venv/bin:$PATH"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]