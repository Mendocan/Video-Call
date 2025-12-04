import mongoose from 'mongoose';

const subscriptionCodeSchema = new mongoose.Schema({
  code: {
    type: String,
    required: true,
    unique: true,
    index: true,
    uppercase: true // "VC-XXXX-XXXX-XXXX" formatında
  },
  phoneNumber: {
    type: String,
    default: null,
    index: true
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
    default: false,
    index: true
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
    default: null,
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
subscriptionCodeSchema.index({ code: 1 });
subscriptionCodeSchema.index({ phoneNumber: 1 });
subscriptionCodeSchema.index({ used: 1 });
subscriptionCodeSchema.index({ subscriptionId: 1 });

export default mongoose.models.SubscriptionCode || mongoose.model('SubscriptionCode', subscriptionCodeSchema);

