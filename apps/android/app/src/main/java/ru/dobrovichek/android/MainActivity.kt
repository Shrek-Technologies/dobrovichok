package ru.dobrovichek.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import ru.dobrovichek.android.data.RequestApiFactory
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.ui.WardApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = RequestRepository(RequestApiFactory.create())

        setContent {
            MaterialTheme {
                Surface {
                    WardApp(repository = repository)
                }
            }
        }
    }
}
