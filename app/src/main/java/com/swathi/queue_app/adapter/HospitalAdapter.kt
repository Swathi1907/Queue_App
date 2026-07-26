package com.swathi.queue_app.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.ItemHospitalBinding
import com.swathi.queue_app.databinding.TrackQueuesBinding
import com.swathi.queue_app.model.HospitalModel
import com.swathi.queue_app.model.QueueModel
/*class HospitalAdapter(

    private val hospitals: List<HospitalModel>,
    private val onClick: (HospitalModel) -> Unit

) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() { */
class HospitalAdapter(
    hospitals: List<HospitalModel>,
    private val onClick: (HospitalModel) -> Unit
) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>(), Filterable {

    private val hospitalList = hospitals.toMutableList()
    private var filteredList = hospitals.toMutableList()

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

        val hospital = filteredList[position]
        Log.d("FILTER", "Binding ${hospital.hospitalName}")
        holder.binding.tvHospitalName.text =
            hospital.hospitalName

        holder.binding.tvAddress.text =
            "${hospital.address}, ${hospital.city}"

        holder.binding.root.setOnClickListener {

            onClick(hospital)

        }
    }


        override fun getItemCount() = filteredList.size

    override fun getFilter(): Filter {

        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {


                val query = constraint
                    ?.toString()
                    ?.replace("\n", "")
                    ?.trim()
                    ?.lowercase()
                    ?: ""

                Log.d("FILTER", "Query = '$query'")
                Log.d("FILTER", "Hospital List Size = ${hospitalList.size}")
                filteredList =
                    if (query.isEmpty()) {
                        hospitalList.toMutableList()
                    } else {
                        hospitalList.forEach {
                            Log.d("FILTER", "Hospital = ${it.hospitalName}")
                        }
                        hospitalList.filter {
                            it.hospitalName.lowercase().contains(query) ||
                                    it.city.lowercase().contains(query)
                        }.toMutableList()
                    }

                Log.d("FILTER", "Filtered Size = ${filteredList.size}")
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                filteredList = results?.values as MutableList<HospitalModel>
                notifyDataSetChanged()
            }
        }
    }
}