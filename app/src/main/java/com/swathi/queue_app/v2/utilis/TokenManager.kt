package com.swathi.queue_app.v2.utilis

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()
    }

    fun getEmail(): String? = sharedPreferences.getString("user_email", null)

    fun getPassword(): String? = sharedPreferences.getString("user_password", null)
    fun saveAuthData(token: String, role: String) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .putString("user_role", role)
            .apply()
    }
    fun saveHospitalId(hospitalId: String) {
        sharedPreferences.edit()
            .putString("hospital_id", hospitalId)
            .apply()
    }
    fun getToken(): String? = sharedPreferences.getString("jwt_token", null)

    fun getRole(): String? = sharedPreferences.getString("user_role", null)
    fun getHospitalId(): String?=sharedPreferences.getString("hospital_id", null)
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}