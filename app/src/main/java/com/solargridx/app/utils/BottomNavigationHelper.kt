package com.solargridx.app.utils

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.solargridx.app.R
import com.solargridx.app.ui.CreateReservationActivity
import com.solargridx.app.ui.MainActivity
import com.solargridx.app.ui.MapsActivity
import com.solargridx.app.ui.QrDispatcherActivity

/**
 * BottomNavigationHelper
 * Centralized setup for the fixed bottom navigation bar across all main screens.
 */
object BottomNavigationHelper {

    fun setup(activity: Activity, bottomNav: BottomNavigationView, currentItemId: Int) {
        bottomNav.selectedItemId = currentItemId
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) {
                if (activity is MainActivity && currentItemId == R.id.nav_dashboard) {
                    activity.onDashboardReselected()
                }
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    val intent = Intent(activity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                    true
                }
                R.id.nav_book_slot -> {
                    val intent = Intent(activity, CreateReservationActivity::class.java)
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                    true
                }
                R.id.nav_map -> {
                    val intent = Intent(activity, MapsActivity::class.java)
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                    true
                }
                R.id.nav_qr -> {
                    val intent = Intent(activity, QrDispatcherActivity::class.java)
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                    true
                }
                R.id.nav_history -> {
                    val intent = Intent(activity, com.solargridx.app.ui.ReservationsActivity::class.java).apply {
                        putExtra(com.solargridx.app.ui.ReservationsActivity.EXTRA_FILTER, "all")
                    }
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showBookingHistoryDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Booking History & Status")
            .setMessage(
                "Solar microgrid slot reservations sync in real time with backend dispatch."
            )
            .setPositiveButton("Book New Slot") { _, _ ->
                if (activity !is CreateReservationActivity) {
                    val intent = Intent(activity, CreateReservationActivity::class.java)
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        activity.finish()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
