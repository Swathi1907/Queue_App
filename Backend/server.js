require('dotenv').config();
const express=require('express');
const app=express(); // express appliaction
app.use(express.urlencoded({ extended: true }));
require("./firebase");
app.use(express.json());
const PORT = 5000;
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




app.post('/api/v2/payment/verify', (req, res) => {
    try {
        const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = req.body;

        // Your Razorpay Key Secret from the dashboard
        const secret = process.env.PAYMENT_TEST_KEY_SECRET; 

        // Create the expected signature using HMAC SHA256
        const generated_signature = crypto
            .createHmac('sha256', secret)
            .update(razorpay_order_id + '|' + razorpay_payment_id)
            .digest('hex');

        if (generated_signature === razorpay_signature) {
            // Signature matches! Payment is authentic.
            // TODO: Update your database to mark the consultation slot as booked/confirmed.
            return res.status(200).json({ 
                success: true, 
                message: 'Payment verified successfully and queue slot confirmed.' 
            });
        } else {
            // Signature mismatch
            return res.status(400).json({ 
                success: false, 
                message: 'Payment verification failed: Invalid signature.' 
            });
        }
    } catch (error) {
        console.error('Error verifying payment:', error);
        return res.status(500).json({ 
            success: false, 
            error: error.message 
        });
    }
});


server.listen(PORT, "0.0.0.0",()=>{
    console.log(`server running at ${PORT}`);
})