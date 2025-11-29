// Subscription Service - Abonelik yönetimi için yardımcı fonksiyonlar

/**
 * Kullanıcının abonelik durumunu kontrol eder
 * @param {string} userId - Kullanıcı ID veya telefon numarası
 * @param {Map} subscriptions - Abonelikler Map'i
 * @param {Object} PRICING_PLANS - Fiyatlandırma planları
 * @returns {Object} Abonelik durumu ve plan bilgisi
 */
export function checkSubscriptionStatus(userId, subscriptions, PRICING_PLANS) {
  // Kullanıcının aktif aboneliğini bul
  let activeSubscription = null;
  for (const [id, sub] of subscriptions.entries()) {
    if (sub.userId === userId || sub.phoneNumber === userId) {
      const expiresAt = new Date(sub.expiresAt);
      const now = new Date();
      
      if (expiresAt > now && sub.status === 'active') {
        activeSubscription = sub;
        break;
      }
    }
  }
  
  if (!activeSubscription) {
    // Abonelik yoksa veya süresi dolmuşsa ücretsiz plan
    return {
      planId: 'free',
      status: 'active',
      isPremium: false,
      plan: PRICING_PLANS.free,
      expiresAt: null
    };
  }
  
  const plan = PRICING_PLANS[activeSubscription.planId];
  
  return {
    ...activeSubscription,
    isPremium: true,
    plan
  };
}

/**
 * Kullanıcının günlük kullanım limitini kontrol eder
 * @param {string} userId - Kullanıcı ID
 * @param {Object} subscriptionStatus - Abonelik durumu
 * @param {number} dailyUsageMinutes - Günlük kullanılan dakika
 * @returns {Object} Limit durumu
 */
export function checkDailyLimit(userId, subscriptionStatus, dailyUsageMinutes) {
  if (subscriptionStatus.isPremium) {
    return {
      allowed: true,
      remaining: Infinity,
      limit: Infinity
    };
  }
  
  // Ücretsiz plan: günde 30 dakika
  const dailyLimit = 30;
  const remaining = Math.max(0, dailyLimit - dailyUsageMinutes);
  
  return {
    allowed: remaining > 0,
    remaining,
    limit: dailyLimit
  };
}

/**
 * Abonelik süresinin dolup dolmadığını kontrol eder
 * @param {Object} subscription - Abonelik objesi
 * @returns {boolean} Süre dolmuş mu?
 */
export function isSubscriptionExpired(subscription) {
  if (!subscription || !subscription.expiresAt) {
    return true;
  }
  
  const expiresAt = new Date(subscription.expiresAt);
  const now = new Date();
  
  return expiresAt <= now;
}

