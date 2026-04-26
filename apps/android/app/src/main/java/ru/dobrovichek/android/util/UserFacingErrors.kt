package ru.dobrovichek.android.util

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object UserFacingErrors {

    const val SERVICE_UNAVAILABLE: String = "Сервис недоступен. Попробуйте позже."

    const val NO_CONNECTION: String = "Нет соединения."

    const val REQUEST_TIMEOUT: String = "Не удалось дождаться ответа. Попробуйте позже."

    fun networkOrHttp(throwable: Throwable, default: String): String {
        if (throwable is HttpException) {
            when (val code = throwable.code()) {
                in 500..599 -> return SERVICE_UNAVAILABLE
                408 -> return REQUEST_TIMEOUT
                429 -> return "Слишком много запросов. Попробуйте чуть позже."
            }
        }
        if (throwable is IOException) {
            return when (throwable) {
                is UnknownHostException ->
                    "Сервер не найден. На реальном устройстве в apps/android/local.properties укажите API_BASE_URL=http://IP_вашего_ПК:8080/ (не localhost и не 10.0.2.2)."
                is ConnectException ->
                    "Не удалось подключиться к серверу. Проверьте, что бэкенд запущен, телефон в той же Wi‑Fi сети и в firewall разрешён порт 8080."
                is SocketTimeoutException -> REQUEST_TIMEOUT
                else -> NO_CONNECTION
            }
        }
        return default
    }
}
