const mongoose = require('mongoose');

const hospitalSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Hospital name is required'],
      trim: true,
    },
    code: {
      type: String,
      required: [true, 'Hospital unique code is required'],
      unique: true,
      uppercase: true,
      trim: true,
      index: true,
    },
    contactNumber: {
      type: String,
      required: [true, 'Primary contact number is required'],
      trim: true,
    },
    email: {
      type: String,
      lowercase: true,
      trim: true,
      match: [/^\S+@\S+\.\S+$/, 'Please enter a valid email address'],
    },
    address: {
      street: { type: String, trim: true },
      city: { type: String, required: true, trim: true },
      state: { type: String, required: true, trim: true },
      zipCode: { type: String, trim: true },
    },
    // --- ADD THIS: GeoJSON Point for mapping and radius filtering ---
    location: {
      type: {
        type: String,
        enum: ['Point'],
        default: 'Point',
      },
      coordinates: {
        type: [Number], // Array of numbers: [longitude, latitude]
        required: [true, 'Geospatial coordinates are required'],
      },
    },
    departments: [
      {
        type: String,
        trim: true,
      },
    ],
    isActive: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
  }
);

// --- ADD THIS: Essential for fast geospatial radius queries ($near) ---
hospitalSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('HospitalV2', hospitalSchema);