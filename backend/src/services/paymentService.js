import axios from 'axios';
import crypto from 'node:crypto';

/**
 * iyzico Ödeme Servisi
 * Türkiye'deki kredi kartı ve banka kartı ödemeleri için
 */
class PaymentService {
    constructor() {
        // iyzico API ayarları
        this.apiKey = process.env.IYZICO_API_KEY;
        this.secretKey = process.env.IYZICO_SECRET_KEY;
        this.baseUrl = process.env.IYZICO_BASE_URL || 'https://api.iyzipay.com';
        this.isTestMode = process.env.IYZICO_TEST_MODE === 'true';
        
        // Test modunda farklı URL kullanılabilir
        if (this.isTestMode) {
            this.baseUrl = 'https://sandbox-api.iyzipay.com';
        }
        
        this.isEnabled = !!(this.apiKey && this.secretKey);
    }

    /**
     * iyzico için authorization header oluştur
     */
    createAuthorizationString() {
        const randomString = this.generateRandomString();
        const data = `${this.apiKey}:${randomString}`;
        const hash = crypto.createHmac('sha256', this.secretKey).update(data).digest('base64');
        return `IYZWS ${this.apiKey}:${hash}`;
    }

    /**
     * Random string oluştur (iyzico için)
     */
    generateRandomString() {
        return crypto.randomBytes(16).toString('base64').replace(/[^a-zA-Z0-9]/g, '');
    }

    /**
     * Ödeme oluştur (3D Secure ile)
     */
    async createPayment(paymentData) {
        if (!this.isEnabled) {
            throw new Error('iyzico servisi yapılandırılmamış');
        }

        const {
            price,
            currency = 'TRY',
            installment = 1,
            paymentCard,
            buyer,
            billingAddress,
            shippingAddress,
            basketItems,
            callbackUrl
        } = paymentData;

        try {
            const request = {
                locale: 'tr',
                conversationId: `conv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                price: price.toFixed(2),
                paidPrice: price.toFixed(2),
                currency: currency,
                installment: installment.toString(),
                paymentCard: paymentCard,
                buyer: buyer,
                billingAddress: billingAddress,
                shippingAddress: shippingAddress || billingAddress,
                basketItems: basketItems,
                callbackUrl: callbackUrl || `${process.env.WEB_URL || 'https://videocall.app'}/payment/callback`
            };

            const response = await axios.post(
                `${this.baseUrl}/payment/auth`,
                request,
                {
                    headers: {
                        'Authorization': this.createAuthorizationString(),
                        'Content-Type': 'application/json',
                        'x-iyzi-client-version': 'iyzipay-node-2.0.50'
                    }
                }
            );

            return response.data;
        } catch (error) {
            console.error('iyzico ödeme hatası:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * 3D Secure callback işleme
     */
    async handle3DCallback(callbackData) {
        if (!this.isEnabled) {
            throw new Error('iyzico servisi yapılandırılmamış');
        }

        try {
            const response = await axios.post(
                `${this.baseUrl}/payment/3dsecure/auth`,
                callbackData,
                {
                    headers: {
                        'Authorization': this.createAuthorizationString(),
                        'Content-Type': 'application/json',
                        'x-iyzi-client-version': 'iyzipay-node-2.0.50'
                    }
                }
            );

            return response.data;
        } catch (error) {
            console.error('iyzico 3D callback hatası:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * Ödeme sorgulama
     */
    async retrievePayment(paymentId) {
        if (!this.isEnabled) {
            throw new Error('iyzico servisi yapılandırılmamış');
        }

        try {
            const request = {
                locale: 'tr',
                paymentId: paymentId
            };

            const response = await axios.post(
                `${this.baseUrl}/payment/retrieve`,
                request,
                {
                    headers: {
                        'Authorization': this.createAuthorizationString(),
                        'Content-Type': 'application/json',
                        'x-iyzi-client-version': 'iyzipay-node-2.0.50'
                    }
                }
            );

            return response.data;
        } catch (error) {
            console.error('iyzico ödeme sorgulama hatası:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * Ödeme iptal etme
     */
    async cancelPayment(paymentId, amount = null) {
        if (!this.isEnabled) {
            throw new Error('iyzico servisi yapılandırılmamış');
        }

        try {
            const request = {
                locale: 'tr',
                paymentId: paymentId,
                ip: '127.0.0.1' // Gerçek IP alınmalı
            };

            if (amount) {
                request.price = amount.toFixed(2);
            }

            const response = await axios.post(
                `${this.baseUrl}/payment/cancel`,
                request,
                {
                    headers: {
                        'Authorization': this.createAuthorizationString(),
                        'Content-Type': 'application/json',
                        'x-iyzi-client-version': 'iyzipay-node-2.0.50'
                    }
                }
            );

            return response.data;
        } catch (error) {
            console.error('iyzico ödeme iptal hatası:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * Ödeme iade etme
     */
    async refundPayment(paymentId, amount) {
        if (!this.isEnabled) {
            throw new Error('iyzico servisi yapılandırılmamış');
        }

        try {
            const request = {
                locale: 'tr',
                paymentId: paymentId,
                price: amount.toFixed(2),
                ip: '127.0.0.1' // Gerçek IP alınmalı
            };

            const response = await axios.post(
                `${this.baseUrl}/payment/refund`,
                request,
                {
                    headers: {
                        'Authorization': this.createAuthorizationString(),
                        'Content-Type': 'application/json',
                        'x-iyzi-client-version': 'iyzipay-node-2.0.50'
                    }
                }
            );

            return response.data;
        } catch (error) {
            console.error('iyzico ödeme iade hatası:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * Abonelik için ödeme oluştur (helper function)
     */
    async createSubscriptionPayment(userData, subscriptionData) {
        const {
            userId,
            email,
            name,
            phone,
            address,
            city,
            country = 'Turkey',
            zipCode
        } = userData;

        const {
            planId,
            planName,
            price,
            currency = 'TRY'
        } = subscriptionData;

        // Basket items (iyzico için gerekli)
        const basketItems = [
            {
                id: planId,
                name: planName,
                category1: 'Abonelik',
                category2: 'Video Call',
                itemType: 'VIRTUAL',
                price: price.toFixed(2)
            }
        ];

        // Buyer bilgileri
        const buyer = {
            id: userId,
            name: name,
            surname: name.split(' ')[1] || name,
            gsmNumber: phone,
            email: email,
            identityNumber: '', // TC Kimlik No (opsiyonel, yasal gereksinimler için)
            lastLoginDate: new Date().toISOString(),
            registrationDate: new Date().toISOString(),
            registrationAddress: address,
            ip: '127.0.0.1', // Gerçek IP alınmalı
            city: city,
            country: country,
            zipCode: zipCode || '34000'
        };

        // Billing address
        const billingAddress = {
            contactName: name,
            city: city,
            country: country,
            address: address,
            zipCode: zipCode || '34000'
        };

        return {
            buyer,
            billingAddress,
            basketItems,
            price,
            currency
        };
    }
}

// Singleton instance
const paymentService = new PaymentService();

export default paymentService;

