package com.swathi.queue_app.v2.fragments

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectDepartmentFragment : Fragment(R.layout.fragment_select_department) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DepartmentAdapter
    private lateinit var tokenManager: TokenManager

    private var hospitalId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        hospitalId = tokenManager.getHospitalId() // Retrieve directly from TokenManager

        recyclerView = view.findViewById(R.id.recyclerDepartments)
        progressBar = view.findViewById(R.id.progressBarDepartments)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Only pass the department name forward; hospitalId is safely stored in preferences
        adapter = DepartmentAdapter(emptyList()) { selectedDepartment ->
            val bundle = Bundle().apply {
                putString("departmentName", selectedDepartment)
            }

            val doctorFragment = SelectDoctorFragment().apply {
                arguments = bundle
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, doctorFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        if (!hospitalId.isNullOrEmpty()) {
            fetchDepartments(hospitalId!!)
        } else {
            Toast.makeText(requireContext(), "Hospital ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDepartments(hospitalId: String) {
        progressBar.visibility = View.VISIBLE

        RetrofitClient.instance.getDepartments(hospitalId).enqueue(object : Callback<DepartmentResponse> {
            override fun onResponse(call: Call<DepartmentResponse>, response: Response<DepartmentResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val departments = response.body()?.data ?: emptyList()
                    adapter.updateData(departments)
                } else {
                    Toast.makeText(requireContext(), "Failed to load departments", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}