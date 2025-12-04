import mongoose from 'mongoose';

const userSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    unique: true,
    index: true
  },
  name: {
    type: String,
    required: true
  },
  email: {
    type: String,
    required: true,
    unique: true,
    sparse: true, // null değerlere izin ver
    index: true
  },
  phone: {
    type: String,
    unique: true,
    sparse: true, // null değerlere izin ver
    index: true
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
    default: false,
    index: true
  },
  lastSeen: {
    type: Date,
    default: Date.now,
    index: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
userSchema.index({ email: 1 });
userSchema.index({ phone: 1 });
userSchema.index({ userId: 1 });

// Şifreyi response'dan çıkar
userSchema.methods.toJSON = function() {
  const obj = this.toObject();
  delete obj.password;
  return obj;
};

export default mongoose.models.User || mongoose.model('User', userSchema);

