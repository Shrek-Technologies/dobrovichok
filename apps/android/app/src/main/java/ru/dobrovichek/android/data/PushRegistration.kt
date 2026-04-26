package ru.dobrovichek.android.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object PushRegistration {
    suspend fun syncToBackend(userRepository: UserRepository) {
        val token = FirebaseMessaging.getInstance().token.await()
        if (token.isNotBlank()) {
            userRepository.registerDevice(token)
        }
    }
}
