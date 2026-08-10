package com.swathi.queue_app.v2.fragments.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.viewmodels.AdminDashboardViewModel

class HospitalAdminDashboardFragment : Fragment(R.layout.new_admin_dashboard) {

    private val viewModel: AdminDashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. UI References for Dashboard Cards
        val tvActiveQueues = view.findViewById<TextView>(R.id.tvActiveQueues)
        val tvDoctorsOnDuty = view.findViewById<TextView>(R.id.tvDoctorsOnDuty)
        val tvPatientsWaiting = view.findViewById<TextView>(R.id.tvPatientsWaiting)

        // 2. UI References for Action Buttons & Navigation Links
        val btnManageStaff = view.findViewById<MaterialButton>(R.id.btnManageStaff)
        val btnAdjustCapacity = view.findViewById<MaterialButton>(R.id.btnAdjustCapacity)
        val btnBroadcast = view.findViewById<MaterialButton>(R.id.btnBroadcast)
        val tvViewAll = view.findViewById<TextView>(R.id.tvViewAll)

        // 3. Observe Dashboard Stats LiveData
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                tvActiveQueues.text = it.activeQueues.toString()
                tvDoctorsOnDuty.text = "${it.doctorsWithQueues} / ${it.totalDoctors} total"

                // Dynamic high load formatting matching your design layout
                // Dynamic high load formatting matching your design layout
                if (it.totalWaitingPatients > 100) {
                    tvPatientsWaiting.text = "${it.totalWaitingPatients}  ↑ High Load"
                    tvPatientsWaiting.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                } else {
                    tvPatientsWaiting.text = "${it.totalWaitingPatients}  • Normal"
                    // Use a standard dark or grey color resource for normal load
                    tvPatientsWaiting.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
            }
        }

        // 4. Observe Error States
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                println("entered")
                Log.d("hsopdash","${it}")
            }

        }

        // 5. Quick Action Button Click Listeners
        //btnManageStaff.setOnClickListener {
          //  findNavController().navigate(R.id.doctors)
        //}//

        btnAdjustCapacity.setOnClickListener {
            Toast.makeText(requireContext(), "Opening capacity adjustment settings...", Toast.LENGTH_SHORT).show()
        }

        btnBroadcast.setOnClickListener {
            Toast.makeText(requireContext(), "Opening broadcast options...", Toast.LENGTH_SHORT).show()
        }

        tvViewAll.setOnClickListener {
            Toast.makeText(requireContext(), "Viewing all recent activities...", Toast.LENGTH_SHORT).show()
        }

        // 6. Trigger data fetch on fragment load
        viewModel.fetchDashboardStats()
    }
}