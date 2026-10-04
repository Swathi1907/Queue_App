const User = require("../new_models/peron_model");
const Notification = require("../new_models/new_notification_model");
const { getMessaging } = require("firebase-admin/messaging");


const sendNotification = async ({
    hospitalId,
    targetRole,
    title,
    message,
    type,
    department = null,
    doctorCode = null
}) => {

    try {

        // Find users of the required role in this hospital
        const users = await User.find({
            hospitalId: hospitalId,
            role: targetRole,
            fcmToken: { $ne: null }
        }).select("fcmToken");

        const tokens = users
            .map(user => user.fcmToken)
            .filter(Boolean);


        // Save notification in MongoDB
        const notification = await Notification.create({
            hospitalId,
            targetRole,
            title,
            message,
            type,
            department,
            doctorCode
        });


        // Send FCM
        if (tokens.length > 0) {

            const response = await getMessaging()
                .sendEachForMulticast({
                    tokens: tokens,

                    notification: {
                        title: title,
                        body: message
                    },

                    data: {
                        notificationId: notification._id.toString(),
                        type: type,
                        targetRole: targetRole,
                        department: department || "",
                        doctorCode: doctorCode || ""
                    }
                });

            console.log(
                `FCM: ${response.successCount} sent, ${response.failureCount} failed`
            );
        }


        return notification;

    } catch (error) {

        console.error(
            "Error sending notification:",
            error
        );

        throw error;
    }
};
const getNotifications = async (req, res) => {
    try {

        console.log("REQ.USER =", req.user);
        const { targetRole } = req.query;

        if (!targetRole) {
            return res.status(400).json({
                message: "targetRole is required"
            });
        }

        const user = await User.findById(req.user.id);

        if (!user) {
            return res.status(404).json({
                message: "User not found"
            });
        }

        const notifications = await Notification.find({
            hospitalId: user.hospitalId,
            targetRole: targetRole
        }).sort({ createdAt: -1 });

        res.status(200).json(notifications);

    } catch (error) {

        console.error(error);

        res.status(500).json({
            message: "Failed to fetch notifications"
        });
    }
};

module.exports = {
    sendNotification,
    getNotifications
};