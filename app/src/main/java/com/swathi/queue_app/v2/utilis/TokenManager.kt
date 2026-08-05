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
    fun saveDoctorCode(doctorCode: String){
        sharedPreferences.edit()
        .putString("doctorId", doctorCode)
            .apply()
    }
    fun getDoctorCode(): String?=sharedPreferences.getString("doctorId","")
    // Added method to save user profile info (ID and Name)
    fun saveUserProfile(userId: String, name: String) {
        sharedPreferences.edit()
            .putString("user_id", userId)
            .putString("user_name", name)
            .apply()
    }
    fun savecontact(contact: String){
        sharedPreferences.edit()
            .putString("contact",contact)
            .apply()
    }
    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
    fun saveUserDepartments(departments: String) {
        sharedPreferences.edit().putString("USER_DEPARTMENTS", departments).apply()
    }
    fun getUserrole(): String?{
        return sharedPreferences.getString("user_role", "User")
    }
    fun getUserDepartments(): String? {
        return sharedPreferences.getString("USER_DEPARTMENTS", null)
    }
    fun getContact(): String?=sharedPreferences.getString("contact","6281556414")
    fun getUserId(): String? = sharedPreferences.getString("user_id", " ")

    fun getUserName(): String? = sharedPreferences.getString("user_name", null)

    fun saveHospitalId(hospitalId: String) {
        sharedPreferences.edit()
            .putString("hospital_id", hospitalId)
            .apply()
    }

    fun getToken(): String? = sharedPreferences.getString("jwt_token", null)

    fun getRole(): String? = sharedPreferences.getString("user_role", null)

    fun getHospitalId(): String? = sharedPreferences.getString("hospital_id", null)

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}