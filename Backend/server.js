require('dotenv').config();
const express=require('express');
const app=express(); // express appliaction

require("./firebase");
app.use(express.json());
const PORT = 3010;
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

app.use("/api/hospital", hospitalRoutes);
socket.init(io);
app.use("/testNotification", testNotification);
app.use('/api/auth',authroutes);
app.use('/api/queue',queueroutes);
app.use('/api/admin',dashboard);
app.use("/api/notification", notificationRoutes);

server.listen(PORT, "0.0.0.0",()=>{
    console.log(`server running at ${PORT}`);
})