package com.swathi.queue_app.v2.adapter.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.NewAdminItemDepartmentBinding
import com.swathi.queue_app.v2.models.GlobalQueueItem

class GlobalQueueAdapter(
    private var queueList: List<GlobalQueueItem>,
    private val onItemClick: (GlobalQueueItem) -> Unit
) : RecyclerView.Adapter<GlobalQueueAdapter.QueueViewHolder>() {

    inner class QueueViewHolder(val binding: NewAdminItemDepartmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GlobalQueueItem) {
            binding.tvDepartmentName.text = item.departmentName
            binding.tvDepartmentLocation.text = item.location
            binding.tvWaitingCount.text = item.waitingCount.toString()
            binding.tvdoctors.text = item.assignedDoctorsCount.toString()

            when (item.loadStatus.uppercase()) {
                "HIGH_LOAD" -> {
                    binding.tvLoadStatusBadge.text = "● High Load"
                    binding.tvLoadStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                    binding.tvWaitingCount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                }
                "MODERATE" -> {
                    binding.tvLoadStatusBadge.text = "● Moderate"
                    binding.tvLoadStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                    binding.tvWaitingCount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                }
                else -> {
                    binding.tvLoadStatusBadge.text = "● Normal"
                    binding.tvLoadStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                    binding.tvWaitingCount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_blue_dark))
                }
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = NewAdminItemDepartmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        holder.bind(queueList[position])
    }

    override fun getItemCount(): Int = queueList.size

    fun updateData(newList: List<GlobalQueueItem>) {
        queueList = newList
        notifyDataSetChanged()
    }
}