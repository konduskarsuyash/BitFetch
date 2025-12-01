###############################
# 1. BUILD STAGE
###############################
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy project files
COPY . .

# IMPORTANT: Give mvnw execute permission
RUN chmod +x mvnw

# Build Spring Boot JAR
RUN ./mvnw -q -DskipTests clean package


###############################
# 2. RUNTIME STAGE
###############################
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install Python, pip, virtualenv, ffmpeg
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg && \
    apt-get clean

# Create Python virtual environment
RUN python3 -m venv /app/venv

# Install Python packages inside venv
RUN /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install yt-dlp mutagen requests

# Copy Spring Boot JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy download.py script
COPY download.py /app/download.py

# Make download.py executable (optional but safe)
RUN chmod +x /app/download.py

# Add venv to PATH so Java app can call python normally
ENV PATH="/app/venv/bin:$PATH"

# Expose Spring Boot port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
