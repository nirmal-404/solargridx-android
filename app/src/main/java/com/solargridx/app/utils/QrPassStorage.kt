package com.solargridx.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solargridx.app.models.QrDispatchPass

/**
 * QrPassStorage.kt
 * Persists generated QR dispatch passes locally so tokens are only generated once
 * and displayed consistently without unnecessary server re-generation.
 */
object QrPassStorage {

    private const val PREF_NAME = "solargridx_qr_passes_pref"
    private const val KEY_PASSES = "saved_qr_passes"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getPass(context: Context, reservationId: String): QrDispatchPass? {
        val all = getAllPasses(context)
        return all.firstOrNull { it.reservationId.equals(reservationId, ignoreCase = true) }
    }

    @Synchronized
    fun savePass(context: Context, pass: QrDispatchPass) {
        val all = getAllPasses(context).toMutableList()
        all.removeAll { it.reservationId.equals(pass.reservationId, ignoreCase = true) }
        all.add(0, pass)
        val json = gson.toJson(all)
        getPrefs(context).edit().putString(KEY_PASSES, json).apply()
    }

    @Synchronized
    fun getAllPasses(context: Context): List<QrDispatchPass> {
        val json = getPrefs(context).getString(KEY_PASSES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QrDispatchPass>>() {}.type
            gson.fromJson<List<QrDispatchPass>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun clear(context: Context) {
        getPrefs(context).edit().remove(KEY_PASSES).apply()
    }
}
