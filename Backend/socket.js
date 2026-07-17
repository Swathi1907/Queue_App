let io;

module.exports = {
    init: (socketIo) => {
        io = socketIo;
    },

    getIO: () => {
        if (!io) {
            throw new Error("Socket.IO not initialized");
        }
        return io;
    }
};