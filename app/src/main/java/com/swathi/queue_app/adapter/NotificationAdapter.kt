package com.swathi.queue_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.ItemNotificationBinding
import com.swathi.queue_app.model.NotificationModel
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val notifications: List<NotificationModel>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(
        val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {

        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NotificationViewHolder(binding)
    }

    override fun getItemCount() = notifications.size

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {

        val notification = notifications[position]

        holder.binding.tvTitle.text = notification.title
        holder.binding.tvMessage.text = notification.message

        val input = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault()
        )

        val output = SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        )

        try {
            val date = input.parse(notification.createdAt)
            holder.binding.tvTime.text = output.format(date!!)
        } catch (e: Exception) {
            holder.binding.tvTime.text = ""
        }

        when (notification.type) {

            "JOIN" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_check_circle_24)
            }

            "READY_2" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_circle_notifications_24)
            }

            "READY_1" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_circle_notifications_24)
            }

            "TURN" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_campaign_24)
            }

            "PAUSE" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_auto_read_pause_24)
            }

            "RESUME" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.outline_auto_read_pause_24)
            }

            "CLOSE" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.baseline_cancel_24)
            }
        }
    }
}