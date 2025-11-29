import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: process.env.NODE_ENV === 'production' ? 'standalone' : undefined,
  // For Electron, we need to handle static assets
  assetPrefix: process.env.NODE_ENV === 'production' ? './' : undefined,
  
  // Güvenlik headers
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block',
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()',
          },
          {
            key: 'Content-Security-Policy',
            value: [
              "default-src 'self'",
              "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // Next.js için gerekli
              "style-src 'self' 'unsafe-inline'", // Tailwind için gerekli
              "img-src 'self' data: https:",
              "font-src 'self' data:",
              // Backend API ve Signaling Server (WebSocket)
              "connect-src 'self' http://localhost:3000 https://api.videocall.app ws://192.168.1.20:8080 wss://192.168.1.20:8080",
              "frame-ancestors 'none'",
              "base-uri 'self'",
              "form-action 'self'",
            ].join('; '),
          },
        ],
      },
    ];
  },
};

export default nextConfig;
