package com.solargridx.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.solargridx.app.databinding.ActivityMainBinding
import com.solargridx.app.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val userEmail = sessionManager.fetchUserEmail()
        if (!userEmail.isNullOrEmpty()) {
            binding.tvUserEmail.text = "Logged in as: $userEmail"
        } else {
            binding.tvUserEmail.text = "Logged in successfully"
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
