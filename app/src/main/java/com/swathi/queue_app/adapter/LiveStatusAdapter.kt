package com.swathi.queue_app.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.ItemLiveStatusBinding
import com.swathi.queue_app.model.ActiveQueueResponse
import com.swathi.queue_app.model.QueueModel
import com.swathi.queue_app.model.myStatusResponse

class LiveStatusAdapter(
    private val queueList: List<ActiveQueueResponse>,
    private val onClick: (ActiveQueueResponse) -> Unit
) : RecyclerView.Adapter<LiveStatusAdapter.LiveStatusViewHolder>() {

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
        Log.d("Live status Adapter", "On bind view called");

        val queue = queueList[position]
       /* if (!queue.queueStarted) {
            holder.binding.progressQueue.progress = 0

        } else {
            holder.binding.progressQueue.max = queue.activeCount
            holder.binding.progressQueue.progress = queue.progress
        } */




        if (!queue.queueStarted && queue.lastCompletedToken == 0) {
            // Queue has never started
            holder.binding.progressQueue.progress = 0
        } else {
            holder.binding.progressQueue.max = queue.totalPeople
            holder.binding.progressQueue.progress = queue.progress
        }



        Log.d("Adapter", queue.queue_status)
        holder.itemView.setOnClickListener {
            onClick(queue)
        }
        when (queue.queue_status) {

            "WAITING_TO_START" -> {
                holder.binding.tvQueueProgressStatus.text =
                    "⏳ Queue hasn't started yet"
            }

            "WAITING_FOR_NEXT_CALL" -> {
                holder.binding.tvQueueProgressStatus.text =
                    "🟡 You're next. Waiting for the receptionist to call you."
            }

            "WAITING" -> {
                holder.binding.tvQueueProgressStatus.text =
                    "👥 ${queue.peopleAhead} people ahead of you"
            }

            "NEXT" -> {
                holder.binding.tvQueueProgressStatus.text =
                    "🔔 You're next. Please be ready."
            }

            "SERVING" -> {
                holder.binding.tvQueueProgressStatus.text =
                    "🟢 It's your turn. Please proceed to the doctor."
            }

            else -> {
                holder.binding.tvQueueProgressStatus.text = " "
            }
        }

        holder.binding.tvQueueName.text = queue.queueName
        holder.binding.tvToken.text = "You are #${queue.yourToken}"

        holder.binding.tvHospitalName.text = queue.hospitalName
        holder.binding.tvBranchName.text = queue.branchName




        if (queue.esttime == -1) {
            holder.binding.tvWait.text = ""
        }

    else
    {

        holder.binding.tvWait.text = "~${queue.esttime} min"
    }

}
    override fun getItemCount(): Int {

            Log.d("ADAPTER", "Size = ${queueList.size}")


        return queueList.size
    }
}