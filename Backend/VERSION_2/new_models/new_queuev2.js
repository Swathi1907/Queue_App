const mongoose = require('mongoose');

const queueSchema = new mongoose.Schema({
  hospitalId: { type: String, required: true, index: true },
  department: { type: String, required: true, index: true },
  doctorCode: { type: String, required: true, index: true },
  date: { type: String, required: true }, // Format: YYYY-MM-DD
  tokens: [
    {
      tokenNumber: { type: Number, required: true },
      userId: { type: mongoose.Schema.Types.ObjectId, ref: 'UserV2', required: true }, // Linked to your user model
      patientName: { type: String, required: true },
      orderId: { type: String, required: true },     // Razorpay Order ID for tracking/refunds
      paymentId: { type: String, required: true },   // Razorpay Payment ID after successful verification
      amountPaid: { type: Number, required: true },  // Consultation fee stored securely
      status: { 
        type: String, 
        enum: ['WAITING', 'IN_CONSULTATION', 'COMPLETED', 'CANCELLED'], 
        default: 'WAITING' 
      },
      createdAt: { type: Date, default: Date.now }
    }
  ],
  isActive: { type: Boolean, default: true }
}, { timestamps: true });

// Ensure a doctor only has one active queue per day for a department
queueSchema.index({ doctorCode: 1, date: 1 }, { unique: true });

module.exports = mongoose.model('new_queueV2', queueSchema);