package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.UserItemHistoryQueuesBinding // Ensure this matches your layout file name
import com.swathi.queue_app.v2.models.HistoryItemDto

class QueueHistoryAdapter(
    private var historyList: List<HistoryItemDto>
) : RecyclerView.Adapter<QueueHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: UserItemHistoryQueuesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = UserItemHistoryQueuesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        with(holder.binding) {
            tvClinicName.text = item.hospitalName
            tvClinicSub.text = item.subText
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newList: List<HistoryItemDto>) {
        historyList = newList
        notifyDataSetChanged()
    }
}