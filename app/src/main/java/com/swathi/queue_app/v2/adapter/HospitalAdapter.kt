package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.UserHospitalCardBinding
import com.swathi.queue_app.v2.models.Hospital

class HospitalAdapter(
    private var hospitalList: List<Hospital>,
    private val onHospitalClick: (Hospital) -> Unit
) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    inner class HospitalViewHolder(private val binding: UserHospitalCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(hospital: Hospital) {
            binding.tvHospitalName.text = hospital.name

            // Format the nested address into a clean display string matching tvAdress ID

            binding.tvAdress.text = hospital.address

            // Optional placeholders for UI elements present in your card XML
            binding.tvRating.text = "4.8 (124 reviews)"

            // Handle item click on the card or specific details button
            binding.root.setOnClickListener {
                onHospitalClick(hospital)
            }

            binding.btnDetails.setOnClickListener {
                onHospitalClick(hospital)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val binding = UserHospitalCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HospitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        holder.bind(hospitalList[position])
    }

    override fun getItemCount(): Int = hospitalList.size

    fun updateData(newList: List<Hospital>) {
        hospitalList = newList
        notifyDataSetChanged()
    }
}