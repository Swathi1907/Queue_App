package com.swathi.queue_app.repository

import com.swathi.queue_app.api.RetrofitInstance
import com.swathi.queue_app.model.CreateQueueRequest

class Queuerepository {
    suspend fun getAllQueues()= RetrofitInstance.api.getAllQueues().also{
        println("Queue Repository called");
    }
    suspend fun joinQueue(
        queueId: String
    ) = RetrofitInstance.api.joinQueue(queueId)

    suspend fun myStatus(
        queueId: String?
    ) = RetrofitInstance.api.myStatus(queueId)


    suspend fun getMyActiveQueue() =
        RetrofitInstance.api.getMyActiveQueue()

    suspend fun exitQueue(
        queueId: String
    ) = RetrofitInstance.api.exitQueue(queueId)

    suspend fun nextToken(
        queueId: String
    ) = RetrofitInstance.api.nextToken(queueId)

    suspend fun toggleQueueStatus(
        queueId: String
    ) = RetrofitInstance.api.toggleQueueStatus(queueId)

    suspend fun closeQueue(
        queueId: String
    ) = RetrofitInstance.api.closeQueue(queueId)

    suspend fun getAllMembers(
        queueId: String
    ) = RetrofitInstance.api.getAllMembers(queueId)
    suspend fun getQueueDetails(
        queueId: String
    ) = RetrofitInstance.api
        .getQueueDetails(queueId)
    suspend fun createQueue(
        queueName: String,
        queueCapacity: Int,
        queueStatus: String,
    ) = RetrofitInstance.api.createQueue(
        CreateQueueRequest(queueName, queueCapacity ,queueStatus)
    )

    suspend fun getNotificationCount() =
        RetrofitInstance.api.getNotificationCount()

    suspend fun markNotificationsRead() =
        RetrofitInstance.api.markNotificationsRead()


    suspend fun completeCurrent(
        queueId: String
    ) = RetrofitInstance.api.completeCurrent(queueId)

    suspend fun getUserMembers(queueId: String?) =
        RetrofitInstance.api.getUserMembers(queueId)


    suspend fun getNotifications() =
        RetrofitInstance.api.getNotifications()
}