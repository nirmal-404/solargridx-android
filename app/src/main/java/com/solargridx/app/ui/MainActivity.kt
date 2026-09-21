package com.solargridx.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.solargridx.app.R
import com.solargridx.app.adapters.ReservationAdapter
import com.solargridx.app.databinding.ActivityMainBinding
import com.solargridx.app.network.RetrofitClient
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: ReservationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        RetrofitClient.tokenProvider = { sessionManager.fetchAuthToken() }

        val user = sessionManager.fetchUser()
        val userEmail = sessionManager.fetchUserEmail()
        val role = user?.role ?: "Prosumer"

        binding.tvHeaderSubtitle.text = "$role • ${userEmail ?: "User"}"

        // Settings View Info
        binding.tvSettingsName.text = user?.fullName ?: userEmail ?: "User"
        binding.tvSettingsEmail.text = userEmail ?: "Not available"
        binding.tvSettingsRole.text = "Role: $role"

        val isStaff = role.equals("GridOperator", ignoreCase = true) ||
                      role.equals("Operator", ignoreCase = true) ||
                      role.equals("Backoffice", ignoreCase = true)

        setupRecyclerView(isStaff)
        setupNavigation()
        setupListeners()
        observeViewModel()

        viewModel.loadDashboardData()
    }

    private fun setupRecyclerView(isStaff: Boolean) {
        adapter = ReservationAdapter(
            isStaff = isStaff,
            onCancelClick = { reservation ->
                Toast.makeText(this, "Cancelling ${reservation.reservationId}...", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvRecentBookings.layoutManager = LinearLayoutManager(this)
        binding.rvRecentBookings.adapter = adapter
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showDashboardView()
                    true
                }
                R.id.nav_queue -> {
                    showDashboardView()
                    Toast.makeText(this, "Refreshing Queue...", Toast.LENGTH_SHORT).show()
                    viewModel.loadDashboardData()
                    true
                }
                R.id.nav_book_slot -> {
                    val intent = Intent(this, CreateBookingActivity::class.java)
                    startActivity(intent)
                    false
                }
                R.id.nav_settings -> {
                    showSettingsView()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDashboardView() {
        binding.layoutDashboard.visibility = View.VISIBLE
        binding.layoutSettings.visibility = View.GONE
    }

    private fun showSettingsView() {
        binding.layoutDashboard.visibility = View.GONE
        binding.layoutSettings.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnHeaderSettings.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_settings
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.loadDashboardData()
        }

        binding.btnBookSlot.setOnClickListener {
            val intent = Intent(this, CreateBookingActivity::class.java)
            startActivity(intent)
        }

        binding.btnPendingQueue.setOnClickListener {
            Toast.makeText(this, "Refreshing pending reservation queue...", Toast.LENGTH_SHORT).show()
            viewModel.loadDashboardData()
        }

        binding.btnLogoutSettings.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DashboardUiState.Idle -> showLoading(false)
                        is DashboardUiState.Loading -> showLoading(true)
                        is DashboardUiState.Success -> {
                            showLoading(false)
                            binding.tvPendingCount.text = state.summary.pendingReservations.toString()
                            binding.tvApprovedCount.text = state.summary.approvedFutureReservations.toString()
                            binding.tvActiveCount.text = state.summary.currentReservations.toString()
                            binding.tvCompletedCount.text = state.summary.completedReservations.toString()
                            adapter.submitList(state.recentReservations)
                        }
                        is DashboardUiState.Error -> {
                            showLoading(false)
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
