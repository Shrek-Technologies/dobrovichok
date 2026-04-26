package ru.dobrovichek.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.MapKit
import ru.dobrovichek.android.data.AuthApiFactory
import ru.dobrovichek.android.data.AuthRepository
import ru.dobrovichek.android.data.RequestApiFactory
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.data.SessionStore
import ru.dobrovichek.android.data.UserApiFactory
import ru.dobrovichek.android.data.UserRepository
import ru.dobrovichek.android.ui.WardApp

class MainActivity : ComponentActivity() {
    private val mapKit: MapKit by lazy { MapKitFactory.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(applicationContext)
        val authRepository = AuthRepository(AuthApiFactory.create(), sessionStore)
        val requestRepository = RequestRepository(
            RequestApiFactory.create(sessionProvider = sessionStore::load)
        )
        val userRepository = UserRepository(
            UserApiFactory.create(sessionProvider = sessionStore::load)
        )

        setContent {
            MaterialTheme {
                Surface {
                    WardApp(
                        sessionStore = sessionStore,
                        authRepository = authRepository,
                        requestRepository = requestRepository,
                        userRepository = userRepository
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapKit.onStart()
    }

    override fun onStop() {
        mapKit.onStop()
        super.onStop()
    }
}
