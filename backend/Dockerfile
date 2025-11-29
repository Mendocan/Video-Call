# Signaling Server için Dockerfile
FROM node:18-alpine

# Çalışma dizini
WORKDIR /app

# Package dosyalarını kopyala
COPY package*.json ./

# Bağımlılıkları yükle
RUN npm install --production

# Kaynak kodları kopyala
COPY . .

# Port'u aç
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:8080/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# Sunucuyu başlat
CMD ["node", "src/signalingServer.js"]

