/**
 * Bildirim görevleri (cron jobs veya scheduled tasks)
 * Abonelik yenileme hatırlatıcıları, sona erme uyarıları vb.
 */

import emailService from '../services/emailService.js';
import smsService from '../services/smsService.js';

/**
 * Abonelik yenileme hatırlatıcılarını kontrol et ve gönder
 * @param {Map} subscriptions - Abonelikler Map'i
 * @param {Function} getUserByEmail - E-posta ile kullanıcı getir
 */
export async function checkAndSendRenewalReminders(subscriptions, getUserByEmail) {
  const now = new Date();
  const remindersSent = new Set(); // Aynı kullanıcıya tekrar göndermemek için

  for (const [id, sub] of subscriptions.entries()) {
    if (sub.status !== 'active') continue;

    const expiresAt = new Date(sub.expiresAt);
    const daysUntilExpiry = Math.ceil((expiresAt - now) / (1000 * 60 * 60 * 24));

    // 30, 14, 7, 3, 1 gün kala hatırlatıcı gönder
    if ([30, 14, 7, 3, 1].includes(daysUntilExpiry)) {
      const reminderKey = `${sub.userId}_${daysUntilExpiry}`;
      if (remindersSent.has(reminderKey)) continue;

      try {
        // Kullanıcı bilgilerini al
        const user = getUserByEmail ? await getUserByEmail(sub.userId) : null;
        if (!user || !user.email) continue;

        // E-posta hatırlatıcısı
        await emailService.sendSubscriptionRenewalReminder(user.email, daysUntilExpiry);

        // SMS hatırlatıcısı (opsiyonel, kullanıcı tercihine bağlı)
        if (user.phone) {
          await smsService.sendSubscriptionReminder(user.phone, daysUntilExpiry);
        }

        remindersSent.add(reminderKey);
        console.log(`Yenileme hatırlatıcısı gönderildi: ${user.email} (${daysUntilExpiry} gün kaldı)`);
      } catch (error) {
        console.error(`Hatırlatıcı gönderim hatası (${sub.userId}):`, error);
      }
    }

    // Abonelik sona erdi mi? (0 gün kaldı veya geçti)
    if (daysUntilExpiry <= 0 && daysUntilExpiry > -1) {
      const expiryKey = `${sub.userId}_expired`;
      if (remindersSent.has(expiryKey)) continue;

      try {
        const user = getUserByEmail ? await getUserByEmail(sub.userId) : null;
        if (!user || !user.email) continue;

        // Sona erme uyarısı
        await emailService.sendSubscriptionExpiryWarning(user.email, expiresAt);

        // SMS uyarısı
        if (user.phone) {
          await smsService.sendSubscriptionExpiryWarning(user.phone, expiresAt);
        }

        remindersSent.add(expiryKey);
        console.log(`Sona erme uyarısı gönderildi: ${user.email}`);
      } catch (error) {
        console.error(`Sona erme uyarısı gönderim hatası (${sub.userId}):`, error);
      }
    }
  }
}

/**
 * Görüşme randevusu hatırlatıcılarını kontrol et ve gönder
 * @param {Array} scheduledCalls - Zamanlanmış görüşmeler listesi
 * @param {Function} getUserByEmail - E-posta ile kullanıcı getir
 */
export async function checkAndSendCallReminders(scheduledCalls, getUserByEmail) {
  const now = new Date();
  const remindersSent = new Set();

  for (const call of scheduledCalls) {
    if (call.isCompleted || call.isCancelled) continue;

    const scheduledTime = new Date(call.scheduledTime);
    const minutesUntilCall = Math.floor((scheduledTime - now) / (1000 * 60));

    // 15 dakika kala hatırlatıcı gönder
    if (minutesUntilCall <= 15 && minutesUntilCall > 0) {
      const reminderKey = `${call.id}_15min`;
      if (remindersSent.has(reminderKey)) continue;

      try {
        // Kullanıcı bilgilerini al (call.userId veya call.userEmail ile)
        const user = getUserByEmail ? await getUserByEmail(call.userId || call.userEmail) : null;
        if (!user || !user.email) continue;

        await emailService.sendCallReminder(user.email, {
          contactName: call.contactName,
          scheduledTime: call.scheduledTime,
          notes: call.notes
        });

        // SMS hatırlatıcısı
        if (user.phone) {
          await smsService.sendCallReminder(
            user.phone,
            call.contactName,
            call.scheduledTime
          );
        }

        remindersSent.add(reminderKey);
        console.log(`Görüşme hatırlatıcısı gönderildi: ${user.email} (${call.contactName})`);
      } catch (error) {
        console.error(`Görüşme hatırlatıcısı gönderim hatası (${call.id}):`, error);
      }
    }
  }
}

