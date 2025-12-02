/**
 * Firebase Cloud Messaging (FCM) Push Notification Service
 * 
 * Bu servis offline kullanıcılara push notification gönderir.
 */

import { createRequire } from 'module';
const require = createRequire(import.meta.url);

// FCM Token Store: phoneNumber -> fcmToken
const fcmTokens = new Map();

// Firebase Admin SDK (eğer yüklüyse)
let admin = null;
try {
    admin = require('firebase-admin');
} catch (error) {
    console.warn('[Push] Firebase Admin SDK yüklü değil. Push notification devre dışı.');
}

// Firebase Admin SDK başlatma (eğer service account key varsa)
let isInitialized = false;

function initializeFCM() {
    if (isInitialized || !admin) {
        return;
    }

    try {
        // Service account key dosyası kontrolü
        const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './firebase-service-account-key.json';
        const fs = require('fs');
        
        if (fs.existsSync(serviceAccountPath)) {
            const serviceAccount = require(serviceAccountPath);
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            isInitialized = true;
            console.log('[Push] Firebase Admin SDK başlatıldı');
        } else {
            console.warn('[Push] Firebase service account key bulunamadı. Push notification devre dışı.');
            console.warn(`[Push] Service account key path: ${serviceAccountPath}`);
        }
    } catch (error) {
        console.error('[Push] Firebase Admin SDK başlatma hatası:', error.message);
    }
}

// FCM Token kaydetme
function registerFCMToken(phoneNumber, fcmToken) {
    if (!fcmToken) {
        console.warn(`[Push] FCM Token boş: ${phoneNumber}`);
        return;
    }

    fcmTokens.set(phoneNumber, fcmToken);
    console.log(`[Push] FCM Token kaydedildi: phoneNumber=${phoneNumber}, token=${fcmToken.substring(0, 20)}...`);
}

// FCM Token silme
function unregisterFCMToken(phoneNumber) {
    if (fcmTokens.has(phoneNumber)) {
        fcmTokens.delete(phoneNumber);
        console.log(`[Push] FCM Token silindi: phoneNumber=${phoneNumber}`);
    }
}

// Push notification gönderme
async function sendPushNotification(phoneNumber, title, body, data = {}) {
    if (!isInitialized || !admin) {
        console.warn('[Push] Firebase Admin SDK başlatılmamış. Push notification gönderilemedi.');
        return { success: false, error: 'FCM not initialized' };
    }

    const fcmToken = fcmTokens.get(phoneNumber);
    
    if (!fcmToken) {
        console.log(`[Push] FCM Token bulunamadı: ${phoneNumber}`);
        return { success: false, error: 'FCM Token not found' };
    }
    
    const message = {
        notification: {
            title: title,
            body: body
        },
        data: {
            ...data,
            phoneNumber: phoneNumber
        },
        token: fcmToken,
        android: {
            priority: 'high',
            notification: {
                sound: 'default',
                channelId: 'video_call_channel'
            }
        },
        apns: {
            payload: {
                aps: {
                    sound: 'default',
                    badge: 1
                }
            }
        }
    };
    
    try {
        const response = await admin.messaging().send(message);
        console.log(`[Push] Bildirim gönderildi: ${phoneNumber}, messageId=${response}`);
        return { success: true, messageId: response };
    } catch (error) {
        console.error(`[Push] Bildirim gönderme hatası:`, error);
        
        // Geçersiz token ise sil
        if (error.code === 'messaging/invalid-registration-token' || 
            error.code === 'messaging/registration-token-not-registered') {
            fcmTokens.delete(phoneNumber);
            console.log(`[Push] Geçersiz token silindi: ${phoneNumber}`);
        }
        
        return { success: false, error: error.message };
    }
}

// Gelen arama için push notification
async function sendIncomingCallNotification(phoneNumber, callerPhoneNumber, callerName) {
    return await sendPushNotification(
        phoneNumber,
        'Gelen Arama',
        `${callerName || callerPhoneNumber} sizi arıyor`,
        {
            type: 'incoming-call',
            callerPhoneNumber: callerPhoneNumber,
            callerName: callerName || ''
        }
    );
}

// Yeni mesaj için push notification
async function sendNewMessageNotification(phoneNumber, senderPhoneNumber, senderName, message) {
    return await sendPushNotification(
        phoneNumber,
        senderName || senderPhoneNumber,
        message,
        {
            type: 'new-message',
            senderPhoneNumber: senderPhoneNumber,
            senderName: senderName || ''
        }
    );
}

// FCM Token kontrolü
function hasFCMToken(phoneNumber) {
    return fcmTokens.has(phoneNumber);
}

// Tüm FCM token'ları listele (debug için)
function getAllFCMTokens() {
    return Array.from(fcmTokens.entries()).map(([phoneNumber, token]) => ({
        phoneNumber,
        token: token.substring(0, 20) + '...'
    }));
}

// Firebase Admin SDK'yı başlat
initializeFCM();

// ES6 module export
export default { 
    registerFCMToken,
    unregisterFCMToken,
    sendPushNotification,
    sendIncomingCallNotification,
    sendNewMessageNotification,
    hasFCMToken,
    getAllFCMTokens,
    isInitialized: () => isInitialized
};
