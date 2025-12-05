import mongoose from 'mongoose';

const subscriptionSchema = new mongoose.Schema({
  subscriptionId: {
    type: String,
    required: true,
    unique: true
    // index: schema.index() ile tanımlanıyor
  },
  userId: {
    type: String,
    required: true
    // index: schema.index() ile tanımlanıyor
  },
  phoneNumber: {
    type: String,
    required: true
    // index: schema.index() ile tanımlanıyor
  },
  planId: {
    type: String,
    required: true
  },
  status: {
    type: String,
    enum: ['active', 'expired', 'cancelled'],
    required: true,
    default: 'active'
    // index: schema.index() ile tanımlanıyor
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
    required: true
    // index: schema.index() ile tanımlanıyor
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
// NOT: subscriptionId zaten unique: true olduğu için otomatik index oluşturuluyor
subscriptionSchema.index({ userId: 1 });
subscriptionSchema.index({ phoneNumber: 1 });
subscriptionSchema.index({ status: 1 });
subscriptionSchema.index({ expiresAt: 1 });

export default mongoose.models.Subscription || mongoose.model('Subscription', subscriptionSchema);

