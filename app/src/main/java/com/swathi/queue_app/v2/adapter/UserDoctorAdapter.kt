package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.UserDoctorItem

class UserDoctorAdapter(
    private var doctors: List<UserDoctorItem>,
    private val onJoinQueueClick: (UserDoctorItem) -> Unit,
    private val onViewDashboardClick: (UserDoctorItem) -> Unit // Added callback for already joined queues
) : RecyclerView.Adapter<UserDoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvDoctorSpecialty: TextView = itemView.findViewById(R.id.tvDoctorSpecialty)
        val tvPeopleAhead: TextView = itemView.findViewById(R.id.tvPeopleAhead)
        val tvEstimatedWait: TextView = itemView.findViewById(R.id.tvEstimatedWait)
        val btnJoinQueue: Button = itemView.findViewById(R.id.btnJoinQueue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dept_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]
        val context = holder.itemView.context // Properly reference context from itemView

        holder.tvDoctorName.text = doctor.name
        holder.tvDoctorSpecialty.text = doctor.specialty
        holder.tvPeopleAhead.text = doctor.peopleAhead.toString()
        holder.tvEstimatedWait.text = doctor.estimatedWaitTime

        if (doctor.isJoined) {
            holder.btnJoinQueue.text = "View Queue"
            holder.btnJoinQueue.setBackgroundColor(
                ContextCompat.getColor(context, android.R.color.holo_green_dark)
            )
            holder.btnJoinQueue.setOnClickListener {
                onViewDashboardClick(doctor)
            }
        } else {
            holder.btnJoinQueue.text = "Join Queue"

            holder.btnJoinQueue.setOnClickListener {
                onJoinQueueClick(doctor)
            }
        }
    }

    override fun getItemCount(): Int = doctors.size

    fun updateData(newDoctors: List<UserDoctorItem>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}