package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.UserItemActiveQueuesBinding // Ensure this matches your layout file name
import com.swathi.queue_app.v2.models.ActiveQueueDto

class ActiveQueueAdapter(
    private var activeQueues: List<ActiveQueueDto>,
    private val onQrClick: (String) -> Unit
) : RecyclerView.Adapter<ActiveQueueAdapter.ActiveQueueViewHolder>() {

    inner class ActiveQueueViewHolder(val binding: UserItemActiveQueuesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveQueueViewHolder {
        val binding = UserItemActiveQueuesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActiveQueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActiveQueueViewHolder, position: Int) {
        val item = activeQueues[position]
        with(holder.binding) {
            tvHospitalName.text = item.hospitalName
            tvDoctorDetails.text = item.doctorDetails
            tvQueueStatus.text = item.peopleAheadText
            tvEstWait.text = item.estWaitTimeText

            // Handle active/paused status text and background styling dynamically
            tvStatusText.text = item.status
            if (item.status.equals("PAUSED", ignoreCase = true)) {
                badgeActive.setCardBackgroundColor(
                    ContextCompat.getColor(badgeActive.context, android.R.color.holo_orange_light)
                )
                tvStatusText.setTextColor(
                    ContextCompat.getColor(tvStatusText.context, android.R.color.holo_orange_dark)
                )
            } else {
                badgeActive.setCardBackgroundColor(
                    ContextCompat.getColor(badgeActive.context, android.R.color.holo_green_light)
                )
                tvStatusText.setTextColor(
                    ContextCompat.getColor(tvStatusText.context, android.R.color.holo_green_dark)
                )
            }

            // QR Code button click action
            btnViewQrCode.setOnClickListener {
                onQrClick(item.queueId)
            }
        }
    }

    override fun getItemCount(): Int = activeQueues.size

    fun updateData(newList: List<ActiveQueueDto>) {
        activeQueues = newList
        notifyDataSetChanged()
    }
}