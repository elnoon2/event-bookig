FROM node:18-slim

# Install chromium and fonts for character support
RUN apt-get update && apt-get install -y \
    chromium \
    libxss1 \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Set working directory inside container
WORKDIR /app

# Copy dependency configs
COPY package*.json ./

# Install npm packages
RUN npm ci --only=production

# Copy the rest of the application files
COPY . .

# Configure Puppeteer to use the installed Chromium binary
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Express app port
EXPOSE 3000

CMD ["node", "index.js"]
