# Builder stage
FROM python:3.10-slim AS builder
WORKDIR /app

# Install minimal build tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake wget \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements
COPY requirements.txt .

# Install dlib-bin (prebuilt) first
RUN pip install --no-cache-dir dlib-bin==19.24.1

# Install the rest of your Python dependencies except dlib
RUN pip install --no-cache-dir -r requirements.txt --no-deps

# Copy app source code
COPY . .

# Runtime stage
FROM python:3.10-slim
WORKDIR /app

# Install runtime libraries
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg libsm6 libxext6 libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy installed Python packages from builder
COPY --from=builder /usr/local /usr/local

# Copy app code
COPY . .

# Expose port for Cloud Run
EXPOSE 8080

# Use dynamic port for Cloud Run
CMD ["sh", "-c", "uvicorn server:app --host 0.0.0.0 --port ${PORT:-8080}"]
