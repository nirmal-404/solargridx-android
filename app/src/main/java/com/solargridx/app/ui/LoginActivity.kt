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
import com.solargridx.app.R
import com.solargridx.app.databinding.ActivityLoginBinding
import com.solargridx.app.models.User
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                viewModel.login(email, password)
            }
        }

        binding.btnQuickBackoffice.setOnClickListener {
            fillCredentials("admin@solargridx.local", "Admin@1234")
        }

        binding.btnQuickOperator.setOnClickListener {
            fillCredentials("operator@solargridx.local", "Operator@1234")
        }

        binding.btnQuickProsumer.setOnClickListener {
            fillCredentials("kamal.silva@prosumer.local", "Prosumer@1234")
        }
    }

    private fun fillCredentials(email: String, pass: String) {
        binding.etEmail.setText(email)
        binding.etPassword.setText(pass)
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_empty_password)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            showLoading(false)
                        }
                        is LoginUiState.Loading -> {
                            showLoading(true)
                        }
                        is LoginUiState.Success -> {
                            showLoading(false)
                            val inputEmail = binding.etEmail.text.toString().trim()
                            val userToSave = state.user ?: User(email = inputEmail, fullName = inputEmail)
                            sessionManager.saveUserSession(userToSave, state.token)

                            val userDisplayName = userToSave.fullName
                                ?: userToSave.email
                                ?: userToSave.nic
                                ?: inputEmail

                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.welcome_user, userDisplayName),
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMain()
                        }
                        is LoginUiState.Error -> {
                            showLoading(false)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnQuickBackoffice.isEnabled = !isLoading
        binding.btnQuickOperator.isEnabled = !isLoading
        binding.btnQuickProsumer.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
