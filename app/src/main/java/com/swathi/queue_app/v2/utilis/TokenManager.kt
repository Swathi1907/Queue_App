package com.swathi.queue_app.v2.utilis

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    fun getEmail(): String? =
        sharedPreferences.getString("user_email", null)

    fun getPassword(): String? =
        sharedPreferences.getString("user_password", null)


    // =====================================================
    // AUTH
    // =====================================================

    fun saveAuthData(token: String, role: String) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .putString("user_role", role)
            .apply()
    }


    // =====================================================
    // DOCTOR
    // =====================================================

    fun saveDoctorCode(doctorCode: String) {
        sharedPreferences.edit()
            .putString("doctorId", doctorCode)
            .apply()
    }

    fun getDoctorCode(): String? =
        sharedPreferences.getString("doctorId", "")


    // =====================================================
    // USER PROFILE
    // =====================================================

    fun saveUserProfile(userId: String, name: String) {
        sharedPreferences.edit()
            .putString("user_id", userId)
            .putString("user_name", name)
            .apply()
    }

    fun savecontact(contact: String) {
        sharedPreferences.edit()
            .putString("contact", contact)
            .apply()
    }


    // =====================================================
    // USER DEPARTMENTS
    // =====================================================

    fun saveUserDepartments(departments: String) {
        sharedPreferences.edit()
            .putString("USER_DEPARTMENTS", departments)
            .apply()
    }

    fun getUserDepartments(): String? {
        return sharedPreferences.getString(
            "USER_DEPARTMENTS",
            null
        )
    }


    // =====================================================
    // HOSPITAL DEPARTMENTS
    // =====================================================

    fun saveHospitalDepartments(
        departments: List<String>
    ) {

        val json = Gson().toJson(departments)

        sharedPreferences.edit()
            .putString(
                "HOSPITAL_DEPARTMENTS",
                json
            )
            .apply()
    }

    fun getHospitalDepartments(): List<String> {

        val json = sharedPreferences.getString(
            "HOSPITAL_DEPARTMENTS",
            null
        ) ?: return emptyList()

        val type =
            object : TypeToken<List<String>>() {}.type

        return Gson().fromJson(
            json,
            type
        )
    }


    // =====================================================
    // HOSPITAL
    // =====================================================

    fun saveHospitalId(hospitalId: String) {
        sharedPreferences.edit()
            .putString(
                "hospital_id",
                hospitalId
            )
            .apply()
    }

    fun getHospitalId(): String? =
        sharedPreferences.getString(
            "hospital_id",
            null
        )


    // =====================================================
    // OTHER
    // =====================================================

    fun getUserrole(): String? {
        return sharedPreferences.getString(
            "user_role",
            "User"
        )
    }

    fun getContact(): String? =
        sharedPreferences.getString(
            "contact",
            "6281556414"
        )

    fun getUserId(): String? =
        sharedPreferences.getString(
            "user_id",
            " "
        )

    fun getUserName(): String? =
        sharedPreferences.getString(
            "user_name",
            null
        )

    fun getToken(): String? =
        sharedPreferences.getString(
            "jwt_token",
            null
        )

    fun getRole(): String? =
        sharedPreferences.getString(
            "user_role",
            null
        )


    // =====================================================
    // CLEAR SESSION
    // =====================================================

    fun clearSession() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }

    fun clear() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}