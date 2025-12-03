/**
 * Telefon Numarası Eşleştirme Utility
 * 
 * Bu modül, farklı formatlardaki telefon numaralarını eşleştirmek için
 * kapsamlı bir algoritma sağlar.
 */

/**
 * Telefon numarasını normalize et (backend formatı: 0 ile başlayan)
 * @param {string} phoneNumber - Normalize edilecek telefon numarası
 * @returns {string|null} Normalize edilmiş telefon numarası veya null
 */
export function normalizePhoneNumber(phoneNumber) {
  if (!phoneNumber) return null;
  
  // Sadece rakamları al
  let cleaned = phoneNumber.replace(/\D/g, '');
  
  // Boşsa null döndür
  if (cleaned.length === 0) return null;
  
  // +90 ile başlıyorsa, +90'ı kaldır ve 0 ekle
  if (cleaned.startsWith('90') && cleaned.length === 12) {
    return '0' + cleaned.substring(2);
  }
  
  // 0 ile başlıyorsa olduğu gibi döndür
  if (cleaned.startsWith('0') && cleaned.length === 11) {
    return cleaned;
  }
  
  // 10 haneli numara ise (0 olmadan), başına 0 ekle
  if (cleaned.length === 10) {
    return '0' + cleaned;
  }
  
  // Diğer durumlarda (10 haneli veya başka format) olduğu gibi döndür
  // Ama log'a yaz
  if (cleaned.length > 0 && cleaned.length < 10) {
    console.warn(`[PhoneMatcher] Uyarı: Telefon numarası formatı beklenmeyen: ${phoneNumber} -> ${cleaned}`);
  }
  
  return cleaned;
}

/**
 * Telefon numarası için tüm olası formatları oluştur
 * @param {string} phoneNumber - Telefon numarası
 * @returns {string[]} Tüm olası formatlar
 */
export function generatePhoneNumberVariants(phoneNumber) {
  if (!phoneNumber) return [];
  
  const variants = new Set();
  
  // Orijinal numara
  variants.add(phoneNumber);
  
  // Sadece rakamlar
  const digitsOnly = phoneNumber.replace(/\D/g, '');
  if (digitsOnly) variants.add(digitsOnly);
  
  // Normalize edilmiş versiyon
  const normalized = normalizePhoneNumber(phoneNumber);
  if (normalized) variants.add(normalized);
  
  // 0 ile başlayan versiyon
  if (digitsOnly && !digitsOnly.startsWith('0') && digitsOnly.length === 10) {
    variants.add('0' + digitsOnly);
  }
  
  // 0 olmadan versiyon
  if (digitsOnly && digitsOnly.startsWith('0') && digitsOnly.length === 11) {
    variants.add(digitsOnly.substring(1));
  }
  
  // +90 ile başlayan versiyon
  if (digitsOnly && !digitsOnly.startsWith('90') && digitsOnly.length === 10) {
    variants.add('90' + digitsOnly);
  }
  
  // +90 olmadan versiyon
  if (digitsOnly && digitsOnly.startsWith('90') && digitsOnly.length === 12) {
    variants.add(digitsOnly.substring(2));
  }
  
  return Array.from(variants).filter(v => v && v.length > 0);
}

/**
 * İki telefon numarasının eşleşip eşleşmediğini kontrol et
 * @param {string} phone1 - İlk telefon numarası
 * @param {string} phone2 - İkinci telefon numarası
 * @returns {boolean} Eşleşiyor mu?
 */
export function arePhoneNumbersMatching(phone1, phone2) {
  if (!phone1 || !phone2) return false;
  
  const normalized1 = normalizePhoneNumber(phone1);
  const normalized2 = normalizePhoneNumber(phone2);
  
  if (!normalized1 || !normalized2) return false;
  
  // Tam eşleşme
  if (normalized1 === normalized2) return true;
  
  // Son 10 hane eşleşmesi (ülke kodu farklı olabilir)
  if (normalized1.length >= 10 && normalized2.length >= 10) {
    const last10_1 = normalized1.slice(-10);
    const last10_2 = normalized2.slice(-10);
    if (last10_1 === last10_2) return true;
  }
  
  return false;
}

/**
 * userRegistry'de telefon numarası ara (fuzzy matching)
 * @param {string} targetPhoneNumber - Aranan telefon numarası
 * @param {Map} userRegistry - Kullanıcı kayıt sistemi
 * @returns {Object|null} Bulunan kullanıcı bilgisi veya null
 */
export function findUserByPhoneNumber(targetPhoneNumber, userRegistry) {
  if (!targetPhoneNumber || !userRegistry) return null;
  
  // Önce normalize edilmiş numara ile direkt arama
  const normalized = normalizePhoneNumber(targetPhoneNumber);
  if (normalized && userRegistry.has(normalized)) {
    return userRegistry.get(normalized);
  }
  
  // Tüm olası formatları oluştur
  const variants = generatePhoneNumberVariants(targetPhoneNumber);
  
  // Her variant için ara
  for (const variant of variants) {
    const normalizedVariant = normalizePhoneNumber(variant);
    if (normalizedVariant && userRegistry.has(normalizedVariant)) {
      console.log(`[PhoneMatcher] ✅ Variant ile bulundu: ${variant} -> ${normalizedVariant}`);
      return userRegistry.get(normalizedVariant);
    }
  }
  
  // Eğer hala bulunamadıysa, userRegistry'deki tüm numaraları tara
  // Bu, normalize edilmiş numaraların son 10 hanesini karşılaştırır
  const targetDigits = normalizePhoneNumber(targetPhoneNumber);
  if (targetDigits && targetDigits.length >= 10) {
    const targetLast10 = targetDigits.slice(-10); // Son 10 hane
    
    for (const [registeredPhone, userInfo] of userRegistry.entries()) {
      if (!registeredPhone || registeredPhone.length < 10) continue;
      
      const registeredLast10 = registeredPhone.slice(-10); // Son 10 hane
      
      // Son 10 hane eşleşiyorsa
      if (targetLast10 === registeredLast10) {
        console.log(`[PhoneMatcher] ✅ Son 10 hane eşleşmesi ile bulundu: ${targetPhoneNumber} -> ${registeredPhone}`);
        return userInfo;
      }
    }
  }
  
  return null;
}

