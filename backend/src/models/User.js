import mongoose from 'mongoose';

const userSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    unique: true
    // index: schema.index() ile tanımlanıyor
  },
  name: {
    type: String,
    required: true
  },
  email: {
    type: String,
    required: true,
    unique: true,
    sparse: true // null değerlere izin ver
    // index: schema.index() ile tanımlanıyor
  },
  phone: {
    type: String,
    unique: true,
    sparse: true // null değerlere izin ver
    // index: schema.index() ile tanımlanıyor
  },
  password: {
    type: String,
    required: true
  },
  role: {
    type: String,
    enum: ['user', 'admin'],
    default: 'user'
  },
  subscription: {
    planId: {
      type: String,
      default: null
    },
    status: {
      type: String,
      enum: ['active', 'expired', 'cancelled', null],
      default: null
    },
    expiresAt: {
      type: Date,
      default: null
    }
  },
  // Signaling server için durum bilgileri
  isOnline: {
    type: Boolean,
    default: false
    // index: schema.index() ile tanımlanıyor (gerekirse)
  },
  lastSeen: {
    type: Date,
    default: Date.now
    // index: schema.index() ile tanımlanıyor (gerekirse)
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
// NOT: email, phone, userId zaten unique: true olduğu için otomatik index oluşturuluyor
// Bu yüzden schema.index() çağrıları kaldırıldı (duplicate index uyarısını önlemek için)

// Şifreyi response'dan çıkar
userSchema.methods.toJSON = function() {
  const obj = this.toObject();
  delete obj.password;
  return obj;
};

export default mongoose.models.User || mongoose.model('User', userSchema);

