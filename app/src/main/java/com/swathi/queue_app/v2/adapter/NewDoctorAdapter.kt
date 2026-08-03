package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.Doctor

class NewDoctorAdapter(
    private var doctors: List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit
) : RecyclerView.Adapter<NewDoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)

        fun bind(doctor: Doctor) {
            tvDoctorName.text = doctor.name

            itemView.setOnClickListener {
                onDoctorClick(doctor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    fun updateData(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}