package com.solargridx.app.utils

import android.content.Context
import com.solargridx.app.database.AppDatabaseHelper
import com.solargridx.app.models.User

class SessionManager(context: Context) {

    private val dbHelper = AppDatabaseHelper.getInstance(context)

    companion object {
        @Volatile
        private var cachedUser: User? = null
        @Volatile
        private var cachedToken: String? = null
    }

    fun saveUserSession(user: User, token: String) {
        cachedUser = user
        cachedToken = token
        dbHelper.saveUserSession(user, token)
    }

    fun fetchAuthToken(): String? {
        if (!cachedToken.isNullOrEmpty()) {
            return cachedToken
        }
        val user = dbHelper.getUserSession()
        cachedUser = user
        cachedToken = user?.token
        return cachedToken
    }

    fun fetchUserEmail(): String? {
        val user = fetchUser()
        return user?.fullName ?: user?.email ?: user?.nic
    }

    fun fetchUser(): User? {
        if (cachedUser != null) {
            return cachedUser
        }
        val user = dbHelper.getUserSession()
        cachedUser = user
        if (cachedToken.isNullOrEmpty()) {
            cachedToken = user?.token
        }
        return cachedUser
    }

    fun updateUser(user: User) {
        val currentToken = fetchAuthToken() ?: ""
        cachedUser = user
        dbHelper.saveUserSession(user, currentToken)
    }

    fun clearSession() {
        cachedUser = null
        cachedToken = null
        dbHelper.clearUserSession()
    }

    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }
}

