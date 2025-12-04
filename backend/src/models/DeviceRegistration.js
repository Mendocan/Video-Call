import mongoose from 'mongoose';

const deviceRegistrationSchema = new mongoose.Schema({
  phoneNumber: {
    type: String,
    required: true,
    index: true
  },
  deviceId: {
    type: String, // hashed device ID
    required: true,
    index: true
  },
  registeredAt: {
    type: Date,
    default: Date.now
  },
  lastSeen: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: false // createdAt manuel olarak yönetiliyor
});

// Index'ler - phoneNumber ve deviceId kombinasyonu unique olmalı
deviceRegistrationSchema.index({ phoneNumber: 1, deviceId: 1 }, { unique: true });
deviceRegistrationSchema.index({ phoneNumber: 1 });
deviceRegistrationSchema.index({ deviceId: 1 });

export default mongoose.models.DeviceRegistration || mongoose.model('DeviceRegistration', deviceRegistrationSchema);

