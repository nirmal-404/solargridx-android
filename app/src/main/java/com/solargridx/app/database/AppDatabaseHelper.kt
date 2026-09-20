package com.solargridx.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.solargridx.app.models.User

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NIC TEXT,
                $COLUMN_EMAIL TEXT,
                $COLUMN_FULL_NAME TEXT,
                $COLUMN_ROLE TEXT,
                $COLUMN_TOKEN TEXT
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun saveUserSession(user: User, token: String): Boolean {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_USERS")

        val values = ContentValues().apply {
            put(COLUMN_ID, user.id ?: "usr_local")
            put(COLUMN_NIC, user.nic ?: "")
            put(COLUMN_EMAIL, user.email ?: "")
            put(COLUMN_FULL_NAME, user.fullName ?: "")
            put(COLUMN_ROLE, user.role ?: "")
            put(COLUMN_TOKEN, token)
        }

        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun getUserSession(): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS LIMIT 1", null)
        var user: User? = null

        if (cursor.moveToFirst()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val nic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NIC))
            val email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            val fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME))
            val role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
            val token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN))

            user = User(
                id = id,
                nic = nic,
                email = email,
                fullName = fullName,
                role = role,
                token = token
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun clearUserSession() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_USERS")
        db.close()
    }

    companion object {
        private const val DATABASE_NAME = "solargridx_local.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users_session"
        const val COLUMN_ID = "id"
        const val COLUMN_NIC = "nic"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_ROLE = "role"
        const val COLUMN_TOKEN = "token"
    }
}
