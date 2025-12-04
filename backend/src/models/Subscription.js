import mongoose from 'mongoose';

const subscriptionSchema = new mongoose.Schema({
  subscriptionId: {
    type: String,
    required: true,
    unique: true,
    index: true
  },
  userId: {
    type: String,
    required: true,
    index: true
  },
  phoneNumber: {
    type: String,
    required: true,
    index: true
  },
  planId: {
    type: String,
    required: true
  },
  status: {
    type: String,
    enum: ['active', 'expired', 'cancelled'],
    required: true,
    default: 'active',
    index: true
  },
  paymentId: {
    type: String,
    default: null
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
subscriptionSchema.index({ subscriptionId: 1 });
subscriptionSchema.index({ userId: 1 });
subscriptionSchema.index({ phoneNumber: 1 });
subscriptionSchema.index({ status: 1 });
subscriptionSchema.index({ expiresAt: 1 });

export default mongoose.models.Subscription || mongoose.model('Subscription', subscriptionSchema);

