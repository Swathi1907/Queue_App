package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.NotificationModel
import com.swathi.queue_app.v2.repo.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = NotificationRepository()

    private val _notifications =
        MutableLiveData<List<NotificationModel>>()

    val notifications: LiveData<List<NotificationModel>>
        get() = _notifications

    private val _error =
        MutableLiveData<String>()

    val error: LiveData<String>
        get() = _error

    private val _loading =
        MutableLiveData<Boolean>()

    val loading: LiveData<Boolean>
        get() = _loading

    fun getNotifications(targetRole: String) {

        viewModelScope.launch {

            _loading.value = true

            val result = repository.getNotifications(targetRole)


            result.onSuccess { list ->

                Log.d(
                    "NOTIFICATION",
                    "Success: ${list.size} notifications"
                )

                _notifications.value = list

            }.onFailure { exception ->

                Log.e(
                    "NOTIFICATION",
                    "ERROR: ${exception.stackTraceToString()}"
                )

                _error.value =
                    exception.localizedMessage
                        ?: "Failed to load notifications"
            }

            _loading.value = false
        }
    }
}