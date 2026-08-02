package com.swathi.queue_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.DoctorInfoBinding
import com.swathi.queue_app.model.DoctorModel

class DoctorAdapter(
    private val doctorList: List<DoctorModel>,
    private val isAdmin: Boolean,
    private val onEdit: (DoctorModel) -> Unit,
    private val onDelete: (DoctorModel) -> Unit
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
if(!isAdmin){
    holder.binding.btnMore.visibility = View.GONE
}
        with(holder.binding) {
            if(isAdmin) {
                holder.binding.btnMore.setOnClickListener {

                    val popup = PopupMenu(holder.itemView.context, holder.binding.btnMore)

                    popup.menu.add("Edit")
                    popup.menu.add("Delete")

                    popup.setOnMenuItemClickListener {

                        when (it.title) {

                            "Edit" -> {
                                onEdit(doctor)
                                // Open edit doctor dialog
                                true
                            }

                            "Delete" -> {
                                // Delete doctor
                                onDelete(doctor)
                                true
                            }

                            else -> false
                        }
                    }

                    popup.show()
                }
            }
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