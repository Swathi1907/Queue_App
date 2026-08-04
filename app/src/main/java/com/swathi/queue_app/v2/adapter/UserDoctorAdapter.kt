package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.UserDoctorItem

class UserDoctorAdapter(
    private var doctors: List<UserDoctorItem>,
    private val onJoinQueueClick: (UserDoctorItem) -> Unit
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
            .inflate(R.layout.item_dept_doctor, parent, false) // Uses your doctor card XML
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvDoctorName.text = doctor.name
        holder.tvDoctorSpecialty.text = doctor.specialty
        holder.tvPeopleAhead.text = doctor.peopleAhead.toString()
        holder.tvEstimatedWait.text = doctor.estimatedWaitTime


        // Trigger action when "Join Queue" is tapped
        holder.btnJoinQueue.setOnClickListener {
            onJoinQueueClick(doctor)
        }
    }

    override fun getItemCount(): Int = doctors.size

    fun updateData(newDoctors: List<UserDoctorItem>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}