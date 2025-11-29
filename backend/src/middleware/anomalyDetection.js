/**
 * Anomaly Detection Middleware
 * Şüpheli aktiviteleri tespit eder ve loglar
 */

// Şüpheli aktivite kayıtları
const suspiciousActivities = new Map(); // IP/UserId -> { count, lastSeen, activities[] }

// Anomali tespit kriterleri
const ANOMALY_THRESHOLDS = {
  // Aynı IP'den çok fazla farklı cihaz kaydı
  multipleDevices: 5, // 5 farklı cihaz
  
  // Aynı IP'den çok fazla farklı telefon numarası
  multiplePhones: 10, // 10 farklı telefon numarası
  
  // Aynı telefon numarasından çok fazla farklı cihaz
  multipleDevicesPerPhone: 3, // 3'ten fazla cihaz (limit 2)
  
  // Çok fazla başarısız giriş denemesi
  failedLogins: 5, // 5 başarısız giriş
  
  // Çok fazla başarısız cihaz kaydı
  failedDeviceRegistrations: 3, // 3 başarısız kayıt
  
  // Anormal istek paterni (çok hızlı istekler)
  rapidRequests: 50, // 1 dakikada 50 istek
};

// Aktivite tipleri
const ACTIVITY_TYPES = {
  DEVICE_REGISTER: 'device_register',
  DEVICE_VERIFY: 'device_verify',
  LOGIN_ATTEMPT: 'login_attempt',
  LOGIN_FAILED: 'login_failed',
  SUBSCRIPTION_CHECK: 'subscription_check',
  RAPID_REQUESTS: 'rapid_requests'
};

/**
 * Şüpheli aktivite kaydet
 */
function recordSuspiciousActivity(identifier, activityType, details = {}) {
  const now = Date.now();
  
  if (!suspiciousActivities.has(identifier)) {
    suspiciousActivities.set(identifier, {
      count: 1,
      lastSeen: now,
      activities: [{
        type: activityType,
        timestamp: now,
        details
      }]
    });
  } else {
    const record = suspiciousActivities.get(identifier);
    record.count++;
    record.lastSeen = now;
    record.activities.push({
      type: activityType,
      timestamp: now,
      details
    });
    
    // Son 100 aktiviteyi tut
    if (record.activities.length > 100) {
      record.activities = record.activities.slice(-100);
    }
    
    suspiciousActivities.set(identifier, record);
  }
  
  // Anomali tespit et
  checkAnomalies(identifier);
}

/**
 * Anomali kontrolü yap
 */
function checkAnomalies(identifier) {
  const record = suspiciousActivities.get(identifier);
  if (!record) return;
  
  const anomalies = [];
  
  // Son 1 saatteki aktiviteleri filtrele
  const oneHourAgo = Date.now() - (60 * 60 * 1000);
  const recentActivities = record.activities.filter(a => a.timestamp > oneHourAgo);
  
  // Başarısız giriş denemeleri
  const failedLogins = recentActivities.filter(a => a.type === ACTIVITY_TYPES.LOGIN_FAILED).length;
  if (failedLogins >= ANOMALY_THRESHOLDS.failedLogins) {
    anomalies.push({
      type: 'multiple_failed_logins',
      severity: 'high',
      count: failedLogins,
      message: `${failedLogins} başarısız giriş denemesi tespit edildi`
    });
  }
  
  // Başarısız cihaz kayıtları
  const failedRegistrations = recentActivities.filter(
    a => a.type === ACTIVITY_TYPES.DEVICE_REGISTER && a.details.success === false
  ).length;
  if (failedRegistrations >= ANOMALY_THRESHOLDS.failedDeviceRegistrations) {
    anomalies.push({
      type: 'multiple_failed_registrations',
      severity: 'medium',
      count: failedRegistrations,
      message: `${failedRegistrations} başarısız cihaz kaydı tespit edildi`
    });
  }
  
  // Hızlı istekler
  const rapidRequests = recentActivities.filter(
    a => a.type === ACTIVITY_TYPES.RAPID_REQUESTS
  ).length;
  if (rapidRequests > 0) {
    anomalies.push({
      type: 'rapid_requests',
      severity: 'high',
      count: rapidRequests,
      message: 'Anormal hızda istek tespit edildi'
    });
  }
  
  // Anomali varsa logla ve uyar
  if (anomalies.length > 0) {
    console.warn(`⚠️ ANOMALY DETECTED [${identifier}]:`, anomalies);
    // TODO: Production'da alerting sistemi (email, SMS, Slack, vb.)
  }
}

/**
 * IP adresini al
 */
function getClientIP(req) {
  return req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
         req.headers['x-real-ip'] ||
         req.connection?.remoteAddress ||
         req.socket?.remoteAddress ||
         'unknown';
}

/**
 * Anomaly detection middleware
 */
export function anomalyDetection(req, res, next) {
  const ip = getClientIP(req);
  const userId = req.user?.id || 'anonymous';
  
  // İstek sayısını kontrol et (hızlı istekler)
  const now = Date.now();
  const recentRequests = res.locals.recentRequests || [];
  const oneMinuteAgo = now - (60 * 1000);
  const requestsInLastMinute = recentRequests.filter(t => t > oneMinuteAgo).length;
  
  if (requestsInLastMinute >= ANOMALY_THRESHOLDS.rapidRequests) {
    recordSuspiciousActivity(ip, ACTIVITY_TYPES.RAPID_REQUESTS, {
      count: requestsInLastMinute,
      endpoint: req.path
    });
  }
  
  // Response'a listener ekle (başarılı/başarısız durumu kaydetmek için)
  const originalJson = res.json.bind(res);
  res.json = function(data) {
    // Başarısız istekleri kaydet
    if (!data.success) {
      if (req.path.includes('/auth/login')) {
        recordSuspiciousActivity(ip, ACTIVITY_TYPES.LOGIN_FAILED, {
          endpoint: req.path,
          error: data.error
        });
      } else if (req.path.includes('/devices/register')) {
        recordSuspiciousActivity(ip, ACTIVITY_TYPES.DEVICE_REGISTER, {
          success: false,
          error: data.error
        });
      }
    } else {
      // Başarılı istekleri de kaydet (pattern analizi için)
      if (req.path.includes('/devices/register')) {
        recordSuspiciousActivity(ip, ACTIVITY_TYPES.DEVICE_REGISTER, {
          success: true,
          phoneNumber: req.body.phoneNumber
        });
      }
    }
    
    return originalJson(data);
  };
  
  next();
}

/**
 * Şüpheli aktivite raporu al
 */
export function getSuspiciousActivities(identifier) {
  return suspiciousActivities.get(identifier) || null;
}

/**
 * Tüm şüpheli aktiviteleri al
 */
export function getAllSuspiciousActivities() {
  return Array.from(suspiciousActivities.entries()).map(([identifier, record]) => ({
    identifier,
    ...record
  }));
}

/**
 * Eski kayıtları temizle (bellek yönetimi)
 */
setInterval(() => {
  const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
  for (const [identifier, record] of suspiciousActivities.entries()) {
    if (record.lastSeen < oneDayAgo) {
      suspiciousActivities.delete(identifier);
    }
  }
}, 60 * 60 * 1000); // Her saatte bir temizle

