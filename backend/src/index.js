import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { registerUser, loginUser } from './auth.js';
import { authenticateToken } from './middleware/auth.js';
import { rateLimiter } from './middleware/rateLimiter.js';
import { anomalyDetection, getAllSuspiciousActivities } from './middleware/anomalyDetection.js';
import emailService from './services/emailService.js';
import smsService from './services/smsService.js';
import paymentService from './services/paymentService.js';
import connectDB from './db/connection.js';
import Subscription from './models/Subscription.js';
import SubscriptionCode from './models/SubscriptionCode.js';
import DeviceRegistration from './models/DeviceRegistration.js';
import Feedback from './models/Feedback.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// MongoDB bağlantısını başlat
let dbConnected = false;
connectDB().then(() => {
  dbConnected = true;
  console.log('✅ MongoDB bağlantısı başarılı - Backend hazır');
}).catch((error) => {
  console.error('❌ MongoDB bağlantı hatası:', error.message);
  console.warn('⚠️ Backend MongoDB olmadan çalışacak (in-memory fallback)');
});

app.use(cors());
app.use(express.json());

// Anomaly detection middleware (tüm istekler için)
app.use(anomalyDetection);

// Pricing Plans - Tek Ürün, Yıllık Abonelik
const PRICING_PLANS = {
  'yearly': {
    id: 'yearly',
    name: 'Yıllık Abonelik',
    price: 1200, // KDV dahil
    currency: 'TRY',
    interval: 'yearly',
    months: 12,
    monthlyPrice: 100, // Aylık bazda (KDV dahil)
    features: [
      'Sınırsız video görüşme',
      'End-to-end şifreleme',
      'Sunucusuz P2P bağlantı (hiçbir sunucuda tutulmaz)',
      'QR kod ile kolay bağlantı',
      'Rehber entegrasyonu',
      'Sınırsız katılımcı',
      'Sınırsız görüşme geçmişi',
      'Konferans görüşme',
      'Davet/istek gönderme',
      'Kabul/Reddet seçenekleri',
      'Gruplar / Favoriler',
      'Ücretsiz güncellemeler',
      'Ekran paylaşımı',
      'Görüşme içi chat',
      'Dosya paylaşımı',
      'Görüşme kaydı',
      'Görüşme süresi',
      'Sesli arama',
      'Arka plan değiştirme',
      'Görüşme filtreleri',
      'Bildirimler',
      'Görüşme zamanlayıcı',
      'Arama geçmişi',
      'Açık/Koyu tema',
      'Ses ayarları',
      'Bildirim özelleştirme',
      'Görüşme kalitesi ayarları',
      'Şifreleme seviyesi',
      'İki faktörlü doğrulama',
      'Görüşme geçmişi silme',
      'Otomatik temizleme',
      'Gizli mod',
      'Durum mesajı',
      'Profil fotoğrafı',
      'Kişi notları',
      'Görüşme istatistikleri',
      'Grup görüşme yönetimi',
      'Bant genişliği optimizasyonu',
      'Offline mod',
      'Bluetooth desteği',
      'Sesli komutlar',
      'Takvim entegrasyonu',
      'E-posta bildirimleri',
      'SMS bildirimleri',
      'Sosyal medya paylaşımı'
    ],
    valueProposition: 'Özel. Güvenli. Gizli. Görüşmeleriniz hiçbir sunucuda tutulmaz.'
  }
};

// MongoDB kullanılıyor - Map'ler kaldırıldı
// subscriptions -> Subscription model
// subscriptionCodes -> SubscriptionCode model
// deviceRegistrations -> DeviceRegistration model
// feedbacks -> Feedback model

const MAX_DEVICES_PER_SUBSCRIPTION = 2; // Bir abonelik için maksimum cihaz sayısı (telefon yenileme hakkı için)

// MongoDB Helper Functions
async function getSubscriptionById(subscriptionId) {
  if (!dbConnected) return null;
  const sub = await Subscription.findOne({ subscriptionId });
  if (!sub) return null;
  return {
    id: sub.subscriptionId,
    subscriptionId: sub.subscriptionId,
    userId: sub.userId,
    phoneNumber: sub.phoneNumber,
    planId: sub.planId,
    status: sub.status,
    paymentId: sub.paymentId,
    createdAt: sub.createdAt.toISOString(),
    expiresAt: sub.expiresAt.toISOString()
  };
}

async function saveSubscription(subscriptionData) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, subscription kaydedilemedi');
    return null;
  }
  const sub = new Subscription({
    subscriptionId: subscriptionData.id || subscriptionData.subscriptionId,
    userId: subscriptionData.userId,
    phoneNumber: subscriptionData.phoneNumber,
    planId: subscriptionData.planId,
    status: subscriptionData.status,
    paymentId: subscriptionData.paymentId || null,
    createdAt: subscriptionData.createdAt ? new Date(subscriptionData.createdAt) : new Date(),
    expiresAt: new Date(subscriptionData.expiresAt)
  });
  await sub.save();
  return sub;
}

async function updateSubscription(subscriptionId, updateData) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, subscription güncellenemedi');
    return null;
  }
  const sub = await Subscription.findOneAndUpdate(
    { subscriptionId },
    { $set: updateData },
    { new: true }
  );
  return sub;
}

async function getActiveSubscriptions(userId, phoneNumber) {
  if (!dbConnected) return [];
  const query = {
    status: 'active',
    expiresAt: { $gt: new Date() },
    $or: []
  };
  if (userId) query.$or.push({ userId });
  if (phoneNumber) query.$or.push({ phoneNumber });
  if (query.$or.length === 0) return [];
  
  const subs = await Subscription.find(query).sort({ createdAt: -1 });
  return subs.map(sub => ({
    id: sub.subscriptionId,
    subscriptionId: sub.subscriptionId,
    userId: sub.userId,
    phoneNumber: sub.phoneNumber,
    planId: sub.planId,
    status: sub.status,
    paymentId: sub.paymentId,
    createdAt: sub.createdAt.toISOString(),
    expiresAt: sub.expiresAt.toISOString()
  }));
}

async function getAllSubscriptions() {
  if (!dbConnected) return [];
  const subs = await Subscription.find({});
  return subs.map(sub => ({
    id: sub.subscriptionId,
    subscriptionId: sub.subscriptionId,
    userId: sub.userId,
    phoneNumber: sub.phoneNumber,
    planId: sub.planId,
    status: sub.status,
    paymentId: sub.paymentId,
    createdAt: sub.createdAt.toISOString(),
    expiresAt: sub.expiresAt.toISOString()
  }));
}

async function getSubscriptionCode(code) {
  if (!dbConnected) return null;
  const codeData = await SubscriptionCode.findOne({ code: code.toUpperCase() });
  if (!codeData) return null;
  return {
    code: codeData.code,
    phoneNumber: codeData.phoneNumber,
    planId: codeData.planId,
    expiresAt: codeData.expiresAt ? codeData.expiresAt.toISOString() : null,
    used: codeData.used,
    usedAt: codeData.usedAt ? codeData.usedAt.toISOString() : null,
    usedBy: codeData.usedBy,
    deviceType: codeData.deviceType,
    subscriptionId: codeData.subscriptionId,
    createdAt: codeData.createdAt.toISOString()
  };
}

async function saveSubscriptionCode(codeData) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, subscription code kaydedilemedi');
    return null;
  }
  const code = new SubscriptionCode({
    code: codeData.code.toUpperCase(),
    phoneNumber: codeData.phoneNumber || null,
    planId: codeData.planId,
    expiresAt: codeData.expiresAt ? new Date(codeData.expiresAt) : null,
    used: codeData.used || false,
    usedAt: codeData.usedAt ? new Date(codeData.usedAt) : null,
    usedBy: codeData.usedBy || null,
    deviceType: codeData.deviceType || null,
    subscriptionId: codeData.subscriptionId || null,
    createdAt: codeData.createdAt ? new Date(codeData.createdAt) : new Date()
  });
  await code.save();
  return code;
}

async function updateSubscriptionCode(code, updateData) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, subscription code güncellenemedi');
    return null;
  }
  const codeData = await SubscriptionCode.findOneAndUpdate(
    { code: code.toUpperCase() },
    { $set: updateData },
    { new: true }
  );
  return codeData;
}

async function getDeviceRegistrations(phoneNumber) {
  if (!dbConnected) return [];
  const devices = await DeviceRegistration.find({ phoneNumber });
  return devices.map(dev => dev.deviceId);
}

async function saveDeviceRegistration(phoneNumber, deviceId) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, device registration kaydedilemedi');
    return null;
  }
  const device = new DeviceRegistration({
    phoneNumber,
    deviceId,
    registeredAt: new Date(),
    lastSeen: new Date()
  });
  await device.save();
  return device;
}

async function updateDeviceLastSeen(phoneNumber, deviceId) {
  if (!dbConnected) return null;
  await DeviceRegistration.findOneAndUpdate(
    { phoneNumber, deviceId },
    { $set: { lastSeen: new Date() } }
  );
}

async function deleteDeviceRegistration(phoneNumber, deviceId) {
  if (!dbConnected) return null;
  await DeviceRegistration.deleteOne({ phoneNumber, deviceId });
}

async function saveFeedback(feedbackData) {
  if (!dbConnected) {
    console.warn('⚠️ MongoDB bağlantısı yok, feedback kaydedilemedi');
    return null;
  }
  const feedback = new Feedback({
    userId: feedbackData.userId || null,
    email: feedbackData.email || null,
    phoneNumber: feedbackData.phoneNumber || null,
    type: feedbackData.type,
    subject: feedbackData.subject,
    message: feedbackData.message,
    rating: feedbackData.rating || null,
    createdAt: new Date()
  });
  await feedback.save();
  return feedback;
}

// Signaling Server IP Storage
// Otomatik IP bulma için signaling server'ın IP'sini sakla
// Her signaling server başladığında kendi IP'sini buraya kaydeder
const signalingServerInfo = {
  wsUrl: null, // ws://IP:PORT/ws
  httpUrl: null, // http://IP:PORT
  lastUpdated: null,
  localIP: null, // Local network IP (192.168.x.x)
  publicIP: null // Public IP (internet üzerinden erişilebilir)
};

// Presence storage (kullanıcı durum bilgileri)
// userId/phoneNumber -> { status, customMessage, lastSeen, lastUpdated }
const userPresence = new Map();
const PRESENCE_STATUSES = {
  ONLINE: 'ONLINE',
  BUSY: 'BUSY',
  DO_NOT_DISTURB: 'DO_NOT_DISTURB',
  INVISIBLE: 'INVISIBLE'
};
const PRESENCE_TIMEOUT = 5 * 60 * 1000; // 5 dakika - son görülme zamanından sonra offline sayılır

// Auth Routes (Rate limiting ile)
app.post('/api/auth/register', rateLimiter('auth'), async (req, res) => {
  try {
    const { name, email, phone, password } = req.body;

    if (!name || !email || !phone || !password) {
      return res.status(400).json({
        success: false,
        error: 'Tüm alanlar zorunludur.'
      });
    }

    const user = await registerUser({ name, email, phone, password });
    const { password: _, ...userWithoutPassword } = user;

    // Hoş geldin e-postası gönder (async, hata olsa bile devam et)
    emailService.sendEmail({
      to: email,
      subject: 'Video Call\'a Hoş Geldiniz!',
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: linear-gradient(135deg, #00BCD4, #0097A7); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
            .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
            .button { display: inline-block; padding: 12px 24px; background: #00BCD4; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
            .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Hoş Geldiniz!</h1>
            </div>
            <div class="content">
              <p>Merhaba ${name},</p>
              <p>Video Call hesabınız başarıyla oluşturuldu.</p>
              <p>Özel, güvenli ve gizli görüşmeleriniz için abonelik başlatabilirsiniz.</p>
              <a href="${process.env.WEB_URL || 'https://videocall.app'}/pricing" class="button">Abonelik Başlat</a>
              <p>Teşekkürler,<br>Video Call Ekibi</p>
            </div>
            <div class="footer">
              <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
            </div>
          </div>
        </body>
        </html>
      `
    }).catch(err => console.error('E-posta gönderim hatası:', err));

    res.status(201).json({
      success: true,
      message: 'Hesap başarıyla oluşturuldu.',
      user: userWithoutPassword
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      error: error.message
    });
  }
});

app.post('/api/auth/login', rateLimiter('auth'), async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        error: 'E-posta ve şifre zorunludur.'
      });
    }

    const result = await loginUser(email, password);

    res.json({
      success: true,
      message: 'Giriş başarılı.',
      ...result
    });
  } catch (error) {
    res.status(401).json({
      success: false,
      error: error.message
    });
  }
});

// Kullanıcı bilgilerini getir (protected)
app.get('/api/auth/me', authenticateToken, (req, res) => {
  const { password: _, ...userWithoutPassword } = req.user;
  res.json({
    success: true,
    user: userWithoutPassword
  });
});

// API Routes
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.get('/api/pricing', (req, res) => {
  res.json({
    success: true,
    plans: PRICING_PLANS
  });
});

app.get('/api/pricing/:planId', (req, res) => {
  const { planId } = req.params;
  const plan = PRICING_PLANS[planId];
  
  if (!plan) {
    return res.status(404).json({
      success: false,
      error: 'Plan bulunamadı'
    });
  }
  
  res.json({
    success: true,
    plan
  });
});

// Subscription Management (Protected - giriş yapmış kullanıcılar için)
app.post('/api/subscribe', authenticateToken, rateLimiter('subscription'), async (req, res) => {
  try {
    const { planId, paymentMethod, paymentCard, billingAddress } = req.body;
    const userId = req.user.id;
    
    if (!planId || !PRICING_PLANS[planId]) {
      return res.status(400).json({
        success: false,
        error: 'Geçersiz plan ID'
      });
    }
    
    const plan = PRICING_PLANS[planId];
    const subscriptionId = `sub_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const now = new Date();
    const expiresAt = new Date(now);
    const months = plan.months || 12;
    expiresAt.setMonth(expiresAt.getMonth() + months);
    
    // Ödeme kartı bilgileri varsa iyzico ile ödeme oluştur
    if (paymentCard && paymentService.isEnabled) {
      try {
        const paymentData = await paymentService.createSubscriptionPayment(
          {
            userId: userId,
            email: req.user.email,
            name: req.user.name,
            phone: req.user.phone,
            address: billingAddress?.address || '',
            city: billingAddress?.city || 'Istanbul',
            zipCode: billingAddress?.zipCode || '34000'
          },
          {
            planId: planId,
            planName: plan.name,
            price: plan.price,
            currency: 'TRY'
          }
        );

        // iyzico ödeme isteği oluştur
        const iyzicoPayment = await paymentService.createPayment({
          price: plan.price,
          currency: 'TRY',
          installment: 1,
          paymentCard: paymentCard,
          buyer: paymentData.buyer,
          billingAddress: paymentData.billingAddress,
          basketItems: paymentData.basketItems,
          callbackUrl: `${process.env.WEB_URL || 'https://videocall.app'}/payment/callback?subscriptionId=${subscriptionId}`
        });

        // 3D Secure yönlendirmesi gerekiyorsa
        if (iyzicoPayment.status === 'success' && iyzicoPayment.threeDSHtmlContent) {
          // Ödeme bekliyor durumunda kaydet
          const subscription = {
            id: subscriptionId,
            userId: userId,
            phoneNumber: req.user.phone,
            planId,
            status: 'pending', // Ödeme bekliyor
            createdAt: now.toISOString(),
            expiresAt: expiresAt.toISOString(),
            paymentMethod: paymentMethod || 'card',
            paymentId: iyzicoPayment.paymentId,
            conversationId: iyzicoPayment.conversationId
          };
          
          await saveSubscription(subscription);
          
          return res.json({
            success: true,
            requires3DS: true,
            threeDSHtmlContent: iyzicoPayment.threeDSHtmlContent,
            subscriptionId: subscriptionId,
            message: '3D Secure doğrulaması gerekiyor.'
          });
        }

        // Ödeme başarılı
        if (iyzicoPayment.status === 'success') {
          const subscription = {
            id: subscriptionId,
            userId: userId,
            phoneNumber: req.user.phone,
            planId,
            status: 'active',
            createdAt: now.toISOString(),
            expiresAt: expiresAt.toISOString(),
            paymentMethod: paymentMethod || 'card',
            paymentId: iyzicoPayment.paymentId,
            conversationId: iyzicoPayment.conversationId
          };
          
          await saveSubscription(subscription);
          req.user.subscription = subscriptionId;

          // Bildirimler
          emailService.sendSubscriptionConfirmation(req.user.email, {
            planName: plan.name,
            startDate: now,
            endDate: expiresAt,
            price: plan.price
          }).catch(err => console.error('E-posta gönderim hatası:', err));

          if (req.user.phone) {
            smsService.sendSMS(
              req.user.phone,
              `Video Call: Aboneliğiniz başarıyla oluşturuldu! ${plan.name} - ${plan.price} TRY. Bitiş: ${expiresAt.toLocaleDateString('tr-TR')}`
            ).catch(err => console.error('SMS gönderim hatası:', err));
          }

          return res.json({
            success: true,
            subscription,
            message: 'Abonelik başarıyla oluşturuldu.'
          });
        } else {
          // Ödeme başarısız
          return res.status(400).json({
            success: false,
            error: iyzicoPayment.errorMessage || 'Ödeme işlemi başarısız oldu.'
          });
        }
      } catch (paymentError) {
        console.error('Ödeme hatası:', paymentError);
        return res.status(400).json({
          success: false,
          error: paymentError.response?.data?.errorMessage || paymentError.message || 'Ödeme işlemi sırasında bir hata oluştu.'
        });
      }
    }
    
    // Ödeme kartı yoksa veya iyzico devre dışıysa (test için)
    const subscription = {
      id: subscriptionId,
      userId: userId,
      phoneNumber: req.user.phone,
      planId,
      status: 'pending', // Ödeme bekliyor
      createdAt: now.toISOString(),
      expiresAt: expiresAt.toISOString(),
      paymentMethod: paymentMethod || 'card'
    };
    
    await saveSubscription(subscription);
    req.user.subscription = subscriptionId;
    
    res.json({
      success: true,
      subscription,
      message: 'Abonelik oluşturuldu. Ödeme işlemi tamamlandığında aktif olacaktır.'
    });
  } catch (error) {
    console.error('Abonelik oluşturma hatası:', error);
    res.status(500).json({
      success: false,
      error: 'Abonelik oluşturulurken bir hata oluştu.'
    });
  }
});

// 3D Secure callback endpoint
app.post('/api/payment/callback', async (req, res) => {
  try {
    const { subscriptionId } = req.query;
    const callbackData = req.body;

    if (!subscriptionId) {
      return res.status(400).json({
        success: false,
        error: 'Subscription ID gerekli'
      });
    }

    // iyzico 3D callback işle
    const result = await paymentService.handle3DCallback(callbackData);

    if (result.status === 'success') {
      // Aboneliği aktif et
      const subscription = await getSubscriptionById(subscriptionId);
      if (subscription) {
        await updateSubscription(subscriptionId, {
          status: 'active',
          paymentId: result.paymentId
        });

        // Bildirimler
        // TODO: Kullanıcı bilgilerini al ve bildirim gönder
      }

      // Frontend'e yönlendir
      return res.redirect(`${process.env.WEB_URL || 'https://videocall.app'}/payment/success?subscriptionId=${subscriptionId}`);
    } else {
      // Ödeme başarısız
      return res.redirect(`${process.env.WEB_URL || 'https://videocall.app'}/payment/failed?error=${encodeURIComponent(result.errorMessage || 'Ödeme başarısız')}`);
    }
  } catch (error) {
    console.error('3D callback hatası:', error);
    return res.redirect(`${process.env.WEB_URL || 'https://videocall.app'}/payment/failed?error=${encodeURIComponent('Ödeme işlemi sırasında bir hata oluştu')}`);
  }
});

// Ödeme sorgulama endpoint
app.get('/api/payment/:paymentId', authenticateToken, async (req, res) => {
  try {
    const { paymentId } = req.params;
    const payment = await paymentService.retrievePayment(paymentId);
    
    res.json({
      success: true,
      payment
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      error: error.message || 'Ödeme sorgulanamadı'
    });
  }
});

// Check subscription status (Protected - kendi aboneliğini kontrol eder)
app.get('/api/subscription/me', authenticateToken, async (req, res) => {
  const userId = req.user.id;
  const phoneNumber = req.user.phone;
  
  // Kullanıcının aktif aboneliğini bul (userId veya phoneNumber ile)
  const activeSubscriptions = await getActiveSubscriptions(userId, phoneNumber);
  const activeSubscription = activeSubscriptions.length > 0 ? activeSubscriptions[0] : null;
  
  if (!activeSubscription) {
    // Abonelik yoksa veya süresi dolmuşsa
    return res.json({
      success: true,
      data: {
        planId: null,
        status: 'expired',
        isPremium: false,
        deviceType: null
      },
      plan: null,
      message: 'Aktif abonelik bulunamadı. Lütfen yeni abonelik oluşturun.'
    });
  }
  
  const plan = PRICING_PLANS[activeSubscription.planId];
  
  res.json({
    success: true,
    data: {
      ...activeSubscription,
      isPremium: true
    },
    plan
  });
});

// Subscription endpoint (alias for /me)
app.get('/api/subscription', authenticateToken, async (req, res) => {
  // /subscription/me endpoint'ine yönlendir
  const userId = req.user.id;
  const phoneNumber = req.user.phone;
  
  const activeSubscriptions = await getActiveSubscriptions(userId, phoneNumber);
  const activeSubscription = activeSubscriptions.length > 0 ? activeSubscriptions[0] : null;
  
  if (!activeSubscription) {
    return res.json({
      success: true,
      data: {
        planId: null,
        status: 'expired',
        isPremium: false,
        deviceType: null
      },
      plan: null
    });
  }
  
  const plan = PRICING_PLANS[activeSubscription.planId];
  
  res.json({
    success: true,
    data: {
      ...activeSubscription,
      isPremium: true
    },
    plan
  });
});

// Cancel subscription (abonelik bitince otomatik freemium'a düşer)
app.post('/api/subscription/:subscriptionId/cancel', async (req, res) => {
  const { subscriptionId } = req.params;
  const subscription = await getSubscriptionById(subscriptionId);
  
  if (!subscription) {
    return res.status(404).json({
      success: false,
      error: 'Abonelik bulunamadı'
    });
  }
  
  await updateSubscription(subscriptionId, { status: 'cancelled' });
  
  res.json({
    success: true,
    message: 'Abonelik iptal edildi. Süre sonuna kadar premium özellikler kullanılabilir.'
  });
});

// Abonelik kodu aktivasyonu (mobil ve desktop için)
app.post('/api/subscription/activate-code', rateLimiter('subscription'), async (req, res) => {
  try {
    const { code, phoneNumber, deviceType } = req.body; // deviceType: 'mobile' | 'desktop'
    
    if (!code || !phoneNumber) {
      return res.status(400).json({
        success: false,
        error: 'Abonelik kodu ve telefon numarası gereklidir.'
      });
    }
    
    // Abonelik kodunu bul
    const codeData = await getSubscriptionCode(code);
    
    if (!codeData) {
      return res.status(404).json({
        success: false,
        error: 'Geçersiz abonelik kodu.'
      });
    }
    
    // Kod daha önce kullanılmış mı?
    if (codeData.used) {
      // Aynı telefon numarası ve cihaz tipi için tekrar kullanılabilir mi kontrol et
      if (codeData.usedBy !== phoneNumber || codeData.deviceType !== deviceType) {
        return res.status(403).json({
          success: false,
          error: 'Bu abonelik kodu daha önce kullanılmış.'
        });
      }
      // Aynı kullanıcı ve cihaz tipi için yeniden aktivasyon (abonelik süresi uzatılabilir)
    }
    
    // Telefon numarası eşleşiyor mu?
    if (codeData.phoneNumber && codeData.phoneNumber !== phoneNumber) {
      return res.status(403).json({
        success: false,
        error: 'Bu abonelik kodu farklı bir telefon numarası için geçerli.'
      });
    }
    
    // Kod süresi dolmuş mu?
    if (codeData.expiresAt && new Date(codeData.expiresAt) < new Date()) {
      return res.status(403).json({
        success: false,
        error: 'Bu abonelik kodunun süresi dolmuş.'
      });
    }
    
    // Abonelik oluştur
    const subscriptionId = `sub_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const now = new Date();
    const expiresAt = new Date(now);
    expiresAt.setFullYear(expiresAt.getFullYear() + 1); // Yıllık abonelik
    
    const subscription = {
      id: subscriptionId,
      userId: phoneNumber, // Telefon numarası userId olarak kullanılıyor
      phoneNumber: phoneNumber,
      planId: codeData.planId || 'yearly',
      status: 'active',
      createdAt: now.toISOString(),
      expiresAt: expiresAt.toISOString(),
      paymentMethod: 'code',
      activationCode: code.toUpperCase(),
      deviceType: deviceType || 'mobile'
    };
    
    await saveSubscription(subscription);
    
    // Kodu kullanıldı olarak işaretle (ama aynı kullanıcı için tekrar kullanılabilir)
    await updateSubscriptionCode(code, {
      used: true,
      usedAt: new Date(),
      usedBy: phoneNumber,
      deviceType: deviceType || 'mobile',
      subscriptionId: subscriptionId
    });
    
    res.json({
      success: true,
      subscription,
      message: 'Abonelik kodu başarıyla aktifleştirildi.',
      expiresAt: expiresAt.toISOString()
    });
  } catch (error) {
    console.error('Abonelik kodu aktivasyon hatası:', error);
    res.status(500).json({
      success: false,
      error: 'Abonelik kodu aktivasyonu sırasında bir hata oluştu.'
    });
  }
});

// Abonelik kodu oluşturma (admin için - test amaçlı)
// Production'da bu endpoint admin authentication ile korunmalı
app.post('/api/admin/subscription-codes/generate', async (req, res) => {
  try {
    const { phoneNumber, planId = 'yearly', count = 1 } = req.body;
    
    const generatedCodes = [];
    for (let i = 0; i < count; i++) {
      // Kod formatı: VC-XXXX-XXXX-XXXX (12 karakter, 4'lü gruplar)
      const code = `VC-${Math.random().toString(36).substring(2, 6).toUpperCase()}-${Math.random().toString(36).substring(2, 6).toUpperCase()}-${Math.random().toString(36).substring(2, 6).toUpperCase()}`;
      
      const codeData = {
        code: code,
        phoneNumber: phoneNumber || null, // null ise herhangi bir telefon numarası için kullanılabilir
        planId: planId,
        expiresAt: null, // Kod süresi sınırsız (veya belirli bir tarih)
        used: false,
        usedAt: null,
        usedBy: null,
        deviceType: null,
        subscriptionId: null,
        createdAt: new Date().toISOString()
      };
      
      await saveSubscriptionCode(codeData);
      generatedCodes.push(code);
    }
    
    res.json({
      success: true,
      codes: generatedCodes,
      message: `${count} abonelik kodu oluşturuldu.`
    });
  } catch (error) {
    console.error('Abonelik kodu oluşturma hatası:', error);
    res.status(500).json({
      success: false,
      error: 'Abonelik kodu oluşturulurken bir hata oluştu.'
    });
  }
});

// Device Registration & Verification (APK paylaşımını önlemek için)
// Cihaz kaydı: Telefon numarası + cihaz ID eşleşmesi
app.post('/api/devices/register', rateLimiter('device'), async (req, res) => {
  const { phoneNumber, deviceId } = req.body;
  
  if (!phoneNumber || !deviceId) {
    return res.status(400).json({
      success: false,
      error: 'Telefon numarası ve cihaz ID gereklidir.'
    });
  }
  
  // Abonelik kontrolü - telefon numarasına göre kullanıcı bul
  // TODO: Production'da users Map'inde telefon numarası ile eşleştirme yapılmalı
  // Şimdilik: Web'den gelen kullanıcılar email ile kayıtlı, mobil kullanıcılar telefon numarası ile
  // Geçici çözüm: Telefon numarası ile abonelik kontrolü yapılmıyor, sadece cihaz kaydı yapılıyor
  // Production'da: users Map'inde telefon numarası ile kullanıcı bulunmalı ve abonelik kontrol edilmeli
  
  // Şimdilik: Cihaz kaydı yapılabilir (abonelik kontrolü sonra eklenecek)
  // Not: Web'den abonelik oluşturulduğunda telefon numarası da kaydedilmeli
  
  // Mevcut cihazları al
  const registeredDevices = await getDeviceRegistrations(phoneNumber);
  
  // Cihaz zaten kayıtlı mı?
  if (registeredDevices.includes(deviceId)) {
    await updateDeviceLastSeen(phoneNumber, deviceId);
    return res.json({
      success: true,
      message: 'Cihaz zaten kayıtlı.'
    });
  }
  
  // Cihaz limiti kontrolü
  if (registeredDevices.length >= MAX_DEVICES_PER_SUBSCRIPTION) {
    return res.status(403).json({
      success: false,
      error: `Maksimum ${MAX_DEVICES_PER_SUBSCRIPTION} cihaz kaydedilebilir. Lütfen eski bir cihazı kaldırın.`
    });
  }
  
  // Yeni cihazı kaydet
  await saveDeviceRegistration(phoneNumber, deviceId);
  
  const newDeviceCount = registeredDevices.length + 1;
  res.json({
    success: true,
    message: 'Cihaz başarıyla kaydedildi.',
    deviceCount: newDeviceCount,
    maxDevices: MAX_DEVICES_PER_SUBSCRIPTION
  });
});

// Cihaz doğrulama
app.post('/api/devices/verify', rateLimiter('device'), async (req, res) => {
  const { phoneNumber, deviceId } = req.body;
  
  if (!phoneNumber || !deviceId) {
    return res.status(400).json({
      success: false,
      error: 'Telefon numarası ve cihaz ID gereklidir.'
    });
  }
  
  // Abonelik kontrolü
  let hasActiveSubscription = false;
  const allSubscriptions = await getAllSubscriptions();
  for (const sub of allSubscriptions) {
    if (sub.status === 'active' && new Date(sub.expiresAt) > new Date()) {
      hasActiveSubscription = true;
      break;
    }
  }
  
  if (!hasActiveSubscription) {
    return res.status(403).json({
      success: false,
      error: 'Aktif abonelik bulunamadı.'
    });
  }
  
  // Cihaz kayıtlı mı?
  const registeredDevices = await getDeviceRegistrations(phoneNumber);
  
  if (!registeredDevices.includes(deviceId)) {
    return res.status(403).json({
      success: false,
      error: 'Bu cihaz kayıtlı değil. Lütfen cihazı kaydedin.'
    });
  }
  
  // Last seen güncelle
  await updateDeviceLastSeen(phoneNumber, deviceId);
  
  res.json({
    success: true,
    message: 'Cihaz yetkili.'
  });
});

// Kullanıcının kayıtlı cihazlarını listele
app.get('/api/devices/:phoneNumber', async (req, res) => {
  const { phoneNumber } = req.params;
  const devices = await DeviceRegistration.find({ phoneNumber }).sort({ registeredAt: -1 });
  
  res.json({
    success: true,
    devices: devices.map((device, index) => ({
      id: device.deviceId,
      index: index + 1,
      registeredAt: device.registeredAt.toISOString(),
      lastSeen: device.lastSeen.toISOString()
    })),
    count: devices.length,
    maxDevices: MAX_DEVICES_PER_SUBSCRIPTION
  });
});

// Cihaz kaydını kaldır
app.delete('/api/devices/:phoneNumber/:deviceId', async (req, res) => {
  const { phoneNumber, deviceId } = req.params;
  
  const device = await DeviceRegistration.findOne({ phoneNumber, deviceId });
  if (!device) {
    return res.status(404).json({
      success: false,
      error: 'Cihaz bulunamadı.'
    });
  }
  
  await deleteDeviceRegistration(phoneNumber, deviceId);
  
  res.json({
    success: true,
    message: 'Cihaz kaydı kaldırıldı.'
  });
});

// Signaling Server IP Management - Otomatik IP bulma için
// Signaling server başladığında kendi IP'sini kaydeder
app.post('/api/signaling/register', rateLimiter('signaling'), (req, res) => {
  const { wsUrl, httpUrl, localIP, publicIP } = req.body;
  
  if (!wsUrl || !httpUrl) {
    return res.status(400).json({
      success: false,
      error: 'wsUrl ve httpUrl gereklidir.'
    });
  }
  
  signalingServerInfo.wsUrl = wsUrl;
  signalingServerInfo.httpUrl = httpUrl;
  signalingServerInfo.localIP = localIP || null;
  signalingServerInfo.publicIP = publicIP || null;
  signalingServerInfo.lastUpdated = new Date();
  
  console.log(`[Backend] Signaling server kaydedildi: ${wsUrl}`);
  
  res.json({
    success: true,
    message: 'Signaling server kaydedildi.',
    serverInfo: {
      wsUrl: signalingServerInfo.wsUrl,
      httpUrl: signalingServerInfo.httpUrl,
      localIP: signalingServerInfo.localIP,
      publicIP: signalingServerInfo.publicIP,
      lastUpdated: signalingServerInfo.lastUpdated
    }
  });
});

// Signaling server IP'sini al (otomatik IP bulma için)
app.get('/api/signaling/server-info', rateLimiter('signaling'), (req, res) => {
  if (!signalingServerInfo.wsUrl) {
    return res.status(404).json({
      success: false,
      error: 'Signaling server kayıtlı değil. Lütfen signaling server\'ı başlatın.'
    });
  }
  
  // Son güncelleme 5 dakikadan eskiyse, server offline sayılabilir
  const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000);
  if (signalingServerInfo.lastUpdated < fiveMinutesAgo) {
    return res.status(503).json({
      success: false,
      error: 'Signaling server son 5 dakikada güncellenmedi. Server offline olabilir.',
      serverInfo: {
        wsUrl: signalingServerInfo.wsUrl,
        httpUrl: signalingServerInfo.httpUrl,
        lastUpdated: signalingServerInfo.lastUpdated
      }
    });
  }
  
  res.json({
    success: true,
    serverInfo: {
      wsUrl: signalingServerInfo.wsUrl,
      httpUrl: signalingServerInfo.httpUrl,
      localIP: signalingServerInfo.localIP,
      publicIP: signalingServerInfo.publicIP,
      lastUpdated: signalingServerInfo.lastUpdated
    }
  });
});

// Presence API - Kullanıcı durum yönetimi
// Kendi durumunu al
app.get('/api/presence/me', authenticateToken, (req, res) => {
  const userId = req.user.id;
  const phoneNumber = req.user.phone;
  const identifier = userId || phoneNumber;
  
  const presence = userPresence.get(identifier);
  
  if (!presence) {
    // Varsayılan durum
    return res.json({
      success: true,
      presence: {
        status: PRESENCE_STATUSES.ONLINE,
        customMessage: null,
        lastSeen: new Date().toISOString(),
        lastUpdated: new Date().toISOString()
      }
    });
  }
  
  // Son görülme zamanı kontrolü (5 dakika)
  const lastSeen = new Date(presence.lastSeen);
  const now = new Date();
  const timeSinceLastSeen = now.getTime() - lastSeen.getTime();
  
  let actualStatus = presence.status;
  if (timeSinceLastSeen > PRESENCE_TIMEOUT && presence.status !== PRESENCE_STATUSES.INVISIBLE) {
    // 5 dakikadan fazla süredir görülmemişse offline sayılır (INVISIBLE hariç)
    actualStatus = 'OFFLINE';
  }
  
  res.json({
    success: true,
    presence: {
      ...presence,
      status: actualStatus
    }
  });
});

// Kendi durumunu güncelle
app.put('/api/presence/me', authenticateToken, (req, res) => {
  const userId = req.user.id;
  const phoneNumber = req.user.phone;
  const identifier = userId || phoneNumber;
  const { status, customMessage } = req.body;
  
  // Status validasyonu
  if (status && !Object.values(PRESENCE_STATUSES).includes(status)) {
    return res.status(400).json({
      success: false,
      error: 'Geçersiz durum. Geçerli durumlar: ONLINE, BUSY, DO_NOT_DISTURB, INVISIBLE'
    });
  }
  
  const now = new Date();
  const currentPresence = userPresence.get(identifier) || {};
  
  const updatedPresence = {
    status: status || currentPresence.status || PRESENCE_STATUSES.ONLINE,
    customMessage: customMessage !== undefined ? customMessage : currentPresence.customMessage,
    lastSeen: now.toISOString(),
    lastUpdated: now.toISOString()
  };
  
  userPresence.set(identifier, updatedPresence);
  
  res.json({
    success: true,
    presence: updatedPresence,
    message: 'Durum başarıyla güncellendi'
  });
});

// Belirli bir kullanıcının durumunu al (sadece contact'lar için)
app.get('/api/presence/:phoneNumber', authenticateToken, (req, res) => {
  const { phoneNumber } = req.params;
  const requestingUserId = req.user.id;
  const requestingPhoneNumber = req.user.phone;
  
  if (!phoneNumber) {
    return res.status(400).json({
      success: false,
      error: 'Telefon numarası gereklidir'
    });
  }
  
  const presence = userPresence.get(phoneNumber);
  
  if (!presence) {
    // Kullanıcı durumu kayıtlı değilse varsayılan değerler
    return res.json({
      success: true,
      presence: {
        status: 'OFFLINE',
        customMessage: null,
        lastSeen: null,
        lastUpdated: null
      }
    });
  }
  
  // INVISIBLE durumunda sadece OFFLINE göster
  let visibleStatus = presence.status;
  if (presence.status === PRESENCE_STATUSES.INVISIBLE) {
    visibleStatus = 'OFFLINE';
  }
  
  // Son görülme zamanı kontrolü
  const lastSeen = new Date(presence.lastSeen);
  const now = new Date();
  const timeSinceLastSeen = now.getTime() - lastSeen.getTime();
  
  if (timeSinceLastSeen > PRESENCE_TIMEOUT && visibleStatus !== 'OFFLINE') {
    visibleStatus = 'OFFLINE';
  }
  
  res.json({
    success: true,
    presence: {
      status: visibleStatus,
      customMessage: presence.customMessage,
      lastSeen: presence.lastSeen,
      lastUpdated: presence.lastUpdated
    }
  });
});

// Birden fazla kullanıcının durumunu al (contact listesi için)
app.post('/api/presence/batch', authenticateToken, (req, res) => {
  const { phoneNumbers } = req.body;
  
  if (!Array.isArray(phoneNumbers) || phoneNumbers.length === 0) {
    return res.status(400).json({
      success: false,
      error: 'Telefon numaraları listesi gereklidir'
    });
  }
  
  // Maksimum 100 telefon numarası
  if (phoneNumbers.length > 100) {
    return res.status(400).json({
      success: false,
      error: 'Maksimum 100 telefon numarası sorgulanabilir'
    });
  }
  
  const now = new Date();
  const presences = {};
  
  phoneNumbers.forEach(phoneNumber => {
    const presence = userPresence.get(phoneNumber);
    
    if (!presence) {
      presences[phoneNumber] = {
        status: 'OFFLINE',
        customMessage: null,
        lastSeen: null,
        lastUpdated: null
      };
      return;
    }
    
    // INVISIBLE durumunda sadece OFFLINE göster
    let visibleStatus = presence.status;
    if (presence.status === PRESENCE_STATUSES.INVISIBLE) {
      visibleStatus = 'OFFLINE';
    }
    
    // Son görülme zamanı kontrolü
    const lastSeen = new Date(presence.lastSeen);
    const timeSinceLastSeen = now.getTime() - lastSeen.getTime();
    
    if (timeSinceLastSeen > PRESENCE_TIMEOUT && visibleStatus !== 'OFFLINE') {
      visibleStatus = 'OFFLINE';
    }
    
    presences[phoneNumber] = {
      status: visibleStatus,
      customMessage: presence.customMessage,
      lastSeen: presence.lastSeen,
      lastUpdated: presence.lastUpdated
    };
  });
  
  res.json({
    success: true,
    presences
  });
});

// Heartbeat - Kullanıcının aktif olduğunu bildirir
app.post('/api/presence/heartbeat', authenticateToken, (req, res) => {
  const userId = req.user.id;
  const phoneNumber = req.user.phone;
  const identifier = userId || phoneNumber;
  
  const now = new Date();
  const currentPresence = userPresence.get(identifier);
  
  // Mevcut durumu koru, sadece lastSeen'i güncelle
  const updatedPresence = {
    status: currentPresence?.status || PRESENCE_STATUSES.ONLINE,
    customMessage: currentPresence?.customMessage || null,
    lastSeen: now.toISOString(),
    lastUpdated: currentPresence?.lastUpdated || now.toISOString()
  };
  
  userPresence.set(identifier, updatedPresence);
  
  res.json({
    success: true,
    message: 'Heartbeat kaydedildi'
  });
});

// Feedback ortalama puan endpoint'i
app.get('/api/feedback/average', async (req, res) => {
  try {
    const allFeedbacks = await Feedback.find({ rating: { $ne: null } });
    if (allFeedbacks.length === 0) {
      return res.json({
        success: true,
        average: 0,
        count: 0
      });
    }

    const totalRating = allFeedbacks.reduce((sum, feedback) => sum + (feedback.rating || 0), 0);
    const average = totalRating / allFeedbacks.length;
    const roundedAverage = Math.round(average * 10) / 10; // 1 ondalık basamak

    res.json({
      success: true,
      average: roundedAverage,
      count: allFeedbacks.length
    });
  } catch (error) {
    console.error('Feedback average endpoint hatası:', error);
    res.status(500).json({
      success: false,
      error: 'Ortalama puan hesaplanırken bir hata oluştu'
    });
  }
});

// Feedback endpoint
app.post('/api/feedback', rateLimiter('feedback'), async (req, res) => {
  try {
    const { rating, name, email, comment } = req.body;

    // Validation
    if (!rating || rating < 1 || rating > 5) {
      return res.status(400).json({
        success: false,
        error: 'Geçerli bir puan seçin (1-5)'
      });
    }

    if (!name || name.trim().length < 2) {
      return res.status(400).json({
        success: false,
        error: 'Ad Soyad en az 2 karakter olmalıdır'
      });
    }

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({
        success: false,
        error: 'Geçerli bir e-posta adresi girin'
      });
    }

    if (!comment || comment.trim().length < 10) {
      return res.status(400).json({
        success: false,
        error: 'Yorum en az 10 karakter olmalıdır'
      });
    }

    // Feedback verilerini kaydet (production'da veritabanına kaydedilebilir)
    const feedbackData = {
      rating: parseInt(rating),
      name: name.trim(),
      email: email.trim().toLowerCase(),
      comment: comment.trim(),
      timestamp: new Date().toISOString(),
      ip: req.ip || req.connection.remoteAddress
    };

    // Memory'de sakla (production'da veritabanına kaydedilecek)
    await saveFeedback(feedbackData);

    // Console'a log
    console.log('📝 Yeni geri bildirim:', feedbackData);

    // TODO: Production'da veritabanına kaydet
    // await db.feedbacks.insert(feedbackData);

    // Opsiyonel: E-posta bildirimi gönder (admin'e)
    try {
      await emailService.sendFeedbackNotification({
        rating: feedbackData.rating,
        name: feedbackData.name,
        email: feedbackData.email,
        comment: feedbackData.comment
      });
    } catch (emailError) {
      console.error('E-posta bildirimi gönderilemedi:', emailError);
      // E-posta hatası feedback kaydını engellemez
    }

    res.json({
      success: true,
      message: 'Geri bildiriminiz başarıyla gönderildi. Teşekkür ederiz!'
    });

  } catch (error) {
    console.error('Feedback endpoint hatası:', error);
    res.status(500).json({
      success: false,
      error: 'Geri bildirim gönderilirken bir hata oluştu'
    });
  }
});

// Anomaly detection endpoint (admin için - production'da korumalı olmalı)
app.get('/api/admin/suspicious-activities', authenticateToken, (req, res) => {
  // TODO: Production'da admin rol kontrolü ekle
  const activities = getAllSuspiciousActivities();
  res.json({
    success: true,
    count: activities.length,
    activities: activities.slice(0, 100) // Son 100 aktivite
  });
});

// Bildirim görevlerini başlat (her 1 saatte bir kontrol et)
// Production'da node-cron veya benzeri bir kütüphane kullanılabilir
let notificationInterval = null;

function startNotificationJobs() {
  // Her 1 saatte bir kontrol et
  notificationInterval = setInterval(async () => {
    try {
      const { checkAndSendRenewalReminders } = await import('./jobs/notificationJobs.js');
      const { getUserById } = await import('./auth.js');
      
      // getUserByEmail fonksiyonu (şimdilik getUserById kullanıyoruz)
      const getUserByEmail = async (userId) => {
        return getUserById(userId);
      };

      const allSubscriptions = await getAllSubscriptions();
      await checkAndSendRenewalReminders(allSubscriptions, getUserByEmail);
      console.log('Bildirim görevleri çalıştırıldı:', new Date().toISOString());
    } catch (error) {
      console.error('Bildirim görevleri hatası:', error);
    }
  }, 60 * 60 * 1000); // 1 saat

  console.log('📧 Bildirim görevleri başlatıldı (her 1 saatte bir)');
}

app.listen(PORT, () => {
  console.log(`🚀 Backend server running on http://localhost:${PORT}`);
  console.log(`🔒 Security features: Rate Limiting, Anomaly Detection`);
  
  // E-posta servisini başlat
  emailService.initialize().catch(err => {
    console.error('E-posta servisi başlatılamadı:', err);
  });
  
  // Bildirim görevlerini başlat
  startNotificationJobs();
});

