package com.swathi.queue_app.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.TrackQueuesBinding
import com.swathi.queue_app.model.QueueModel

class QueueAdapter(
    private val queueList: List<QueueModel>,
    private val onJoinclick: (QueueModel) -> Unit,
    private val activeQueueId: String?,

    private val onViewDetails: (QueueModel) -> Unit
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    inner class QueueViewHolder(
        val binding: TrackQueuesBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QueueViewHolder {

        val binding = TrackQueuesBinding.inflate(
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
        Log.d(
            "QUEUE_ADAPTER",
            "queueId=${queue._id}, activeQueueId=$activeQueueId"
        )
        holder.binding.btnjoinqueue.alpha = 1f
        holder.binding.btnjoinqueue.isEnabled = true
        holder.binding.tvQueueName.text =
            queue.queueName

       // holder.binding.tvlastIssued.text =
         //   "Last Issued : ${queue.latestToken}"
        //if (queue.currentToken == null) {
        //    holder.binding.tvCurrentToken.text = "No One Serving"
        //}
        //else{
          //  holder.binding.tvCurrentToken.text =
            //    "Current Token : ${queue.currentToken}"
        //}

        holder.binding.tvQueueStatus.text =
       "${queue.queueStatus}"
        println("${queue.activeCount} in queue");
        holder.binding.tvDoctor.text = queue.doctorName

        holder.binding.tvRoomFloor.text =
            "Room ${queue.roomNumber} • Floor ${queue.floor}"

        holder.binding.tvTiming.text =
            "${queue.startTime} - ${queue.endTime}"

        holder.binding.tvWaitingCount.text =
            queue.waiting_members.toString()

        holder.binding.tvPeopleAhead.text =
            "People in Queue"
        when (queue.queueStatus) {

            "active" -> {
                holder.binding.tvQueueStatus.text = "● Open"
                holder.binding.tvQueueStatus.setBackgroundResource(R.drawable.bg_status_green)
            }

            "paused" -> {
                holder.binding.tvQueueStatus.text = "⏸ Paused"
                holder.binding.tvQueueStatus.setBackgroundResource(R.drawable.bg_status_green)
            }

            "closed" -> {
                holder.binding.tvQueueStatus.text = "● Closed"
                holder.binding.tvQueueStatus.setBackgroundResource(R.drawable.bg_status_green)
            }
        }


            if (activeQueueId != null) {

                if (queue._id == activeQueueId) {

                    holder.binding.btnjoinqueue.text = "View"

                    holder.binding.btnjoinqueue.isEnabled = true

                    holder.binding.btnjoinqueue.setOnClickListener {
                        onViewDetails(queue)
                    }

                } else {

                    holder.binding.btnjoinqueue.text = "+Join"
                    holder.binding.btnjoinqueue.isEnabled = true
                    holder.binding.btnjoinqueue.alpha = 0.7f

                    holder.binding.btnjoinqueue.setOnClickListener {

                        Toast.makeText(
                            holder.itemView.context,
                            "You are already in another queue",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                }
            } else {

                holder.binding.btnjoinqueue.text = "+Join"

                holder.binding.btnjoinqueue.isEnabled = true

                holder.binding.btnjoinqueue.setOnClickListener {
                    onJoinclick(queue)
                }


        }
    }

    override fun getItemCount(): Int {
        return queueList.size
    }
}