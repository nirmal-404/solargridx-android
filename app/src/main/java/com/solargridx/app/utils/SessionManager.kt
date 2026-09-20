package com.solargridx.app.utils

import android.content.Context
import com.solargridx.app.database.AppDatabaseHelper
import com.solargridx.app.models.User

class SessionManager(context: Context) {

    private val dbHelper = AppDatabaseHelper(context)

    fun saveUserSession(user: User, token: String) {
        dbHelper.saveUserSession(user, token)
    }

    fun fetchAuthToken(): String? {
        return dbHelper.getUserSession()?.token
    }

    fun fetchUserEmail(): String? {
        val user = dbHelper.getUserSession()
        return user?.fullName ?: user?.email ?: user?.nic
    }

    fun fetchUser(): User? {
        return dbHelper.getUserSession()
    }

    fun clearSession() {
        dbHelper.clearUserSession()
    }

    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }
}
