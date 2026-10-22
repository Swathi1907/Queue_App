
const User = require("../new_models/peron_model");
const Notification = require("../new_models/new_notification_model");
const { getMessaging } = require("firebase-admin/messaging");

// ==========================================
// SEND NOTIFICATION
// ==========================================
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
        console.log("[sendNotification] Starting:", {
            hospitalId,
            targetRole,
            type,
            department,
            doctorCode
        });

        // Find recipients in this hospital.
        const users = await User.find({
            hospitalId,
            role: targetRole,
            fcmToken: { $exists: true, $nin: [null, ""] }
        }).select("_id fcmToken");

        const tokens = [
            ...new Set(
                users.map(user => user.fcmToken).filter(Boolean)
            )
        ];

        console.log("[sendNotification] Recipients:", users.length);
        console.log("[sendNotification] FCM tokens:", tokens.length);

        // Save notification in MongoDB.
        const notification = await Notification.create({
            hospitalId,
            targetRole,
            title,
            message,
            type,
            department,
            doctorCode
        });

        console.log(
            "[sendNotification] Saved notification:",
            notification._id.toString()
        );

        // Send push notifications.
        if (tokens.length > 0) {
            const response = await getMessaging().sendEachForMulticast({
                tokens,
                notification: {
                    title,
                    body: message
                },
                data: {
                    notificationId: notification._id.toString(),
                    type,
                    targetRole,
                    department: department || "",
                    doctorCode: doctorCode || ""
                }
            });

            console.log("[sendNotification] FCM result:", {
                successCount: response.successCount,
                failureCount: response.failureCount
            });

            // Remove invalid or unregistered tokens.
            const invalidTokens = [];

            response.responses.forEach((result, index) => {
                if (
                    !result.success &&
                    [
                        "messaging/invalid-registration-token",
                        "messaging/registration-token-not-registered"
                    ].includes(result.error?.code)
                ) {
                    invalidTokens.push(tokens[index]);
                }
            });

            if (invalidTokens.length > 0) {
                await User.updateMany(
                    { fcmToken: { $in: invalidTokens } },
                    { $set: { fcmToken: null } }
                );

                console.log(
                    "[sendNotification] Removed invalid FCM tokens:",
                    invalidTokens.length
                );
            }
        } else {
            console.log(
                "[sendNotification] No registered FCM tokens found"
            );
        }

        return notification;

    } catch (error) {
        console.error("[sendNotification] Error:", error);
        throw error;
    }
};


// ==========================================
// GET NOTIFICATIONS FOR AUTHENTICATED USER
// ==========================================
const getNotifications = async (req, res) => {
    const requestId = `NOTIF-${Date.now()}`;

    try {
        console.log(`\n[${requestId}] GET notifications`);
        console.log(`[${requestId}] User ID:`, req.user?.id);

        const user = await User.findById(req.user?.id)
            .select("role hospitalId")
            .lean();

        if (!user) {
            return res.status(404).json({
                success: false,
                message: "User not found",
                requestId
            });
        }

        if (!user.hospitalId) {
            return res.status(403).json({
                success: false,
                message: "User is not associated with a hospital",
                requestId
            });
        }

        // Never trust targetRole supplied by the client.
        // Return notifications intended for this user's role only.
        const { unreadOnly = "false", page = "1", limit = "20" } =
            req.query;

        const pageNumber = Math.max(parseInt(page, 10) || 1, 1);
        const pageSize = Math.min(
            Math.max(parseInt(limit, 10) || 20, 1),
            50
        );

        const query = {
            hospitalId: user.hospitalId,
            targetRole: user.role
        };

        if (unreadOnly === "true") {
            query.isRead = false;
        }

        console.log(`[${requestId}] Query:`, query);

        const [notifications, total, unreadCount] = await Promise.all([
            Notification.find(query)
                .sort({ createdAt: -1 })
                .skip((pageNumber - 1) * pageSize)
                .limit(pageSize)
                .lean(),

            Notification.countDocuments(query),

            Notification.countDocuments({
                hospitalId: user.hospitalId,
                targetRole: user.role,
                isRead: false
            })
        ]);

        console.log(`[${requestId}] Results:`, {
            count: notifications.length,
            total,
            unreadCount
        });

        return res.status(200).json({
            success: true,
            requestId,
            pagination: {
                page: pageNumber,
                limit: pageSize,
                total,
                totalPages: Math.ceil(total / pageSize)
            },
            unreadCount,
            notifications
        });

    } catch (error) {
        console.error(`[${requestId}] Fetch notifications failed:`, {
            message: error.message,
            stack: error.stack
        });

        return res.status(500).json({
            success: false,
            message: "Failed to fetch notifications",
            requestId
        });
    }
};


module.exports = {
    sendNotification,
    getNotifications
};
