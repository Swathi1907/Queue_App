package com.swathi.queue_app.v2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.DepartmentItem

class DepartmentAdapter(
    private var departments: List<DepartmentItem>,
    private val onDepartmentClick: (DepartmentItem) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.DepartmentViewHolder>() {

    inner class DepartmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDepartmentName: TextView = itemView.findViewById(R.id.tvDepartmentName)
        //val tvWaitingCount: TextView = itemView.findViewById(R.id.tvWaitingCount)

        fun bind(department: DepartmentItem) {
            tvDepartmentName.text = department.name
         //   tvWaitingCount.text = "${department.waitingCount} waiting"

            itemView.setOnClickListener {
                onDepartmentClick(department)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_department, parent, false)
        return DepartmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.bind(departments[position])
    }

    override fun getItemCount(): Int = departments.size

    fun updateData(newDepartments: List<DepartmentItem>) {
        departments = newDepartments
        notifyDataSetChanged()
    }
}