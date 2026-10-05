package com.swathi.queue_app.v2.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.AdminItemRecentActivityBinding

import com.swathi.queue_app.v2.models.NotificationModel

class NotificationAdapter(
    private var notifications: List<NotificationModel> = emptyList()
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(
        val binding: AdminItemRecentActivityBinding

    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {

        val binding = AdminItemRecentActivityBinding
            .inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {

        val notification = notifications[position]

        holder.binding.tvActivityTitle.text =
            notification.title

        holder.binding.tvActivitySubtitle.text =
            notification.message

        // Change background based on notification type
        when (notification.type) {


            "QUEUE_CLEARED" -> {
                holder.binding.viewActivityIcon
                    .setBackgroundResource(
                        R.drawable.bg_chip_light_red
                    )
            }

            else -> {
                holder.binding.viewActivityIcon
                    .setBackgroundResource(
                        R.drawable.bg_chip_light_green
                    )
            }
        }
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateNotifications(
        newNotifications: List<NotificationModel>
    ) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}