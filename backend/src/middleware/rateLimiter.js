/**
 * Rate Limiting Middleware
 * API isteklerini sınırlar ve DDoS saldırılarına karşı korur
 */

// IP bazlı rate limiting
const requestCounts = new Map(); // IP -> { count, resetTime }

// Rate limit ayarları
const RATE_LIMITS = {
  // Genel endpoint'ler için
  default: {
    windowMs: 15 * 60 * 1000, // 15 dakika
    maxRequests: 100 // Maksimum 100 istek
  },
  // Auth endpoint'leri için (daha sıkı)
  auth: {
    windowMs: 15 * 60 * 1000, // 15 dakika
    maxRequests: 5 // Maksimum 5 istek (brute force koruması)
  },
  // Device registration için
  device: {
    windowMs: 60 * 60 * 1000, // 1 saat
    maxRequests: 10 // Maksimum 10 istek
  },
  // Subscription endpoint'leri için
  subscription: {
    windowMs: 15 * 60 * 1000, // 15 dakika
    maxRequests: 20 // Maksimum 20 istek
  },
  // Feedback endpoint'leri için
  feedback: {
    windowMs: 60 * 60 * 1000, // 1 saat
    maxRequests: 5 // Maksimum 5 geri bildirim (spam koruması)
  }
};

/**
 * IP adresini al (proxy arkasındaysa X-Forwarded-For header'ından)
 */
function getClientIP(req) {
  return req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
         req.headers['x-real-ip'] ||
         req.connection?.remoteAddress ||
         req.socket?.remoteAddress ||
         'unknown';
}

/**
 * Rate limit kontrolü
 */
function checkRateLimit(ip, limitType = 'default') {
  const now = Date.now();
  const limit = RATE_LIMITS[limitType] || RATE_LIMITS.default;
  
  // Eski kayıtları temizle
  if (requestCounts.has(ip)) {
    const record = requestCounts.get(ip);
    if (now > record.resetTime) {
      requestCounts.delete(ip);
    }
  }
  
  // Yeni kayıt oluştur veya mevcut kaydı al
  if (!requestCounts.has(ip)) {
    const resetTime = now + limit.windowMs;
    requestCounts.set(ip, {
      count: 1,
      resetTime: resetTime,
      limitType
    });
    return { allowed: true, remaining: limit.maxRequests - 1, resetTime: resetTime };
  }
  
  const record = requestCounts.get(ip);
  
  // Limit tipi değişmişse sıfırla
  if (record.limitType !== limitType) {
    const resetTime = now + limit.windowMs;
    requestCounts.set(ip, {
      count: 1,
      resetTime: resetTime,
      limitType
    });
    return { allowed: true, remaining: limit.maxRequests - 1, resetTime: resetTime };
  }
  
  // Limit aşılmış mı?
  if (record.count >= limit.maxRequests) {
    return {
      allowed: false,
      remaining: 0,
      resetTime: record.resetTime
    };
  }
  
  // İsteği say
  record.count++;
  requestCounts.set(ip, record);
  
  return {
    allowed: true,
    remaining: limit.maxRequests - record.count,
    resetTime: record.resetTime
  };
}

/**
 * Rate limiting middleware factory
 */
export function rateLimiter(limitType = 'default') {
  return (req, res, next) => {
    const ip = getClientIP(req);
    const result = checkRateLimit(ip, limitType);
    
    if (!result.allowed) {
      const retryAfter = result.resetTime 
        ? Math.ceil((result.resetTime - Date.now()) / 1000)
        : 60; // Varsayılan 60 saniye
      return res.status(429).json({
        success: false,
        error: 'Çok fazla istek. Lütfen daha sonra tekrar deneyin.',
        retryAfter: retryAfter
      });
    }
    
    // Rate limit bilgilerini header'a ekle
    res.setHeader('X-RateLimit-Limit', RATE_LIMITS[limitType]?.maxRequests || RATE_LIMITS.default.maxRequests);
    res.setHeader('X-RateLimit-Remaining', result.remaining);
    if (result.resetTime) {
      res.setHeader('X-RateLimit-Reset', new Date(result.resetTime).toISOString());
    }
    
    next();
  };
}

/**
 * Eski kayıtları temizle (bellek yönetimi)
 */
setInterval(() => {
  const now = Date.now();
  for (const [ip, record] of requestCounts.entries()) {
    if (now > record.resetTime) {
      requestCounts.delete(ip);
    }
  }
}, 5 * 60 * 1000); // Her 5 dakikada bir temizle

