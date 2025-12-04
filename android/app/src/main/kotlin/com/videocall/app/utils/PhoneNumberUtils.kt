package com.videocall.app.utils

/**
 * Telefon numarası formatlama ve normalizasyon yardımcı fonksiyonları
 */
object PhoneNumberUtils {
    /**
     * Telefon numarasını normalize eder (boşlukları ve özel karakterleri kaldırır)
     * @param phoneNumber Ham telefon numarası (örn: "0535 626 15 22" veya "+90 535 626 15 22")
     * @return Normalize edilmiş telefon numarası (örn: "05356261522" veya "+905356261522")
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        // Sadece rakamlar ve + işaretini tut
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        // Eğer +90 ile başlıyorsa, +90'ı koru
        if (cleaned.startsWith("+90")) {
            return cleaned
        }
        
        // Eğer 90 ile başlıyorsa, +90'a çevir
        if (cleaned.startsWith("90") && cleaned.length == 12) {
            return "+$cleaned"
        }
        
        // Eğer 0 ile başlıyorsa, +90'a çevir (0'ı kaldır, 90 ekle)
        if (cleaned.startsWith("0") && cleaned.length == 11) {
            return "+90${cleaned.substring(1)}"
        }
        
        // Diğer durumlarda olduğu gibi döndür
        return cleaned
    }
    
    /**
     * Telefon numarasını backend formatına çevirir (0 ile başlayan format)
     * Backend'de telefon numaraları 0 ile başlayan formatta saklanır
     * @param phoneNumber Normalize edilmiş telefon numarası
     * @return Backend formatında telefon numarası (örn: "05356261522")
     */
    fun toBackendFormat(phoneNumber: String): String {
        val normalized = normalizePhoneNumber(phoneNumber)
        
        // +90 ile başlıyorsa, +90'ı kaldır ve 0 ekle
        if (normalized.startsWith("+90")) {
            return "0${normalized.substring(3)}"
        }
        
        // 90 ile başlıyorsa, 0 ekle
        if (normalized.startsWith("90") && normalized.length == 12) {
            return "0${normalized.substring(2)}"
        }
        
        // Zaten 0 ile başlıyorsa olduğu gibi döndür
        return normalized
    }
    
    /**
     * İki telefon numarasının aynı olup olmadığını kontrol eder
     * Farklı formatlarda olsa bile aynı numarayı tanır
     */
    fun arePhoneNumbersEqual(phone1: String?, phone2: String?): Boolean {
        if (phone1 == null || phone2 == null) return false
        return normalizePhoneNumber(phone1) == normalizePhoneNumber(phone2)
    }
}

