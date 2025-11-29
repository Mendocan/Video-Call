import { verifyToken, getUserById } from '../auth.js';

/**
 * Authentication middleware
 * Protected route'lar için kullanılır
 */
export function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({
      success: false,
      error: 'Token bulunamadı. Lütfen giriş yapın.'
    });
  }

  try {
    const decoded = verifyToken(token);
    const user = getUserById(decoded.userId);

    if (!user) {
      return res.status(401).json({
        success: false,
        error: 'Kullanıcı bulunamadı.'
      });
    }

    // Kullanıcı bilgisini request'e ekle
    req.user = user;
    next();
  } catch (error) {
    return res.status(403).json({
      success: false,
      error: error.message || 'Geçersiz token.'
    });
  }
}

