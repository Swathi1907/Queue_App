package com.swathi.queue_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.ItemLiveStatusBinding
import com.swathi.queue_app.model.QueueModel
import com.swathi.queue_app.model.myStatusResponse

class LiveStatusAdapter(
    private val queueList: List<myStatusResponse>
) : RecyclerView.Adapter<LiveStatusAdapter.LiveStatusViewHolder>(){

    inner class LiveStatusViewHolder(
        val binding: ItemLiveStatusBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LiveStatusViewHolder {

        val binding =
            ItemLiveStatusBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return LiveStatusViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LiveStatusViewHolder,
        position: Int
    ) {

        val queue = queueList[position]
        if (!queue.queueStarted) {
            holder.binding.progressQueue.progress = 0

        } else {
            holder.binding.progressQueue.max = queue.activeCount
            holder.binding.progressQueue.progress = queue.progress
        }


        when (queue.queue_status) {

            "WAITING_TO_START" -> {
               holder. binding.tvQueueProgressStatus.text =
                    "⏳ Queue hasn't started yet"
            }

            "WAITING_FOR_NEXT_CALL" -> {
                holder. binding.tvQueueProgressStatus.text =
                    "🟡 You're next. Waiting for the receptionist to call you."
            }

            "WAITING" -> {
                holder. binding.tvQueueProgressStatus.text =
                    "👥 ${queue.peopleAhead} people ahead of you"
            }

            "NEXT" -> {
                holder.  binding.tvQueueProgressStatus.text =
                    "🔔 You're next. Please be ready."
            }

            "SERVING" -> {
                holder.  binding.tvQueueProgressStatus.text =
                    "🟢 It's your turn. Please proceed to the doctor."
            }

            else -> {
                holder. binding.tvQueueProgressStatus.text = ""
            }
        }

        holder.binding.tvQueueName.text = queue.queueName
holder.binding.tvToken.text="You are #${queue.yourToken}"





        val wait = maxOf(0, queue.peopleAhead) * queue.avgServiceTime
        if(queue.peopleAhead<=0){

            holder.binding.tvWait.text = "~${queue.avgServiceTime} min"
        }
 else{
     holder.binding.tvWait.text = "~${wait} min"}
    }

    override fun getItemCount(): Int {
        return queueList.size
    }
}