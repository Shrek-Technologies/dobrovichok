package ru.dobrovichek.android.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import ru.dobrovichek.android.BuildConfig

data class RegisterPayload(
    val fullName: String,
    val phone: String,
    val password: String,
    val role: String
)

data class LoginPayload(
    val phone: String,
    val password: String
)

data class AuthResponseDto(
    val userId: String,
    val fullName: String,
    val phone: String,
    val role: String
)

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body payload: RegisterPayload): AuthResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body payload: LoginPayload): AuthResponseDto
}

object AuthApiFactory {
    fun create(): AuthApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.IDENTITY_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(AuthApi::class.java)
    }
}
