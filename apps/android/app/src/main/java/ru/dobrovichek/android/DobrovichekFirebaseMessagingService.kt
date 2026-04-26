package ru.dobrovichek.android

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.dobrovichek.android.data.SessionStore
import ru.dobrovichek.android.data.UserApiFactory
import ru.dobrovichek.android.data.UserRepository

class DobrovichekFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            val ctx = applicationContext
            if (SessionStore(ctx).load() == null) return@launch
            runCatching {
                UserRepository(UserApiFactory.create(sessionProvider = { SessionStore(ctx).load() }))
                    .registerDevice(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (inForeground) {
            return
        }
        val title = message.notification?.title
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: defaultBody(message.data["type"])
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, DobrovichekApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pin)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify((message.messageId ?: message.sentTime.toString()).hashCode(), notification)
    }

    private fun defaultBody(type: String?): String {
        return when (type) {
            "REQUEST_ACCEPTED" -> "Заявка принята волонтёром"
            "REQUEST_CANCELLED" -> "Заявка отменена"
            "REQUEST_COMPLETED" -> "Заявка завершена"
            "VOLUNTEER_ABANDONED" -> "Волонтёр отказался от заявки"
            else -> "Откройте приложение Добровичок"
        }
    }
}
