package com.swathi.queue_app.adapter.admin
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

import com.swathi.queue_app.databinding.ItemQueuesBinding

import com.swathi.queue_app.model.QueueModel
class AdminQueueAdapter(
    private val queueList: List<QueueModel>,
    private val onManageClick: (QueueModel) -> Unit
) : RecyclerView.Adapter<AdminQueueAdapter.QueueViewHolder>() {

    inner class QueueViewHolder(
        val binding: ItemQueuesBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QueueViewHolder {

        val binding = ItemQueuesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QueueViewHolder,
        position: Int
    ) {

        val queue = queueList[position]
holder.binding.tvWaitingCount.text=queue.waiting_members.toString()
        holder.binding.tvQueueName.text =
            queue.queueName
        if (queue.currentToken == null) {
           holder. binding.tvCurrentToken.text = "No One Serving"}
        else {
            holder.binding.tvCurrentToken.text =
                "${queue.currentToken}"
        }

        holder.binding.tvLatestToken.text =
            "${queue.latestToken}"

        holder.binding.tvStatus.text =
            "${queue.queueStatus}"

        holder.binding.btnManage.setOnClickListener {

            onManageClick(queue)
        }
    }

    override fun getItemCount() =
        queueList.size
}