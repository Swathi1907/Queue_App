require('dotenv').config();
const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const User = require('../models/user.models');
const jwt = require('jsonwebtoken');
const Authmiddleware=require('../middleware/authmiddleware');

router.post('/signup', async (req, res) => {
    try {
        const { name, email, password,role } = req.body;

        const existingEmail = await User.findOne({ email });

        if (existingEmail) {
            return res.status(400).json({
                message: "Account already exists"
            });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const newUser = new User({
            name,
            email,
            password: hashedPassword,
            role
        });

        await newUser.save();
if(newUser.role==="admin"){
 res.status(200).json({
            message: "Admin registered successfully"
        });
}
else{
    res.status(200).json({
            message: "user registered successfully"
        });
}

    } catch (err) {
          console.log(req.body)
        res.status(500).json({
          
            message: err.message
        });
    }
});
router.post("/saveFcmToken", Authmiddleware, async (req, res) => {

    try {

        const { fcmToken } = req.body;

        await User.findByIdAndUpdate(
            req.user.userId,
            {
                fcmToken
            }
        );

        res.status(200).json({
            message: "FCM token saved"
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});
router.get(
    '/profile',
    Authmiddleware,
    async (req, res) => {

        try {

            const user =
                await User.findById(
                    req.user.userId
                ).select('-password');

            if (!user) {

                return res.status(404).json({
                    message: "User not found"
                });
            }

            res.status(200).json({
                name: user.name,
                email: user.email,
                role: user.role
            });

        } catch (err) {

            res.status(500).json({
                message: err.message
            });
        }
    }
);
router.post('/login', async(req, res) => {
   try{const {email,password}=req.body;
   const user= await User.findOne({email});
   if(!user){
  return  res.status(400).json({
        message:"User not found",
    })
   }
   const isMatch=await bcrypt.compare(
    password,
    user.password
   );
   if(!isMatch){
    return res.status(500).json({
        message:"Password mismatch",
    })
   }
   const jwt_token = jwt.sign( // jwt.sign takes jwt.sign(payload, secret, options)
    {
        userId:user._id, 
        name: user.name,
        role: user.role,
    },
    process.env.JWT_SECRET,
    {
         expiresIn: "1d",
    }
   )
   res.status(200).json({
    message:"Login successfull",
    jwt_token,
    name:user.name,
    role:user.role
   })}
   catch(err){
res.status(500).json({
    message:err.message,
})
   }
});

module.exports = router;