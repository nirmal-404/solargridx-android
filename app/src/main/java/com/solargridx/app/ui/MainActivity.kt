package com.solargridx.app.ui

import android.R
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.solargridx.app.databinding.ActivityMainBinding
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: DashboardViewModel by viewModels()

    private var isNavigationExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setupUserInfo()
        setupListeners()
        observeViewModel()
    }

    private fun setupUserInfo() {
        val user = sessionManager.fetchUser()
        val displayName = user?.fullName?.ifBlank { null }
            ?: user?.email?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() }
            ?: user?.email
            ?: user?.nic
            ?: "User"

        val role = user?.role?.ifBlank { null } ?: "GridOperator"

        binding.tvWelcomeSubtitle.text =
            "Welcome back, $displayName. Real-time solar microgrid slot bookings and capacity status."
        binding.tvRoleBadge.text = role
    }

    private fun setupListeners() {
        // Toggle fold/unfold navigation when clicking header row or breadcrumb tag
        binding.btnPortalHeader.setOnClickListener {
            toggleNavigationFold()
        }

        binding.tvBreadcrumbTag.setOnClickListener {
            toggleNavigationFold()
        }

        binding.btnBookEnergySlot.setOnClickListener {
            startActivity(Intent(this, CreateReservationActivity::class.java))
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.loadSummary()
        }

        binding.btnQrDispatcher.setOnClickListener {
            startActivity(Intent(this, QrDispatcherActivity::class.java))
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            navigateToLogin()
        }

        // Navigation Menu Item Clicks
        binding.btnNavDashboard.setOnClickListener {
            selectMenuItem("Dashboard")
            viewModel.loadSummary()
        }

        binding.btnNavAllReservations.setOnClickListener {
            selectMenuItem("All Reservations")
            startActivity(Intent(this, CreateReservationActivity::class.java))
        }

        binding.btnNavPendingQueue.setOnClickListener {
            selectMenuItem("Pending Queue")
            Toast.makeText(this, "Pending Queue selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnNavBookingHistory.setOnClickListener {
            selectMenuItem("Booking History")
            Toast.makeText(this, "Booking History selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleNavigationFold() {
        isNavigationExpanded = !isNavigationExpanded
        if (isNavigationExpanded) {
            binding.layoutPortalContent.visibility = View.VISIBLE
            binding.ivPortalChevron.animate().rotation(0f).setDuration(200).start()
            val currentTitle = binding.tvBreadcrumbTag.text.toString().replace(" ▾", "").replace(" ▸", "")
            binding.tvBreadcrumbTag.text = "$currentTitle ▾"
        } else {
            binding.layoutPortalContent.visibility = View.GONE
            binding.ivPortalChevron.animate().rotation(-90f).setDuration(200).start()
            val currentTitle = binding.tvBreadcrumbTag.text.toString().replace(" ▾", "").replace(" ▸", "")
            binding.tvBreadcrumbTag.text = "$currentTitle ▸"
        }
    }

    private fun selectMenuItem(title: String) {
        val arrow = if (isNavigationExpanded) " ▾" else " ▸"
        binding.tvBreadcrumbTag.text = "$title$arrow"

        // Update active highlight styling on menu items
        updateMenuItemStyle(
            binding.btnNavDashboard,
            binding.tvNavDashboard,
            binding.ivNavDashboard,
            isSelect = (title == "Dashboard")
        )
        updateMenuItemStyle(
            binding.btnNavAllReservations,
            binding.tvNavAllReservations,
            binding.ivNavAllReservations,
            isSelect = (title == "All Reservations")
        )
        updateMenuItemStyle(
            binding.btnNavPendingQueue,
            binding.tvNavPendingQueue,
            binding.ivNavPendingQueue,
            isSelect = (title == "Pending Queue")
        )
        updateMenuItemStyle(
            binding.btnNavBookingHistory,
            binding.tvNavBookingHistory,
            binding.ivNavBookingHistory,
            isSelect = (title == "Booking History")
        )
    }

    private fun updateMenuItemStyle(
        container: View,
        textView: TextView,
        imageView: ImageView,
        isSelect: Boolean
    ) {
        if (isSelect) {
            container.setBackgroundColor(Color.parseColor("#18181B"))
            textView.setTextColor(Color.parseColor("#FFFFFF"))
            imageView.setColorFilter(Color.parseColor("#FFFFFF"))
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
            container.setBackgroundResource(typedValue.resourceId)
            textView.setTextColor(Color.parseColor("#3F3F46"))
            imageView.setColorFilter(Color.parseColor("#71717A"))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnRefresh.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.summary.collect { summary ->
                        if (summary != null) {
                            binding.tvPendingBookingsCount.text = summary.pendingBookings.toString()
                            binding.tvApprovedFutureCount.text = summary.approvedFuture.toString()
                            binding.tvActiveSessionsCount.text = summary.activeSessions.toString()

                            summary.userName?.let { name ->
                                if (name.isNotBlank()) {
                                    binding.tvWelcomeSubtitle.text =
                                        "Welcome back, $name. Real-time solar microgrid slot bookings and capacity status."
                                }
                            }

                            summary.role?.let { role ->
                                if (role.isNotBlank()) {
                                    binding.tvRoleBadge.text = role
                                }
                            }
                        } else {
                            binding.tvPendingBookingsCount.text = "0"
                            binding.tvApprovedFutureCount.text = "0"
                            binding.tvActiveSessionsCount.text = "0"
                        }
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
