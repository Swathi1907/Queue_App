package com.swathi.queue_app.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.AdminItemActiveQueueBinding
import com.swathi.queue_app.model.adminactivequeues

class ActiveQueueAdapter(
    private val queueList: List<adminactivequeues>,
    private val onClick: (adminactivequeues) -> Unit
) : RecyclerView.Adapter<ActiveQueueAdapter.ActiveQueueViewHolder>() {

    inner class ActiveQueueViewHolder(
        val binding: AdminItemActiveQueueBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActiveQueueViewHolder {

        val binding = AdminItemActiveQueueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ActiveQueueViewHolder(binding)
    }

    override fun getItemCount(): Int = queueList.size

    override fun onBindViewHolder(
        holder: ActiveQueueViewHolder,
        position: Int
    ) {

        val queue = queueList[position]
        println("status${queue.status}")
        holder.binding.tvStatus.text = queue.status
        holder.binding.tvQueueName.text = queue.queueName
        holder.binding.tvWaiting.text = "${queue.waitingCount} Waiting"

        holder.binding.tvServing.text =
            queue.servingToken?.let {
                "Serving #$it"
            } ?: "No one serving"

        holder.binding.tvStatus.text = queue.status

        holder.itemView.setOnClickListener {
            onClick(queue)
        }
    }
}