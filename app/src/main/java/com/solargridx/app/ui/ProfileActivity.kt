package com.solargridx.app.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.solargridx.app.R
import com.solargridx.app.databinding.ActivityProfileBinding
import com.solargridx.app.models.UpdateProsumerRequest
import com.solargridx.app.models.User
import com.solargridx.app.repositories.ProsumerRepository
import com.solargridx.app.utils.BottomNavigationHelper
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var prosumerRepository: ProsumerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
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
        prosumerRepository = ProsumerRepository(this)

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setupUI()
        setupListeners()
        loadServerProfile()
    }

    private fun setupUI() {
        BottomNavigationHelper.setup(
            this,
            binding.bottomNavigation,
            0 // No active bottom nav tab explicitly highlighted for profile, or keeps previous
        )

        val user = sessionManager.fetchUser()
        populateFields(user)
    }

    private fun populateFields(user: User?) {
        if (user == null) return

        val nic = user.nic.orEmpty().ifEmpty { "N/A" }
        binding.tvNicBadge.text = nic
        binding.tvUserFullName.text = user.displayName
        binding.tvUserEmail.text = user.email ?: "prosumer@solargridx.local"

        val role = user.role ?: "Prosumer"
        val status = user.accountStatus ?: "Active"
        val isDeactivationPending = status.equals("DeactivationRequested", ignoreCase = true)

        if (isDeactivationPending) {
            binding.tvRoleBadge.text = "$role · Deactivation Pending"
            binding.tvDeactivationDescription.text =
                "A deactivation request is currently pending administrative review by SolarGridX backoffice and operators. Your account remains active while pending. You may cancel this request below."
            binding.btnRequestDeactivation.text = "Cancel Deactivation Request"
            binding.btnRequestDeactivation.setTextColor(Color.parseColor("#4B5563"))
            binding.btnRequestDeactivation.strokeColor = ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
        } else {
            binding.tvRoleBadge.text = "$role · $status"
            binding.tvDeactivationDescription.text =
                "Submit an official deactivation request to SolarGridX backoffice administration. While deactivation is pending, active slot allocations may be concluded."
            binding.btnRequestDeactivation.text = "Request Account Deactivation"
            binding.btnRequestDeactivation.setTextColor(Color.parseColor("#DC2626"))
            binding.btnRequestDeactivation.strokeColor = ColorStateList.valueOf(Color.parseColor("#DC2626"))
        }

        if (!user.firstName.isNullOrBlank()) {
            binding.etFirstName.setText(user.firstName)
        }
        if (!user.lastName.isNullOrBlank()) {
            binding.etLastName.setText(user.lastName)
        }
        if (!user.phone.isNullOrBlank()) {
            binding.etPhone.setText(user.phone)
        }
        if (!user.address.isNullOrBlank()) {
            binding.etAddress.setText(user.address)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            navigateToLogin()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnRequestDeactivation.setOnClickListener {
            val user = sessionManager.fetchUser()
            if (user?.accountStatus.equals("DeactivationRequested", ignoreCase = true)) {
                confirmCancelDeactivation()
            } else {
                confirmDeactivationRequest()
            }
        }
    }

    private fun loadServerProfile() {
        val user = sessionManager.fetchUser()
        val nic = user?.nic
        if (nic.isNullOrBlank()) return

        lifecycleScope.launch {
            val result = prosumerRepository.getProfile(nic)
            result.onSuccess { serverUser ->
                // Keep token
                val updated = serverUser.copy(token = sessionManager.fetchAuthToken())
                sessionManager.updateUser(updated)
                populateFields(updated)
            }.onFailure {
                val localUser = sessionManager.fetchUser()
                populateFields(localUser)
            }
        }
    }

    private fun saveProfileChanges() {
        val user = sessionManager.fetchUser()
        val nic = user?.nic
        if (nic.isNullOrBlank()) {
            Toast.makeText(this, "NIC identifier not found for this profile", Toast.LENGTH_SHORT).show()
            return
        }

        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "First name and Last name are required", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProsumerRequest(
            firstName = firstName,
            lastName = lastName,
            phone = phone.ifEmpty { null },
            address = address.ifEmpty { null }
        )

        binding.btnSaveProfile.isEnabled = false

        lifecycleScope.launch {
            val result = prosumerRepository.updateProfile(nic, request)
            binding.btnSaveProfile.isEnabled = true

            result.onSuccess { updatedUser ->
                val fullUser = updatedUser.copy(token = sessionManager.fetchAuthToken())
                sessionManager.updateUser(fullUser)
                populateFields(fullUser)
                Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }.onFailure { ex ->
                Toast.makeText(this@ProfileActivity, "Failed to update profile: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDeactivationRequest() {
        val user = sessionManager.fetchUser()
        val nic = user?.nic
        if (nic.isNullOrBlank()) {
            Toast.makeText(this, "NIC identifier not found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Deactivation Request")
            .setMessage("Are you sure you want to request deactivation for your prosumer account ($nic)? SolarGridX backoffice and operators will review this request. Your account remains active while pending.")
            .setIcon(R.drawable.ic_clock)
            .setPositiveButton("Submit Request") { _, _ ->
                submitDeactivation(nic)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitDeactivation(nic: String) {
        binding.btnRequestDeactivation.isEnabled = false

        lifecycleScope.launch {
            val result = prosumerRepository.requestDeactivation(nic)
            binding.btnRequestDeactivation.isEnabled = true

            result.onSuccess {
                val currentUser = sessionManager.fetchUser()
                val updated = currentUser?.copy(accountStatus = "DeactivationRequested")
                if (updated != null) {
                    sessionManager.updateUser(updated)
                    populateFields(updated)
                }
                AlertDialog.Builder(this@ProfileActivity)
                    .setTitle("Request Submitted")
                    .setMessage("Your deactivation request for NIC $nic has been recorded and submitted to SolarGridX backoffice and operators. You may continue to conclude active slot allocations while it is reviewed.")
                    .setPositiveButton("OK", null)
                    .show()
            }.onFailure { ex ->
                Toast.makeText(this@ProfileActivity, "Deactivation request failed: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmCancelDeactivation() {
        val user = sessionManager.fetchUser()
        val nic = user?.nic
        if (nic.isNullOrBlank()) {
            Toast.makeText(this, "NIC identifier not found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Cancel Deactivation Request")
            .setMessage("Do you want to withdraw your deactivation request and keep your prosumer account ($nic) active?")
            .setIcon(R.drawable.ic_clock)
            .setPositiveButton("Keep Account Active") { _, _ ->
                submitCancelDeactivation(nic)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun submitCancelDeactivation(nic: String) {
        binding.btnRequestDeactivation.isEnabled = false

        lifecycleScope.launch {
            val result = prosumerRepository.cancelDeactivation(nic)
            binding.btnRequestDeactivation.isEnabled = true

            result.onSuccess {
                val currentUser = sessionManager.fetchUser()
                val updated = currentUser?.copy(accountStatus = "Active")
                if (updated != null) {
                    sessionManager.updateUser(updated)
                    populateFields(updated)
                }
                AlertDialog.Builder(this@ProfileActivity)
                    .setTitle("Request Withdrawn")
                    .setMessage("Your deactivation request for NIC $nic has been cancelled. Your account remains active.")
                    .setPositiveButton("OK", null)
                    .show()
            }.onFailure { ex ->
                Toast.makeText(this@ProfileActivity, "Failed to cancel deactivation: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
