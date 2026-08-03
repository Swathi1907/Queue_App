const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'User name is required'],
      trim: true,
    },
    email: {
      type: String,
      unique: true,
      sparse: true,
      lowercase: true,
      trim: true,
    },

    phoneNumber: {
      type: String,
      required: [true, 'Phone number is required'],
      unique: true,
      trim: true,
      index: true,
    },
    password: {
      type: String,
      required: [true, 'Password is required'],
      select: false,
    },
    role: {
      type: String,
      enum: ['PATIENT', 'COMPOUNDER', 'SUPER_ADMIN', 'DOCTOR'],
      default: 'PATIENT',
      required: true,
    },
    hospitalId: {
      type: String,
      ref: 'HospitalV2',
      default: null,
      index: true,
      required: [
        function () {
          return this.role === 'DOCTOR' || this.role === 'COMPOUNDER';
        },
        'Hospital link is required for doctors and compounders',
      ],
    },
   department: {
      type: [String], // Changed from String to an array of strings
      trim: true,
      default: undefined,
      index: true,
      required: [
        function () {
          return this.role === 'DOCTOR';
        },
        'At least one department is required for doctors',
      ],
      validate: {
        validator: function (v) {
          // If role is DOCTOR, ensure the array is provided and has at least 1 item
          if (this.role === 'DOCTOR') {
            return Array.isArray(v) && v.length > 0;
          }
          return true;
        },
        message: 'A doctor must be assigned to at least one department.',
      },
    },
    doctorCode: {
      type: String,
      uppercase: true,
      trim: true,
      default: null,
      required: [
        function () {
          return this.role === 'DOCTOR';
        },
        'Doctor code is required for doctors',
      ],
    },
    qualification: {
      type: String,
      trim: true,
      default: null,
      required: [
        function () {
          return this.role === 'DOCTOR';
        },
        'Qualification is required for doctors',
      ],
    },
    rating: {
      type: Number,
      default: 5.0,
      min: [0, 'Rating cannot be less than 0'],
      max: [5, 'Rating cannot exceed 5'],
      required: [
        function () {
          return this.role === 'DOCTOR';
        },
        'Rating is required for doctors',
      ],
    },
    isAvailable: {
      type: Boolean,
      default: true,
    },
    isActive: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
  }
);

// Unique index for doctor code per hospital
userSchema.index(
  { hospitalId: 1, doctorCode: 1 },
  { unique: true, sparse: true }
);

// REMOVED the pre('save') hook entirely. 
// Passwords are now handled and hashed manually in the controllers.

// Helper method to compare passwords during login
userSchema.methods.comparePassword = async function (candidatePassword) {
  return await bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('UserV2', userSchema);