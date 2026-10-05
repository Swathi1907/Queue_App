package com.swathi.queue_app.v2.fragments.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.models.NotificationModel
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AdminDashboardViewModel
import com.swathi.queue_app.v2.viewmodels.NotificationViewModel

class HospitalAdminDashboardFragment :
    Fragment(R.layout.new_admin_dashboard) {

    private val viewModel: AdminDashboardViewModel by viewModels()

    private val notificationViewModel: NotificationViewModel by viewModels()

    private lateinit var tokenManager: TokenManager

    private var isNotificationsExpanded = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // -----------------------------
        // Dashboard UI
        // -----------------------------

        val tvActiveQueues =
            view.findViewById<TextView>(R.id.tvActiveQueues)

        val tvDoctorsOnDuty =
            view.findViewById<TextView>(R.id.tvDoctorsOnDuty)

        val tvPatientsWaiting =
            view.findViewById<TextView>(R.id.tvPatientsWaiting)




        val tvViewAll =
            view.findViewById<TextView>(R.id.tvViewAll)

        // -----------------------------
        // Token Manager
        // -----------------------------

        tokenManager = TokenManager(requireContext())

        // -----------------------------
        // Notification Container
        // -----------------------------

        val notificationContainer =
            view.findViewById<LinearLayout>(
                R.id.notificationContainer
            )

        // -----------------------------
        // Dashboard Stats
        // -----------------------------

        viewModel.dashboardStats.observe(
            viewLifecycleOwner
        ) { stats ->

            stats?.let {

                tvActiveQueues.text =
                    it.activeQueues.toString()

                tvDoctorsOnDuty.text =
                    "${it.doctorsWithQueues} / ${it.totalDoctors} total"

                if (it.totalWaitingPatients > 100) {

                    tvPatientsWaiting.text =
                        "${it.totalWaitingPatients}  ↑ High Load"

                    tvPatientsWaiting.setTextColor(
                        resources.getColor(
                            android.R.color.holo_red_dark,
                            null
                        )
                    )

                } else {

                    tvPatientsWaiting.text =
                        "${it.totalWaitingPatients}  • Normal"

                    tvPatientsWaiting.setTextColor(
                        resources.getColor(
                            android.R.color.darker_gray,
                            null
                        )
                    )
                }
            }
        }

        // -----------------------------
        // Dashboard Errors
        // -----------------------------

        viewModel.errorMessage.observe(
            viewLifecycleOwner
        ) { errorMsg ->

            errorMsg?.let {

                Toast.makeText(
                    requireContext(),
                    it,
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("hsopdash", it)
            }
        }

        // -----------------------------
        // Notification Observer
        // -----------------------------

        notificationViewModel.notifications.observe(
            viewLifecycleOwner
        ) { notifications ->

            showNotifications(
                notifications,
                notificationContainer
            )
        }

        // -----------------------------
        // Notification Error
        // -----------------------------

        notificationViewModel.error.observe(
            viewLifecycleOwner
        ) { errorMsg ->

            errorMsg?.let {

                Log.e(
                    "NOTIFICATION",
                    it
                )

                Toast.makeText(
                    requireContext(),
                    "Failed to load notifications: $it",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // -----------------------------
        // View All / Show Less
        // -----------------------------

        tvViewAll.setOnClickListener {

            isNotificationsExpanded =
                !isNotificationsExpanded

            tvViewAll.text =
                if (isNotificationsExpanded) {
                    "Show Less"
                } else {
                    "View All"
                }

            notificationViewModel.notifications.value?.let {

                showNotifications(
                    it,
                    notificationContainer
                )
            }
        }

        // -----------------------------
        // Quick Actions
        // -----------------------------





        // -----------------------------
        // Fetch Dashboard
        // -----------------------------

        viewModel.fetchDashboardStats()

        // -----------------------------
        // Fetch Notifications
        // -----------------------------

        val role = tokenManager.getRole()

        if (role != null) {

            Log.d(
                "NOTIFICATION",
                "Fetching notifications for role: $role"
            )

            notificationViewModel.getNotifications(role)

        } else {

            Log.e(
                "NOTIFICATION",
                "Role not found in TokenManager"
            )
        }
    }


    // =====================================================
    // SHOW NOTIFICATIONS
    // =====================================================

    private fun showNotifications(
        notifications: List<NotificationModel>,
        container: LinearLayout
    ) {

        container.removeAllViews()

        val displayList =
            if (isNotificationsExpanded) {
                notifications
            } else {
                notifications.take(3)
            }

        for (notification in displayList) {

            val itemView = layoutInflater.inflate(
                R.layout.admin_item_recent_activity,
                container,
                false
            )

            val tvTitle =
                itemView.findViewById<TextView>(
                    R.id.tvActivityTitle
                )

            val tvSubtitle =
                itemView.findViewById<TextView>(
                    R.id.tvActivitySubtitle
                )

            val icon =
                itemView.findViewById<View>(
                    R.id.viewActivityIcon
                )

            tvTitle.text =
                notification.title

            tvSubtitle.text =
                notification.message

            when (notification.type) {

                "QUEUE_STARTED" -> {

                    icon.setBackgroundResource(
                        R.drawable.bg_chip_light_green
                    )
                }

                "QUEUE_CLEARED" -> {

                    icon.setBackgroundResource(
                        R.drawable.bg_chip_light_red
                    )
                }

                else -> {

                    icon.setBackgroundResource(
                        R.drawable.bg_chip_light_green
                    )
                }
            }

            container.addView(itemView)
        }
    }
}