package ru.dobrovichek.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import ru.dobrovichek.android.data.AuthApiFactory
import ru.dobrovichek.android.data.AuthRepository
import ru.dobrovichek.android.data.RequestApiFactory
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.data.SessionStore
import ru.dobrovichek.android.ui.WardApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(applicationContext)
        val authRepository = AuthRepository(AuthApiFactory.create(), sessionStore)
        val requestRepository = RequestRepository(
            RequestApiFactory.create(sessionProvider = sessionStore::load)
        )

        setContent {
            MaterialTheme {
                Surface {
                    WardApp(
                        authRepository = authRepository,
                        requestRepository = requestRepository
                    )
                }
            }
        }
    }
}
