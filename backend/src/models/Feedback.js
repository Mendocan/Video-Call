import mongoose from 'mongoose';

const feedbackSchema = new mongoose.Schema({
  userId: {
    type: String,
    default: null,
    index: true
  },
  email: {
    type: String,
    default: null
  },
  phoneNumber: {
    type: String,
    default: null,
    index: true
  },
  type: {
    type: String,
    enum: ['bug', 'feature', 'general'],
    required: true,
    index: true
  },
  subject: {
    type: String,
    required: true
  },
  message: {
    type: String,
    required: true
  },
  rating: {
    type: Number,
    min: 1,
    max: 5,
    default: null
  },
  createdAt: {
    type: Date,
    default: Date.now,
    index: true
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler
feedbackSchema.index({ userId: 1 });
feedbackSchema.index({ phoneNumber: 1 });
feedbackSchema.index({ type: 1 });
feedbackSchema.index({ createdAt: -1 }); // En yeni feedback'ler için

export default mongoose.models.Feedback || mongoose.model('Feedback', feedbackSchema);

