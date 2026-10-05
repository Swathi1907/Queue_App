package com.swathi.queue_app.v2.utilis

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private const val TAG = "SocketManager"

    // Backend running on your PC
    private const val SOCKET_URL = "http://172.22.23.104:5001"

    private var socket: Socket? = null

    fun connect() {

        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            return
        }

        try {

            Log.d(TAG, "Connecting to: $SOCKET_URL")

            socket = IO.socket(SOCKET_URL)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected: ${socket?.id()}")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(
                    TAG,
                    "Socket connection error: ${args.firstOrNull()}"
                )
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket disconnected")
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e(TAG, "Socket initialization error", e)
        }
    }

    fun joinQueue(queueId: String) {

        Log.d(TAG, "joinQueue() called with queueId = $queueId")

        if (socket?.connected() != true) {
            Log.e(TAG, "Socket NOT connected")
            connect()
            return
        }

        socket?.emit("JOIN_QUEUE_ROOM", queueId)

        Log.d(
            TAG,
            "JOIN_QUEUE_ROOM emitted for queue_$queueId"
        )
    }

    fun leaveQueue(queueId: String) {

        socket?.emit("LEAVE_QUEUE_ROOM", queueId)

        Log.d(TAG, "Requested to leave queue_$queueId")
    }

    fun on(
        event: String,
        listener: (JSONObject) -> Unit
    ) {

        socket?.on(event) { args ->

            val data = args.firstOrNull() as? JSONObject

            if (data != null) {
                listener(data)
            }
        }
    }

    fun off(event: String) {
        socket?.off(event)
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}