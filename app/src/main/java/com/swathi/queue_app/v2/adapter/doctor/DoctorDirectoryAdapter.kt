package com.swathi.queue_app.v2.adapter.doctor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.DoctorDirectoryItem

class DoctorDirectoryAdapter(
    private var doctorList: List<DoctorDirectoryItem>,
    private val onViewQueueClick: (DoctorDirectoryItem) -> Unit,
    private val onEditClick: (DoctorDirectoryItem) -> Unit
) : RecyclerView.Adapter<DoctorDirectoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvValueLeft: TextView = itemView.findViewById(R.id.tvValueLeft)
        val tvValueRight: TextView = itemView.findViewById(R.id.tvValueRight)
        val btnActionQueue: Button = itemView.findViewById(R.id.btnActionQueue)
        val btnEditDoctor: ImageButton = itemView.findViewById(R.id.btnEditDoctor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.new_admin_item_doctor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctorList[position]

        holder.tvDoctorName.text = doctor.name
        holder.tvSpecialization.text = doctor.specialization

        // Bind dynamic status mapping (ACTIVE, PAUSED, OFF_DUTY)
        when (doctor.status.uppercase()) {
            "ACTIVE" -> {
                holder.tvStatusBadge.text = "● Active"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#137547"))
            }
            "PAUSED" -> {
                holder.tvStatusBadge.text = "● Paused"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#A53860"))
            }
            else -> {
                holder.tvStatusBadge.text = "● Off Duty"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#6C757D"))
            }
        }

        // Parse embedded active queue tokens if present
        val queue = doctor.activeQueue
        if (queue != null) {
            val waitingCount = queue.tokens?.count {
                val tokenMap = it as? Map<*, *>
                val tokenStatus = tokenMap?.get("status") as? String
                tokenStatus?.uppercase() == "WAITING"
            } ?: 0

            holder.tvValueLeft.text = "$waitingCount Patients"
            holder.tvValueRight.text = queue.queueStatus.lowercase().replaceFirstChar { it.uppercase() }
            holder.btnActionQueue.text = "View Queue"
        } else {
            holder.tvValueLeft.text = "No Queue"
            holder.tvValueRight.text = "Unavailable"
            holder.btnActionQueue.text = "Queue Unavailable"
        }

        holder.btnActionQueue.setOnClickListener { onViewQueueClick(doctor) }
        holder.btnEditDoctor.setOnClickListener { onEditClick(doctor) }
    }

    override fun getItemCount(): Int = doctorList.size

    fun updateData(newList: List<DoctorDirectoryItem>) {
        doctorList = newList
        notifyDataSetChanged()
    }
}