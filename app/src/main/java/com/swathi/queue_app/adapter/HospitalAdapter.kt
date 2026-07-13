package com.swathi.queue_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.ItemHospitalBinding
import com.swathi.queue_app.databinding.TrackQueuesBinding
import com.swathi.queue_app.model.HospitalModel
import com.swathi.queue_app.model.QueueModel
class HospitalAdapter(

    private val hospitals: List<HospitalModel>,
    private val onClick: (HospitalModel) -> Unit

) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    inner class HospitalViewHolder(
        val binding: ItemHospitalBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HospitalViewHolder {

        val binding = ItemHospitalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return HospitalViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HospitalViewHolder,
        position: Int
    ) {

        val hospital = hospitals[position]

        holder.binding.tvHospitalName.text =
            hospital.hospitalName

        holder.binding.tvAddress.text =
            "${hospital.address}, ${hospital.city}"

        holder.binding.root.setOnClickListener {

            onClick(hospital)

        }
    }

    override fun getItemCount() =
        hospitals.size
}