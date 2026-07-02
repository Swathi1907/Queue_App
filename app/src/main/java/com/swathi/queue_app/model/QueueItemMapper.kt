package com.swathi.queue_app.model

object QueueItemMapper {

    fun map(list: List<MemberModel>): List<QueueItem> {

        val result = mutableListOf<QueueItem>()

        var i = 0

        while (i < list.size) {

            val member = list[i]

            if (member.status == "cancelled") {

                val tokens = mutableListOf<String>()

                while (
                    i < list.size &&
                    list[i].status == "cancelled"
                ) {
                    tokens.add(list[i].tokenNumber.toString())
                    i++
                }

                result.add(
                    QueueItem.CancelledItem(
                        tokens.joinToString(", ")
                    )
                )

            } else {

                result.add(
                    QueueItem.MemberItem(member)
                )

                i++
            }
        }

        return result
    }
}