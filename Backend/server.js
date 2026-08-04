require('dotenv').config();
const express=require('express');
const app=express(); // express appliaction
app.use(express.urlencoded({ extended: true }));
require("./firebase");
app.use(express.json());
const crypto = require('crypto');
const PORT = 5001;
const QueueModel = require('./VERSION_2/new_models/new_queuev2')
const mongoose=require('mongoose');
MONGO_URI=process.env.MONGO_URI;
mongoose.connect(MONGO_URI).then(()=>{
    console.log("Mongodb connected")

})
.catch((err)=>{
    console.log(err);
})
console.log(process.env.MONGO_URI);
app.get('/',(req,res)=>{
    res.send("Hello queue app");
})

const http=require('http');
const server = http.createServer(app); // actual http server
const { Server } = require("socket.io");
// io-> socket.io server;
const io = new Server(server, {
    cors: {
        origin: "*"
    }
});
io.on("connection", (socket) => {
    console.log("Connected:", socket.id);

    socket.on("disconnect", () => {
        console.log("Disconnected:", socket.id);
    });
});


const authroutes=require('./routes/auth');
const dashboard=require('./admin/dashboard')
const queueroutes=require('./routes/queue');
const notificationRoutes = require("./routes/notifications");
const testNotification = require("./routes/testNotifications");
const socket = require("./socket");
const hospitalRoutes = require("./routes/hospital");
const aiRoutes = require("./routes/ai");

app.use("/api/ai", aiRoutes);
app.use("/api/hospital", hospitalRoutes);
socket.init(io);
app.use("/testNotification", testNotification);
app.use('/api/auth',authroutes);
app.use('/api/queue',queueroutes);
app.use('/api/admin',dashboard);
app.use("/api/notification", notificationRoutes);





// ==========================================
// V2 ROUTES (New Modular Routes)
// ==========================================
const v2HospitalRoutes = require('./VERSION_2/new_routes/new_hosp'); // Ensure route file is in new_routes

app.use('/api/v2/hospital', v2HospitalRoutes);
// Import v2 Auth Routes
const v2AuthRoutes = require('./VERSION_2/new_routes/new_auth');

// Mount v2 Auth Router under /api/v2/auth
app.use('/api/v2/auth', v2AuthRoutes);

const v2QueueRoutes = require('./VERSION_2/new_routes/new_queue');
app.use('/api/v2/queue', v2QueueRoutes);


app.post('/api/v2/payment/verify', async (req, res) => {
    try {
        const { 
            razorpay_order_id, 
            razorpay_payment_id, 
            razorpay_signature, 
            doctorCode, 
            hospitalId, 
            department, 
            userId, 
            patientName,
            amount 
        } = req.body;

        // Your Razorpay Key Secret from the dashboard
        const secret = process.env.PAYMENT_TEST_KEY_SECRET; 

        // Create the expected signature using HMAC SHA256
        const generated_signature = crypto
            .createHmac('sha256', secret)
            .update(razorpay_order_id + '|' + razorpay_payment_id)
            .digest('hex');

        if (generated_signature !== razorpay_signature) {
            return res.status(400).json({ 
                success: false, 
                message: 'Payment verification failed: Invalid signature.' 
            });
        }

        // Signature matches! Payment is authentic. Now update the queue database.
        const todayDate = new Date().toISOString().split('T')[0];

        // 1. Atomically find the queue document for today or create one if it doesn't exist
        const queueDoc = await QueueModel.findOneAndUpdate(
            { doctorCode: doctorCode, date: todayDate },
            { 
                $setOnInsert: { 
                    hospitalId: hospitalId, 
                    department: department,
                    isActive: true 
                } 
            },
            { upsert: true, new: true, setDefaultsOnInsert: true }
        );

        // 2. Determine the next sequential token number
        const nextTokenNumber = queueDoc.tokens.length + 1;

        // 3. Push the patient token into the array with payment details
        queueDoc.tokens.push({
            tokenNumber: nextTokenNumber,
            userId: userId,
            patientName: patientName,
            orderId: razorpay_order_id,
            paymentId: razorpay_payment_id,
            amountPaid: amount || 0,
            status: 'WAITING'
        });

        await queueDoc.save();

        return res.status(200).json({ 
            success: true, 
            message: 'Payment verified successfully and queue slot confirmed.',
            tokenNumber: nextTokenNumber
        });

    } catch (error) {
        console.error('Error verifying payment and updating queue:', error);
        return res.status(500).json({ 
            success: false, 
            error: error.message 
        });
    }
});

server.listen(PORT, "0.0.0.0",()=>{
    console.log(`server running at ${PORT}`);
})