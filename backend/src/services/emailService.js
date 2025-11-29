import nodemailer from 'nodemailer';

/**
 * E-posta servisi
 * Gmail SMTP ile başlar, büyüme durumunda SendGrid/AWS SES'e geçilebilir
 */
class EmailService {
    constructor() {
        // Gmail SMTP transporter (başlangıç)
        this.transporter = null;
        this.isInitialized = false;
        this.isDisabled = false;
        
        // SendGrid/AWS SES için gelecek güncelleme
        this.provider = process.env.EMAIL_PROVIDER || 'gmail'; // 'gmail', 'sendgrid', 'ses'
    }

    /**
     * E-posta servisini başlat
     */
    async initialize() {
        if (this.isInitialized || this.isDisabled) return;

        try {
            if (this.provider === 'gmail') {
                const hasCredentials = Boolean(process.env.GMAIL_USER && process.env.GMAIL_APP_PASSWORD);
                if (!hasCredentials) {
                    this.isDisabled = true;
                    console.warn('📭 E-posta servisi devre dışı: GMAIL_USER ve GMAIL_APP_PASSWORD tanımlı değil.');
                    return;
                }

                // Gmail SMTP
                this.transporter = nodemailer.createTransport({
                    service: 'gmail',
                    auth: {
                        user: process.env.GMAIL_USER,
                        pass: process.env.GMAIL_APP_PASSWORD // Gmail App Password gerekli
                    }
                });
            } else if (this.provider === 'sendgrid') {
                // SendGrid (gelecek güncelleme)
                // const sgMail = require('@sendgrid/mail');
                // sgMail.setApiKey(process.env.SENDGRID_API_KEY);
                // this.transporter = sgMail;
            } else if (this.provider === 'ses') {
                // AWS SES (gelecek güncelleme)
                // AWS SDK ile yapılacak
            }

            // Bağlantı testi
            if (this.transporter && this.provider === 'gmail') {
                await this.transporter.verify();
            }

            this.isInitialized = true;
            console.log(`E-posta servisi başlatıldı: ${this.provider}`);
        } catch (error) {
            console.error('E-posta servisi başlatılamadı:', error);
            this.isInitialized = false;
            this.isDisabled = true;
        }
    }

    /**
     * E-posta gönder
     */
    async sendEmail({ to, subject, html, text }) {
        if (!this.isInitialized && !this.isDisabled) {
            await this.initialize();
        }

        if (!this.transporter || this.isDisabled) {
            console.warn('📭 E-posta gönderimi atlandı: E-posta servisi devrede değil.');
            return { success: false, skipped: true, reason: 'Email service disabled' };
        }

        try {
            const mailOptions = {
                from: process.env.EMAIL_FROM || process.env.GMAIL_USER,
                to,
                subject,
                html,
                text: text || html.replace(/<[^>]*>/g, '') // HTML'den text çıkar
            };

            if (this.provider === 'gmail') {
                const info = await this.transporter.sendMail(mailOptions);
                console.log('E-posta gönderildi:', info.messageId);
                return { success: true, messageId: info.messageId };
            } else {
                // SendGrid/AWS SES implementasyonu gelecek
                throw new Error('Provider henüz desteklenmiyor');
            }
        } catch (error) {
            console.error('E-posta gönderim hatası:', error);
            throw error;
        }
    }

    /**
     * Abonelik onay e-postası
     */
    async sendSubscriptionConfirmation(userEmail, subscriptionDetails) {
        const subject = 'Abonelik Onayı - Video Call';
        const html = `
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
                        <h1>Abonelik Onayı</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p>Aboneliğiniz başarıyla oluşturuldu!</p>
                        <p><strong>Abonelik Detayları:</strong></p>
                        <ul>
                            <li>Plan: ${subscriptionDetails.planName}</li>
                            <li>Başlangıç: ${new Date(subscriptionDetails.startDate).toLocaleDateString('tr-TR')}</li>
                            <li>Bitiş: ${new Date(subscriptionDetails.endDate).toLocaleDateString('tr-TR')}</li>
                            <li>Fiyat: ${subscriptionDetails.price} TRY</li>
                        </ul>
                        <p>Aboneliğiniz süresince tüm özelliklerden faydalanabilirsiniz.</p>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }

    /**
     * Abonelik yenileme hatırlatıcısı
     */
    async sendSubscriptionRenewalReminder(userEmail, daysUntilExpiry) {
        const subject = `Abonelik Yenileme Hatırlatıcısı - ${daysUntilExpiry} Gün Kaldı`;
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #FF9800, #F57C00); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #FF9800; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Abonelik Yenileme Hatırlatıcısı</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p>Aboneliğinizin süresi <strong>${daysUntilExpiry} gün</strong> sonra dolacak.</p>
                        <p>Kesintisiz hizmet almak için aboneliğinizi yenilemeyi unutmayın.</p>
                        <a href="${process.env.WEB_URL || 'https://videocall.app'}/pricing" class="button">Aboneliği Yenile</a>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }

    /**
     * Abonelik sona erme uyarısı
     */
    async sendSubscriptionExpiryWarning(userEmail, expiryDate) {
        const subject = 'Abonelik Sona Erme Uyarısı - Video Call';
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #F44336, #D32F2F); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #F44336; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Abonelik Sona Erme Uyarısı</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p>Aboneliğiniz <strong>${new Date(expiryDate).toLocaleDateString('tr-TR')}</strong> tarihinde sona erecek.</p>
                        <p>Hizmetlerimizden kesintisiz faydalanmak için aboneliğinizi yenilemeniz gerekmektedir.</p>
                        <a href="${process.env.WEB_URL || 'https://videocall.app'}/pricing" class="button">Aboneliği Yenile</a>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }

    /**
     * Görüşme randevusu hatırlatıcısı
     */
    async sendCallReminder(userEmail, callDetails) {
        const subject = `Görüşme Hatırlatıcısı - ${callDetails.contactName}`;
        const html = `
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
                        <h1>Görüşme Hatırlatıcısı</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p><strong>${callDetails.contactName}</strong> ile planladığınız görüşme yaklaşıyor.</p>
                        <p><strong>Tarih:</strong> ${new Date(callDetails.scheduledTime).toLocaleString('tr-TR')}</p>
                        ${callDetails.notes ? `<p><strong>Notlar:</strong> ${callDetails.notes}</p>` : ''}
                        <p>Görüşmeye hazır olun!</p>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }

    /**
     * 2FA doğrulama e-postası
     */
    async send2FAVerificationCode(userEmail, code) {
        const subject = 'İki Faktörlü Doğrulama Kodu - Video Call';
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #4CAF50, #388E3C); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                    .code { font-size: 32px; font-weight: bold; text-align: center; padding: 20px; background: white; border: 2px dashed #4CAF50; border-radius: 8px; margin: 20px 0; letter-spacing: 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { background: #FFF3CD; border-left: 4px solid #FFC107; padding: 12px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Güvenlik Kodu</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p>İki faktörlü doğrulama için güvenlik kodunuz:</p>
                        <div class="code">${code}</div>
                        <div class="warning">
                            <strong>⚠️ Güvenlik Uyarısı:</strong> Bu kodu kimseyle paylaşmayın. Kod 10 dakika geçerlidir.
                        </div>
                        <p>Eğer bu işlemi siz yapmadıysanız, lütfen hemen şifrenizi değiştirin.</p>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }

    /**
     * Geri bildirim bildirimi (admin'e)
     */
    async sendFeedbackNotification({ rating, name, email, comment }) {
        const adminEmail = process.env.ADMIN_EMAIL || process.env.GMAIL_USER;
        const subject = `Yeni Geri Bildirim - ${rating} Yıldız - Video Call`;
        const stars = '⭐'.repeat(rating) + '☆'.repeat(5 - rating);
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #FFA500, #FF8C00); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                    .rating { font-size: 24px; text-align: center; margin: 15px 0; }
                    .info { background: white; padding: 15px; border-radius: 4px; margin: 10px 0; }
                    .comment { background: white; padding: 15px; border-radius: 4px; margin: 10px 0; border-left: 4px solid #FFA500; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Yeni Geri Bildirim</h1>
                    </div>
                    <div class="content">
                        <div class="rating">${stars}</div>
                        <div class="info">
                            <p><strong>Ad Soyad:</strong> ${name}</p>
                            <p><strong>E-posta:</strong> ${email}</p>
                            <p><strong>Puan:</strong> ${rating} / 5</p>
                        </div>
                        <div class="comment">
                            <p><strong>Yorum:</strong></p>
                            <p>${comment.replace(/\n/g, '<br>')}</p>
                        </div>
                        <p>Teşekkürler,<br>Video Call Sistemi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: adminEmail,
            subject,
            html
        });
    }

    /**
     * Şifre sıfırlama e-postası
     */
    async sendPasswordReset(userEmail, resetToken) {
        const resetUrl = `${process.env.WEB_URL || 'https://videocall.app'}/reset-password?token=${resetToken}`;
        const subject = 'Şifre Sıfırlama - Video Call';
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #2196F3, #1976D2); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #2196F3; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { background: #FFF3CD; border-left: 4px solid #FFC107; padding: 12px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Şifre Sıfırlama</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba,</p>
                        <p>Şifrenizi sıfırlamak için aşağıdaki butona tıklayın:</p>
                        <a href="${resetUrl}" class="button">Şifremi Sıfırla</a>
                        <p>Veya bu linki kopyalayıp tarayıcınıza yapıştırın:</p>
                        <p style="word-break: break-all; color: #666;">${resetUrl}</p>
                        <div class="warning">
                            <strong>⚠️ Güvenlik Uyarısı:</strong> Bu link 1 saat geçerlidir. Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelin.
                        </div>
                        <p>Teşekkürler,<br>Video Call Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        return await this.sendEmail({
            to: userEmail,
            subject,
            html
        });
    }
}

// Singleton instance
const emailService = new EmailService();

export default emailService;

