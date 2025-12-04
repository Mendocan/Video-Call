import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import User from './models/User.js';

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
const JWT_EXPIRES_IN = '7d';

/**
 * Kullanıcı kaydı
 */
export async function registerUser(userData) {
  const { name, email, phone, password } = userData;

  // Email kontrolü
  const existingUserByEmail = await User.findOne({ email });
  if (existingUserByEmail) {
    throw new Error('Bu e-posta adresi zaten kullanılıyor.');
  }

  // Telefon kontrolü (eğer telefon numarası varsa)
  if (phone) {
    const existingUserByPhone = await User.findOne({ phone });
    if (existingUserByPhone) {
      throw new Error('Bu telefon numarası zaten kullanılıyor.');
    }
  }

  // Şifreyi hashle
  const hashedPassword = await bcrypt.hash(password, 10);

  const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  
  // MongoDB'ye kaydet
  const user = new User({
    userId,
    name,
    email,
    phone: phone || null,
    password: hashedPassword,
    createdAt: new Date(),
    subscription: null // Abonelik bilgisi
  });

  await user.save();

  // Response için user objesini döndür (password olmadan)
  const userObj = user.toJSON();
  return {
    id: userObj.userId,
    userId: userObj.userId,
    name: userObj.name,
    email: userObj.email,
    phone: userObj.phone,
    createdAt: userObj.createdAt,
    subscription: userObj.subscription
  };
}

/**
 * Kullanıcı girişi
 */
export async function loginUser(email, password) {
  // Kullanıcıyı MongoDB'den bul
  const user = await User.findOne({ email });
  
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
    { userId: user.userId, email: user.email },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );

  // Şifreyi response'dan çıkar (toJSON zaten password'ü çıkarıyor)
  const userObj = user.toJSON();

  return {
    user: {
      id: userObj.userId,
      userId: userObj.userId,
      name: userObj.name,
      email: userObj.email,
      phone: userObj.phone,
      createdAt: userObj.createdAt,
      subscription: userObj.subscription
    },
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
export async function getUserById(userId) {
  const user = await User.findOne({ userId });
  if (!user) {
    return null;
  }
  
  // Response için user objesini döndür (password olmadan)
  const userObj = user.toJSON();
  return {
    id: userObj.userId,
    userId: userObj.userId,
    name: userObj.name,
    email: userObj.email,
    phone: userObj.phone,
    createdAt: userObj.createdAt,
    subscription: userObj.subscription
  };
}

/**
 * Kullanıcıyı email'e göre bul
 */
export async function getUserByEmail(email) {
  const user = await User.findOne({ email });
  if (!user) {
    return null;
  }
  
  const userObj = user.toJSON();
  return {
    id: userObj.userId,
    userId: userObj.userId,
    name: userObj.name,
    email: userObj.email,
    phone: userObj.phone,
    createdAt: userObj.createdAt,
    subscription: userObj.subscription
  };
}

/**
 * Kullanıcıyı telefon numarasına göre bul
 */
export async function getUserByPhone(phone) {
  const user = await User.findOne({ phone });
  if (!user) {
    return null;
  }
  
  const userObj = user.toJSON();
  return {
    id: userObj.userId,
    userId: userObj.userId,
    name: userObj.name,
    email: userObj.email,
    phone: userObj.phone,
    createdAt: userObj.createdAt,
    subscription: userObj.subscription
  };
}

