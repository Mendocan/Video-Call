import axios from 'axios';

/**
 * SMS servisi
 * Netgsm API entegrasyonu
 */
class SmsService {
    constructor() {
        this.apiUrl = process.env.NETGSM_API_URL || 'https://api.netgsm.com.tr/sms/send/get';
        this.username = process.env.NETGSM_USERNAME;
        this.password = process.env.NETGSM_PASSWORD;
        this.msgheader = process.env.NETGSM_MSGHEADER || 'VIDEOCALL'; // Gönderen başlığı
        this.isEnabled = !!(this.username && this.password);
    }

    /**
     * SMS gönder
     */
    async sendSMS(phoneNumber, message) {
        if (!this.isEnabled) {
            console.warn('SMS servisi yapılandırılmamış');
            return { success: false, error: 'SMS servisi yapılandırılmamış' };
        }

        // Telefon numarası formatını düzelt (90 ile başlamalı)
        let formattedNumber = phoneNumber.replace(/\D/g, ''); // Sadece rakamlar
        if (!formattedNumber.startsWith('90')) {
            if (formattedNumber.startsWith('0')) {
                formattedNumber = '90' + formattedNumber.substring(1);
            } else {
                formattedNumber = '90' + formattedNumber;
            }
        }

        try {
            const params = new URLSearchParams({
                usercode: this.username,
                password: this.password,
                gsmno: formattedNumber,
                message: message,
                msgheader: this.msgheader,
                dil: 'TR' // Türkçe
            });

            const response = await axios.get(`${this.apiUrl}?${params.toString()}`);

            // Netgsm yanıt formatı: "00 123456789" (başarılı) veya hata kodu
            const result = response.data.trim();
            
            if (result.startsWith('00')) {
                console.log('SMS gönderildi:', result);
                return { success: true, messageId: result };
            } else {
                console.error('SMS gönderim hatası:', result);
                return { success: false, error: result };
            }
        } catch (error) {
            console.error('SMS gönderim hatası:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * 2FA doğrulama kodu SMS'i
     */
    async send2FACode(phoneNumber, code) {
        const message = `Video Call güvenlik kodunuz: ${code}. Bu kodu kimseyle paylaşmayın. Kod 10 dakika geçerlidir.`;
        return await this.sendSMS(phoneNumber, message);
    }

    /**
     * Abonelik yenileme hatırlatıcısı SMS'i
     */
    async sendSubscriptionReminder(phoneNumber, daysUntilExpiry) {
        const message = `Video Call: Aboneliğinizin süresi ${daysUntilExpiry} gün sonra dolacak. Yenilemek için: ${process.env.WEB_URL || 'https://videocall.app'}/pricing`;
        return await this.sendSMS(phoneNumber, message);
    }

    /**
     * Görüşme randevusu hatırlatıcısı SMS'i
     */
    async sendCallReminder(phoneNumber, contactName, scheduledTime) {
        const dateStr = new Date(scheduledTime).toLocaleString('tr-TR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        const message = `Video Call: ${contactName} ile görüşmeniz ${dateStr} tarihinde. Görüşmeye hazır olun!`;
        return await this.sendSMS(phoneNumber, message);
    }

    /**
     * Abonelik sona erme uyarısı SMS'i
     */
    async sendSubscriptionExpiryWarning(phoneNumber, expiryDate) {
        const dateStr = new Date(expiryDate).toLocaleDateString('tr-TR');
        const message = `Video Call: Aboneliğiniz ${dateStr} tarihinde sona erecek. Yenilemek için: ${process.env.WEB_URL || 'https://videocall.app'}/pricing`;
        return await this.sendSMS(phoneNumber, message);
    }
}

// Singleton instance
const smsService = new SmsService();

export default smsService;

