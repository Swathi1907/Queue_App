package com.swathi.queue_app.model


sealed class QueueItem {

    data class MemberItem(
        val member: MemberModel
    ) : QueueItem()

    data class CancelledItem(
        val tokens: String
    ) : QueueItem()
}