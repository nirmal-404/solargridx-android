package com.solargridx.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.solargridx.app.databinding.ActivityRegisterBinding
import com.solargridx.app.models.RegisterRequest
import com.solargridx.app.repositories.AuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            windowInsets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                submitRegistration()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val nic = binding.etNic.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (nic.isEmpty()) {
            binding.tilNic.error = "NIC is required as primary identity key"
            isValid = false
        } else if (nic.length < 9) {
            binding.tilNic.error = "Please enter a valid NIC (at least 9 characters)"
            isValid = false
        } else {
            binding.tilNic.error = null
        }

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "First name is required"
            isValid = false
        } else {
            binding.tilFirstName.error = null
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Last name is required"
            isValid = false
        } else {
            binding.tilLastName.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty() || password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun submitRegistration() {
        val nic = binding.etNic.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim().ifEmpty { null }
        val address = binding.etAddress.text.toString().trim().ifEmpty { null }

        val request = RegisterRequest(
            nic = nic,
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            address = address
        )

        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.register(request)
            showLoading(false)

            result.onSuccess { user ->
                AlertDialog.Builder(this@RegisterActivity)
                    .setTitle("Registration Successful")
                    .setMessage("Prosumer account registered with NIC: $nic.\nYour account is now pending backoffice activation or immediate login.")
                    .setPositiveButton("Go to Login") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }.onFailure { ex ->
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration Failed: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.etNic.isEnabled = !isLoading
        binding.etFirstName.isEnabled = !isLoading
        binding.etLastName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etAddress.isEnabled = !isLoading
    }
}
