package com.swathi.queue_app.v2.adapter.doctor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.DoctorItemDepartmentBinding

class DoctorDepartmentAdapter(
    private var departments: List<String>,
    private val onDepartmentClick: (String) -> Unit
) : RecyclerView.Adapter<DoctorDepartmentAdapter.DepartmentViewHolder>() {

    inner class DepartmentViewHolder(val binding: DoctorItemDepartmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = DoctorItemDepartmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DepartmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        val department = departments[position]
        holder.binding.tvDepartmentName.text = department

        holder.itemView.setOnClickListener {
            onDepartmentClick(department)
        }
    }

    override fun getItemCount(): Int = departments.size

    // Add this helper function to refresh the data safely
    fun updateDepartments(newDepartments: List<String>) {
        departments = newDepartments
        notifyDataSetChanged()
    }
}