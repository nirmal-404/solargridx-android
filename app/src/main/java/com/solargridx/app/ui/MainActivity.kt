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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = 0
            )
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            windowInsets
        }

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
        setupBottomNavigation()

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            navigateToLogin()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.tvRoleBadge.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardPendingBookings.setOnClickListener {
            val intent = Intent(this, ReservationsActivity::class.java).apply {
                putExtra(ReservationsActivity.EXTRA_FILTER, "pending")
            }
            startActivity(intent)
        }

        binding.cardApprovedFuture.setOnClickListener {
            val intent = Intent(this, ReservationsActivity::class.java).apply {
                putExtra(ReservationsActivity.EXTRA_FILTER, "approved")
            }
            startActivity(intent)
        }

        binding.cardActiveSessions.setOnClickListener {
            val intent = Intent(this, ReservationsActivity::class.java).apply {
                putExtra(ReservationsActivity.EXTRA_FILTER, "history")
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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

    fun onDashboardReselected() {
        binding.nestedScrollView.smoothScrollTo(0, 0)
        viewModel.loadSummary()
    }

    private fun setupBottomNavigation() {
        com.solargridx.app.utils.BottomNavigationHelper.setup(
            this,
            binding.bottomNavigation,
            com.solargridx.app.R.id.nav_dashboard
        )
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = com.solargridx.app.R.id.nav_dashboard
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
