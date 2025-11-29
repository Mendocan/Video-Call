import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
const JWT_EXPIRES_IN = '7d';

// In-memory user storage (production'da database kullanılmalı)
export const users = new Map();

/**
 * Kullanıcı kaydı
 */
export async function registerUser(userData) {
  const { name, email, phone, password } = userData;

  // Email kontrolü
  for (const [id, user] of users.entries()) {
    if (user.email === email) {
      throw new Error('Bu e-posta adresi zaten kullanılıyor.');
    }
    if (user.phone === phone) {
      throw new Error('Bu telefon numarası zaten kullanılıyor.');
    }
  }

  // Şifreyi hashle
  const hashedPassword = await bcrypt.hash(password, 10);

  const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  const user = {
    id: userId,
    name,
    email,
    phone,
    password: hashedPassword,
    createdAt: new Date().toISOString(),
    subscription: null // Abonelik bilgisi
  };

  users.set(userId, user);
  return user;
}

/**
 * Kullanıcı girişi
 */
export async function loginUser(email, password) {
  // Kullanıcıyı bul
  let user = null;
  for (const [id, u] of users.entries()) {
    if (u.email === email) {
      user = u;
      break;
    }
  }

  if (!user) {
    throw new Error('E-posta veya şifre hatalı.');
  }

  // Şifre kontrolü
  const isPasswordValid = await bcrypt.compare(password, user.password);
  if (!isPasswordValid) {
    throw new Error('E-posta veya şifre hatalı.');
  }

  // JWT token oluştur
  const token = jwt.sign(
    { userId: user.id, email: user.email },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );

  // Şifreyi response'dan çıkar
  const { password: _, ...userWithoutPassword } = user;

  return {
    user: userWithoutPassword,
    token
  };
}

/**
 * JWT token doğrulama
 */
export function verifyToken(token) {
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    return decoded;
  } catch (error) {
    throw new Error('Geçersiz veya süresi dolmuş token.');
  }
}

/**
 * Kullanıcıyı ID'ye göre bul
 */
export function getUserById(userId) {
  return users.get(userId);
}

