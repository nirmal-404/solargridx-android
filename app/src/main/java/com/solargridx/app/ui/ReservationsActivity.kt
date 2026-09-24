package com.solargridx.app.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.solargridx.app.R
import com.solargridx.app.adapters.ReservationAdapter
import com.solargridx.app.databinding.ActivityReservationsBinding
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.repositories.ReservationRepository
import com.solargridx.app.utils.BottomNavigationHelper
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch

class ReservationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: ReservationRepository
    private lateinit var adapter: ReservationAdapter

    private var allReservationsList = listOf<ReservationResponse>()
    private var currentFilterTab = "all" // "all", "pending", "approved", "history"
    private var currentSearchQuery = ""

    companion object {
        const val EXTRA_FILTER = "extra_filter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReservationsBinding.inflate(layoutInflater)
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
        repository = ReservationRepository(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentFilterTab = intent.getStringExtra(EXTRA_FILTER) ?: "all"

        setupRecyclerView()
        setupListeners()
        setupBottomNav()
        selectFilterTab(currentFilterTab)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_history
        loadReservations()
    }

    private fun setupBottomNav() {
        BottomNavigationHelper.setup(
            this,
            binding.bottomNavigation,
            R.id.nav_history
        )
    }

    private fun setupRecyclerView() {
        val currentUserRole = sessionManager.fetchUser()?.role ?: "Prosumer"

        adapter = ReservationAdapter(
            reservations = emptyList(),
            currentUserRole = currentUserRole,
            onCancelClick = { reservation -> confirmCancelReservation(reservation) },
            onApproveClick = { reservation -> approveReservation(reservation) },
            onViewQrClick = { reservation -> openQrPass(reservation) },
            onItemClick = { reservation -> showReservationDetails(reservation) }
        )

        binding.rvReservations.layoutManager = LinearLayoutManager(this)
        binding.rvReservations.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnNewBooking.setOnClickListener {
            startActivity(Intent(this, CreateReservationActivity::class.java))
        }

        binding.btnRefresh.setOnClickListener {
            loadReservations()
        }

        binding.chipAll.setOnClickListener { selectFilterTab("all") }
        binding.chipPending.setOnClickListener { selectFilterTab("pending") }
        binding.chipApproved.setOnClickListener { selectFilterTab("approved") }
        binding.chipHistory.setOnClickListener { selectFilterTab("history") }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun selectFilterTab(tab: String) {
        currentFilterTab = tab
        updateChipStyle(binding.chipAll, isSelected = (tab == "all"))
        updateChipStyle(binding.chipPending, isSelected = (tab == "pending"))
        updateChipStyle(binding.chipApproved, isSelected = (tab == "approved"))
        updateChipStyle(binding.chipHistory, isSelected = (tab == "history"))
        applyFilterAndSearch()
    }

    private fun updateChipStyle(chip: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            chip.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#18181B"))
            chip.setTextColor(Color.parseColor("#FFFFFF"))
            chip.strokeWidth = 0
        } else {
            chip.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            chip.setTextColor(Color.parseColor("#71717A"))
            chip.strokeColor = ColorStateList.valueOf(Color.parseColor("#E4E4E7"))
            chip.strokeWidth = 2
        }
    }

    private fun loadReservations() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRefresh.isEnabled = false

        lifecycleScope.launch {
            val result = repository.getReservations()
            binding.progressBar.visibility = View.GONE
            binding.btnRefresh.isEnabled = true

            result.onSuccess { list ->
                allReservationsList = list
                applyFilterAndSearch()
            }.onFailure { ex ->
                Toast.makeText(
                    this@ReservationsActivity,
                    "Failed to fetch bookings: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
                applyFilterAndSearch()
            }
        }
    }

    private fun applyFilterAndSearch() {
        var filtered = allReservationsList

        // Apply Tab Filter
        filtered = when (currentFilterTab) {
            "pending" -> filtered.filter { it.status.equals("Pending", ignoreCase = true) }
            "approved" -> filtered.filter { it.status.equals("Approved", ignoreCase = true) }
            "history" -> filtered.filter {
                it.status.equals("Completed", ignoreCase = true) ||
                it.status.equals("Cancelled", ignoreCase = true) ||
                it.status.equals("Expired", ignoreCase = true)
            }
            else -> filtered
        }

        // Apply Search Query
        if (currentSearchQuery.isNotEmpty()) {
            val q = currentSearchQuery.lowercase()
            filtered = filtered.filter { item ->
                item.displayId.lowercase().contains(q) ||
                (item.stationId ?: "").lowercase().contains(q) ||
                (item.slotId ?: "").lowercase().contains(q) ||
                (item.status ?: "").lowercase().contains(q) ||
                (item.prosumerNic ?: "").lowercase().contains(q)
            }
        }

        adapter.updateList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvReservations.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvReservations.visibility = View.VISIBLE
        }
    }

    private fun confirmCancelReservation(reservation: ReservationResponse) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Reservation")
            .setMessage("Are you sure you want to cancel booking ${reservation.displayId}?\n\nNote: Cancellations require at least 12 hours' notice before slot start.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelReservation(reservation.displayId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelReservation(id: String) {
        lifecycleScope.launch {
            val result = repository.cancelReservation(id)
            result.onSuccess {
                Toast.makeText(this@ReservationsActivity, "Reservation $id cancelled successfully", Toast.LENGTH_SHORT).show()
                loadReservations()
            }.onFailure { ex ->
                Toast.makeText(this@ReservationsActivity, "Cancellation failed: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun approveReservation(reservation: ReservationResponse) {
        lifecycleScope.launch {
            val result = repository.approveReservation(reservation.displayId)
            result.onSuccess {
                Toast.makeText(this@ReservationsActivity, "Reservation ${reservation.displayId} approved! QR token issued.", Toast.LENGTH_SHORT).show()
                loadReservations()
            }.onFailure { ex ->
                Toast.makeText(this@ReservationsActivity, "Approval failed: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openQrPass(reservation: ReservationResponse) {
        val intent = Intent(this, QrDispatcherActivity::class.java).apply {
            putExtra(QrDispatcherActivity.EXTRA_RESERVATION_ID, reservation.displayId)
            putExtra(QrDispatcherActivity.EXTRA_STATION_ID, reservation.stationId)
            putExtra(QrDispatcherActivity.EXTRA_CAPACITY, reservation.requestedCapacity ?: 0.0)
        }
        startActivity(intent)
    }

    private fun showReservationDetails(reservation: ReservationResponse) {
        val details = StringBuilder()
            .append("Reservation ID: ${reservation.displayId}\n")
            .append("Station ID: ${reservation.stationId ?: "N/A"}\n")
            .append("Slot ID: ${reservation.slotId ?: "N/A"}\n")
            .append("Capacity: ${reservation.requestedCapacity ?: 0.0} kWh\n")
            .append("Status: ${reservation.status ?: "Pending"}\n")
            .append("Schedule: ${reservation.scheduledStartTime?.replace("T", " ") ?: "N/A"} to ${reservation.scheduledEndTime?.replace("T", " ") ?: "N/A"}\n")

        if (!reservation.prosumerNic.isNullOrBlank()) {
            details.append("Prosumer NIC: ${reservation.prosumerNic}\n")
        }
        if (!reservation.transactionId.isNullOrBlank()) {
            details.append("Transaction: ${reservation.transactionId}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .show()
    }
}
