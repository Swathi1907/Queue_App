package com.swathi.queue_app

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket

object SocketManager {

    private const val SERVER_URL = "http://192.168.29.181:3000"

    private val socket: Socket = IO.socket(SERVER_URL)

    init {

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET", "Connected: ${socket.id()}")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e("SOCKET", "Error: ${it.joinToString()}")
        }
    }

    fun connect() {
        if (!socket.connected()) {
            socket.connect()
        }
    }

    fun disconnect() {
        if (socket.connected()) {
            socket.disconnect()
        }
    }

    fun getSocket() = socket
}