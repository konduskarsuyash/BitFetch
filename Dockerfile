###############################
# 1. BUILD STAGE
###############################
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy project files
COPY . .

# Give mvnw execute permission (required on Render)
RUN chmod +x mvnw

# Build Spring Boot JAR
RUN ./mvnw -q -DskipTests clean package


###############################
# 2. RUNTIME STAGE
###############################
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install Python + FFmpeg + Node.js (for yt-dlp YouTube JS engine)
RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-venv ffmpeg curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean

# Create Python virtual environment
RUN python3 -m venv /app/venv

# Install Python packages inside venv
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install yt-dlp mutagen requests

# Copy Spring Boot JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy yt-dlp Python script
COPY download.py /app/download.py

# Make Python script executable
RUN chmod +x /app/download.py

# Ensure Java calls the venv Python
ENV PATH="/app/venv/bin:$PATH"

# Expose Spring Boot port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
