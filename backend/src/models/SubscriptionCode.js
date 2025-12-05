import mongoose from 'mongoose';

const subscriptionCodeSchema = new mongoose.Schema({
  code: {
    type: String,
    required: true,
    unique: true,
    uppercase: true // "VC-XXXX-XXXX-XXXX" formatında
    // index: schema.index() ile tanımlanıyor
  },
  phoneNumber: {
    type: String,
    default: null
    // index: schema.index() ile tanımlanıyor
  },
  planId: {
    type: String,
    required: true
  },
  expiresAt: {
    type: Date,
    default: null
  },
  used: {
    type: Boolean,
    default: false
    // index: schema.index() ile tanımlanıyor
  },
  usedAt: {
    type: Date,
    default: null
  },
  usedBy: {
    type: String, // userId veya phoneNumber
    default: null
  },
  deviceType: {
    type: String,
    enum: ['mobile', 'desktop', null],
    default: null
  },
  subscriptionId: {
    type: String,
    default: null
    // index: schema.index() ile tanımlanıyor
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
// NOT: code zaten unique: true olduğu için otomatik index oluşturuluyor
subscriptionCodeSchema.index({ phoneNumber: 1 });
subscriptionCodeSchema.index({ used: 1 });
subscriptionCodeSchema.index({ subscriptionId: 1 });

export default mongoose.models.SubscriptionCode || mongoose.model('SubscriptionCode', subscriptionCodeSchema);

