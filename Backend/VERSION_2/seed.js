const path = require('path');
// Point to .env in the parent directory
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const mongoose = require('mongoose');
const User = require('./new_models/peron_model');

console.log('Loaded MONGO_URI:', process.env.MONGO_URI);

const createSuperAdmin = async () => {
  try {
    if (!process.env.MONGO_URI) {
      throw new Error("MONGO_URI still undefined. Check your parent folder's .env file!");
    }

    await mongoose.connect(process.env.MONGO_URI);
    console.log('Connected to DB:', mongoose.connection.name);

    await User.deleteOne({ email: 'supaadmin@gmail.com' });

    const admin = await User.create({
      name: 'Sheker',
      email: 'sheker@gmail.com',
      phoneNumber: '+919347891848',
      password: 'superadmin123',
      role: 'SUPER_ADMIN',
    });

    console.log('SUPER_ADMIN created successfully!');
    console.log(admin);

    process.exit(0);
  } catch (error) {
    console.error('Error creating admin:', error.message);
    process.exit(1);
  }
};

createSuperAdmin();