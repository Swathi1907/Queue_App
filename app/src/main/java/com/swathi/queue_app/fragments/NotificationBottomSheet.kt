package com.swathi.queue_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.swathi.queue_app.adapter.NotificationAdapter
import com.swathi.queue_app.databinding.BottomNotificationSheetBinding
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel

class NotificationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomNotificationSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomNotificationSheetBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNotifications.layoutManager =
            LinearLayoutManager(requireContext())

        viewModel.getNotifications()

        viewModel.notifications.observe(viewLifecycleOwner) {
            Log.d("NOTIFICATION", "Size = ${it.size}")
            binding.rvNotifications.adapter =
                NotificationAdapter(it)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}