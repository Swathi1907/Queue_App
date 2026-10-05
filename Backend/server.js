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

    socket.on("JOIN_QUEUE_ROOM", (queueId) => {

        socket.join(`queue_${queueId}`);

        console.log(
            `Socket ${socket.id} joined queue_${queueId}`
        );
    });

    socket.on("LEAVE_QUEUE_ROOM", (queueId) => {

        socket.leave(`queue_${queueId}`);

        console.log(
            `Socket ${socket.id} left queue_${queueId}`
        );
    });

    socket.on("disconnect", () => {
        console.log("Disconnected:", socket.id);
    });

});

const Razorpay = require("razorpay");

const razorpayInstance = new Razorpay({
    key_id: process.env.PAYMENT_TEST_API_KEY,
    key_secret: process.env.PAYMENT_TEST_KEY_SECRET
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


const v2AdminRoutes=require('./VERSION_2/new_routes/new_admin')
app.use('/api/v2/admin',v2AdminRoutes);


const v2DoctorRoutes=require('./VERSION_2/new_routes/new_doctor')
app.use('/api/v2/doctor',v2DoctorRoutes);



const v2HospitalRoutes = require('./VERSION_2/new_routes/new_hosp'); // Ensure route file is in new_routes
app.use('/api/v2/hospital', v2HospitalRoutes);

const v2notificationRoutes=require('./VERSION_2/new_routes/new_notifications')
app.use("/api/v2/notifications", v2notificationRoutes);

const v2AiRoutes = require('./VERSION_2/new_routes/new_ai');
app.use('/api/v2/ai', v2AiRoutes); // Mounted under /api/v2/ai/scanResume

// Import v2 Auth Routes
const v2AuthRoutes = require('./VERSION_2/new_routes/new_auth');
// Mount v2 Auth Router under /api/v2/auth
app.use('/api/v2/auth', v2AuthRoutes);




const v2QueueRoutes = require('./VERSION_2/new_routes/new_queue');
app.use('/api/v2/queue', v2QueueRoutes);


const UserActiveQueue = require('./VERSION_2/new_models/new_user_active_queue');
const UserV2 = require('./VERSION_2/new_models/peron_model');
const HospitalV2 = require('./VERSION_2/new_models/new_hosp_model');

app.post('/api/v2/payment/verify', async (req, res) => {

    try {

        console.log("=== PAYMENT VERIFY HIT ===");

        console.log(
            "Request Body Received:",
            JSON.stringify(req.body, null, 2)
        );

        const {
            razorpay_order_id,
            razorpay_payment_id,
            razorpay_signature,

            doctorCode,
            hospitalId,
            department,
            userId,

            patientName,
            amount,
            notes
        } = req.body;


        // ---------------------------------------
        // 1. VALIDATION
        // ---------------------------------------

        if (
            !hospitalId ||
            !department ||
            !userId ||
            !doctorCode ||
            !razorpay_order_id ||
            !razorpay_payment_id ||
            !razorpay_signature
        ) {

            return res.status(400).json({
                success: false,
                message:
                    'Required payment and queue fields are missing.'
            });

        }


        // ---------------------------------------
        // 2. VERIFY RAZORPAY SIGNATURE
        // ---------------------------------------

        const secret =
            process.env.PAYMENT_TEST_KEY_SECRET;

        const generated_signature =
            crypto
                .createHmac('sha256', secret)
                .update(
                    razorpay_order_id +
                    '|' +
                    razorpay_payment_id
                )
                .digest('hex');


        if (
            generated_signature !==
            razorpay_signature
        ) {

            console.log(
                "Signature mismatch"
            );

            return res.status(400).json({
                success: false,
                message:
                    'Payment verification failed: Invalid signature.'
            });

        }


        console.log(
            "Signature verified successfully."
        );


        // ---------------------------------------
        // 3. TODAY
        // ---------------------------------------

        const todayDate =
            new Date()
                .toISOString()
                .split('T')[0];


        // ---------------------------------------
// 4. FIND QUEUE
// ---------------------------------------

console.log("\n======================================");
console.log("QUEUE LOOKUP");
console.log("======================================");

console.log("hospitalId:", hospitalId);
console.log("department:", department);
console.log("doctorCode:", doctorCode);

// Find queue using doctorCode only
let queueDoc = await QueueModel.findOne({
    doctorCode: doctorCode
}).sort({ createdAt: -1 });

console.log("\nQUEUE FOUND:");

if (queueDoc) {
    console.log({
        queueId: queueDoc._id.toString(),
        hospitalId: queueDoc.hospitalId,
        department: queueDoc.department,
        doctorCode: queueDoc.doctorCode,
        queueStatus: queueDoc.queueStatus,
        isActive: queueDoc.isActive,
        tokenCount: queueDoc.tokens?.length || 0
    });
} else {
    console.log("❌ NO QUEUE FOUND");
}

// ---------------------------------------
// IF NO QUEUE
// ---------------------------------------

if (!queueDoc) {

    return res.status(400).json({
        success: false,
        message: "No queue found for this doctor."
    });
}

console.log(
    "Using Queue ID:",
    queueDoc._id.toString()
);

console.log(
    "Tokens BEFORE:",
    queueDoc.tokens?.length || 0
);

// ---------------------------------------
// 5. GENERATE TOKEN
// ---------------------------------------

const nextTokenNumber =
    queueDoc.tokens.length + 1;

console.log(
    "Next Token Number:",
    nextTokenNumber
);


// ---------------------------------------
// 6. CREATE TOKEN
// ---------------------------------------

const newToken = {

    tokenNumber: nextTokenNumber,

    userId: userId,

    patientName: patientName,

    orderId: razorpay_order_id,

    paymentId: razorpay_payment_id,

    amountPaid: amount || 0,

    status: "WAITING",

    notes: notes || ""
};

console.log("\nNEW TOKEN:");
console.log(newToken);


// ---------------------------------------
// 7. ADD TOKEN TO QUEUE
// ---------------------------------------

queueDoc.tokens.push(newToken);

console.log(
    "Tokens AFTER PUSH:",
    queueDoc.tokens.length
);

await queueDoc.save();

console.log(
    "✅ QUEUE SAVED"
);

console.log(
    "Tokens AFTER SAVE:",
    queueDoc.tokens.length
);


// ---------------------------------------
// GET CREATED TOKEN
// ---------------------------------------

const createdToken =
    queueDoc.tokens[
        queueDoc.tokens.length - 1
    ];

console.log("\nCREATED TOKEN:");

console.log({
    tokenId: createdToken._id,
    tokenNumber: createdToken.tokenNumber,
    userId: createdToken.userId,
    status: createdToken.status
});


// ---------------------------------------
// 8. CREATE USER ACTIVE QUEUE
// ---------------------------------------
// ---------------------------------------
// 8. GET HOSPITAL + DOCTOR DETAILS
// ---------------------------------------

let hospital = null;
let doctor = null;

// Hospital ID may be either MongoDB _id or hospital code
if (
    typeof queueDoc.hospitalId === "string" &&
    /^[0-9a-fA-F]{24}$/.test(queueDoc.hospitalId)
) {

    hospital = await HospitalV2.findById(
        queueDoc.hospitalId
    ).lean();

} else {

    hospital = await HospitalV2.findOne({
        code: queueDoc.hospitalId
    }).lean();
}


// Find doctor
doctor = await UserV2.findOne({
    doctorCode: queueDoc.doctorCode,
    role: "DOCTOR"
}).lean();


console.log("HOSPITAL FOR ACTIVE QUEUE:", hospital);
console.log("DOCTOR FOR ACTIVE QUEUE:", doctor);


// ---------------------------------------
// 9. CREATE USER ACTIVE QUEUE
// ---------------------------------------

const activeQueue =
    await UserActiveQueue.create({

        userId: userId,

        queueId: queueDoc._id,

        tokenId: createdToken._id,

        tokenNumber: nextTokenNumber,


        // -------------------------
        // Hospital
        // -------------------------

        hospitalId:
            queueDoc.hospitalId,

        hospitalName:
            hospital?.name || "",

        hospitalLogoUrl:
            hospital?.logoUrl || "",


        // -------------------------
        // Doctor
        // -------------------------

        doctorCode:
            queueDoc.doctorCode,

        doctorName:
            doctor?.name || "",


        // -------------------------
        // Queue
        // -------------------------

        department:
            queueDoc.department || department || "",

        roomNumber:
            queueDoc.roomNumber || "",

        date:
            queueDoc.date || "",


        // -------------------------
        // Status
        // -------------------------

        status: "WAITING",

        joinedAt: new Date()
    });

console.log("\n✅ USER ACTIVE QUEUE CREATED");

console.log({
    activeQueueId: activeQueue._id,
    userId: activeQueue.userId,
    queueId: activeQueue.queueId,
    tokenId: activeQueue.tokenId,
    tokenNumber: activeQueue.tokenNumber,
    doctorCode: activeQueue.doctorCode,
    department: activeQueue.department,
    status: activeQueue.status
});

console.log("\n======================================");
console.log("PAYMENT → QUEUE SUCCESS");
console.log("Queue ID:", queueDoc._id.toString());
console.log("Token ID:", createdToken._id.toString());
console.log("Token Number:", nextTokenNumber);
console.log("======================================\n");

        // ---------------------------------------
        // 9. RESPONSE
        // ---------------------------------------

        return res.status(200).json({

            success: true,

            message:
                'Payment verified successfully and queue slot confirmed.',

            tokenNumber:
                nextTokenNumber,

            queueId:
                queueDoc._id,

            tokenId:
                createdToken._id
        });


    } catch (error) {

        console.error(
            'Error verifying payment and updating queue:',
            error
        );

        return res.status(500).json({

            success: false,

            error:
                error.message
        });

    }

});

// Create Order Endpoint
// Create Order Endpoint
app.post('/api/v2/payment/create-order', async (req, res) => {
    try {
        console.log("hit")
        const { amount, doctorCode } = req.body;

        // Keep the receipt short (under 40 characters)
        const shortDocCode = (doctorCode || 'doc').substring(0, 10);
        const uniqueSuffix = Date.now().toString().slice(-8); // Last 8 digits of timestamp

        const options = {
            amount: amount * 100, // Amount in smallest currency unit (paise for INR)
            currency: "INR",
            receipt: `rcpt_${shortDocCode}_${uniqueSuffix}` // Well under 40 chars
        };

        const order = await razorpayInstance.orders.create(options);
        
        if (!order) {
            return res.status(500).json({ success: false, message: "Error creating Razorpay order" });
        }
console.log("response sent",order.id)
        return res.status(200).json({
            success: true,
            orderId: order.id,
            amount: order.amount,
            currency: order.currency
        });

    } catch (error) {
        console.error("Error creating order:", error);
        return res.status(500).json({ success: false, error: error.message });
    }
});
server.listen(PORT, "0.0.0.0",()=>{
    console.log(`server running at ${PORT}`);
})