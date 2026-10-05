package com.solargridx.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.solargridx.app.models.User

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
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
                $COLUMN_FIRST_NAME TEXT,
                $COLUMN_LAST_NAME TEXT,
                $COLUMN_FULL_NAME TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_ROLE TEXT,
                $COLUMN_ACCOUNT_STATUS TEXT,
                $COLUMN_TOKEN TEXT
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    @Synchronized
    fun saveUserSession(user: User, token: String): Boolean {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_USERS")

        val values = ContentValues().apply {
            put(COLUMN_ID, user.id ?: "usr_local")
            put(COLUMN_NIC, user.nic ?: "")
            put(COLUMN_EMAIL, user.email ?: "")
            put(COLUMN_FIRST_NAME, user.firstName ?: "")
            put(COLUMN_LAST_NAME, user.lastName ?: "")
            put(COLUMN_FULL_NAME, user.fullName ?: "")
            put(COLUMN_PHONE, user.phone ?: "")
            put(COLUMN_ADDRESS, user.address ?: "")
            put(COLUMN_ROLE, user.role ?: "")
            put(COLUMN_ACCOUNT_STATUS, user.accountStatus ?: "")
            put(COLUMN_TOKEN, token)
        }

        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    @Synchronized
    fun getUserSession(): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS LIMIT 1", null)
        var user: User? = null

        cursor.use { c ->
            if (c.moveToFirst()) {
                val id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID))
                val nic = c.getString(c.getColumnIndexOrThrow(COLUMN_NIC))
                val email = c.getString(c.getColumnIndexOrThrow(COLUMN_EMAIL))
                val firstName = c.getString(c.getColumnIndexOrThrow(COLUMN_FIRST_NAME))
                val lastName = c.getString(c.getColumnIndexOrThrow(COLUMN_LAST_NAME))
                val fullName = c.getString(c.getColumnIndexOrThrow(COLUMN_FULL_NAME))
                val phone = c.getString(c.getColumnIndexOrThrow(COLUMN_PHONE))
                val address = c.getString(c.getColumnIndexOrThrow(COLUMN_ADDRESS))
                val role = c.getString(c.getColumnIndexOrThrow(COLUMN_ROLE))
                val accountStatus = c.getString(c.getColumnIndexOrThrow(COLUMN_ACCOUNT_STATUS))
                val token = c.getString(c.getColumnIndexOrThrow(COLUMN_TOKEN))

                user = User(
                    id = id,
                    nic = nic,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    role = role,
                    accountStatus = accountStatus,
                    token = token
                )
            }
        }
        return user
    }

    @Synchronized
    fun clearUserSession() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_USERS")
    }

    companion object {
        private const val DATABASE_NAME = "solargridx_local.db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }

        const val TABLE_USERS = "users_session"
        const val COLUMN_ID = "id"
        const val COLUMN_NIC = "nic"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_LAST_NAME = "last_name"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_ROLE = "role"
        const val COLUMN_ACCOUNT_STATUS = "account_status"
        const val COLUMN_TOKEN = "token"
    }
}
