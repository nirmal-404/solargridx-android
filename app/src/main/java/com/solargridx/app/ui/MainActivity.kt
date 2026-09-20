package com.solargridx.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.solargridx.app.R
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
        if (userEmail != null) {
            binding.tvUserEmail.text = getString(R.string.welcome_user, userEmail)
        } else {
            binding.tvUserEmail.text = ""
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
