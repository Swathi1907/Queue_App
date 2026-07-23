package com.swathi.queue_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.DoctorInfoBinding
import com.swathi.queue_app.model.DoctorModel

class DoctorAdapter(
    private val doctorList: List<DoctorModel>
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(val binding: DoctorInfoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DoctorViewHolder {

        val binding = DoctorInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DoctorViewHolder,
        position: Int
    ) {

        val doctor = doctorList[position]

        with(holder.binding) {

            tvDoctorName.text = doctor.doctorName

            tvSpecialization.text = doctor.specialization

            tvQualification.text = doctor.qualification

            tvRoom.text = "Room ${doctor.roomNumber}"

            tvDays.text = doctor.availableDays.joinToString(" • ")

            tvTiming.text = "${doctor.startTime} - ${doctor.endTime}"
        }
    }

    override fun getItemCount(): Int = doctorList.size
}