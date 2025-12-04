import mongoose from 'mongoose';

// Connection caching (Next.js pattern)
let cached = global.mongoose;

if (!cached) {
  cached = global.mongoose = { conn: null, promise: null };
}

/**
 * MongoDB'ye bağlan
 * @returns {Promise<mongoose.Connection>}
 */
async function connectDB() {
  const MONGODB_URI = process.env.MONGODB_URI;
  
  if (!MONGODB_URI) {
    console.warn('⚠️ MONGODB_URI environment variable bulunamadı. MongoDB bağlantısı yapılamayacak.');
    console.warn('💡 MongoDB Atlas connection string\'i .env dosyasına ekleyin:');
    console.warn('   MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority');
    throw new Error('MONGODB_URI environment variable is required');
  }

  // Zaten bağlıysa mevcut bağlantıyı döndür
  if (cached.conn) {
    return cached.conn;
  }

  // Bağlantı promise'i yoksa oluştur
  if (!cached.promise) {
    const opts = {
      bufferCommands: false,
      maxPoolSize: 10, // Connection pool size
      serverSelectionTimeoutMS: 5000, // 5 saniye timeout
      socketTimeoutMS: 45000, // 45 saniye socket timeout
    };

    cached.promise = mongoose.connect(MONGODB_URI, opts).then((mongoose) => {
      console.log('✅ MongoDB bağlantısı başarılı');
      console.log(`📊 Database: ${mongoose.connection.db.databaseName}`);
      return mongoose;
    }).catch((error) => {
      console.error('❌ MongoDB bağlantı hatası:', error.message);
      cached.promise = null;
      throw error;
    });
  }

  try {
    cached.conn = await cached.promise;
  } catch (e) {
    cached.promise = null;
    throw e;
  }

  return cached.conn;
}

/**
 * MongoDB bağlantısını kapat
 */
async function disconnectDB() {
  if (cached.conn) {
    await mongoose.disconnect();
    cached.conn = null;
    cached.promise = null;
    console.log('🔌 MongoDB bağlantısı kapatıldı');
  }
}

// Bağlantı durumu event'leri
mongoose.connection.on('connected', () => {
  console.log('🔗 MongoDB bağlandı');
});

mongoose.connection.on('error', (err) => {
  console.error('❌ MongoDB hatası:', err);
});

mongoose.connection.on('disconnected', () => {
  console.log('🔌 MongoDB bağlantısı kesildi');
});

// Graceful shutdown
process.on('SIGINT', async () => {
  await disconnectDB();
  process.exit(0);
});

export default connectDB;
export { disconnectDB };

