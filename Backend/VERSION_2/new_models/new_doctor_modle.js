import mongoose from 'mongoose';

const doctorSchema = new mongoose.Schema({
  doctorId: { 
    type: String, 
    required: true, 
    unique: true 
  },
  hospitalId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Hospital', 
    required: true 
  },
  fullName: { 
    type: String, 
    required: true,
    trim: true 
  },
  email: { 
    type: String, 
    required: true, 
    unique: true,
    lowercase: true,
    trim: true 
  },
  department: { 
    type: String, 
    required: true,
    trim: true 
  }, // Stores the exact string matching the hospital's departments array
  defaultRoomNumber: { 
    type: String, 
    required: true,
    trim: true 
  },
  designation: { 
    type: String,
    trim: true 
  },
  isActive: { 
    type: Boolean, 
    default: true 
  }
}, { timestamps: true });

export const Doctor = mongoose.model('Doctor', doctorSchema);