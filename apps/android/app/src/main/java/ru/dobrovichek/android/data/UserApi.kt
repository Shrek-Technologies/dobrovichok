package ru.dobrovichek.android.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.dobrovichek.android.BuildConfig

data class UpdateMyProfilePayload(
    val firstName: String,
    val lastName: String,
    val patronymic: String? = null,
    val phone: String,
    val bio: String? = null,
    val city: String? = null
)

/** Ответ {@code PUT /api/v1/users/me} (поля опциональны — парсим только нужное). */
data class UserProfileMeDto(
    val id: String? = null,
    val role: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val fullName: String? = null,
    val phone: String? = null
)

data class VolunteerProfileDto(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val completedRequestsCount: Int? = null
)

interface UserApi {
    @PUT("api/v1/users/me")
    suspend fun updateMyProfile(@Body payload: UpdateMyProfilePayload): UserProfileMeDto

    @GET("api/v1/volunteers/{volunteerId}")
    suspend fun getVolunteerProfile(@Path("volunteerId") volunteerId: String): VolunteerProfileDto
}

object UserApiFactory {
    fun create(sessionProvider: () -> UserSession?): UserApi {
        val authHeadersInterceptor = Interceptor { chain ->
            val session = sessionProvider()
            val request = chain.request().newBuilder()
                .apply {
                    if (session != null) {
                        addHeader("X-User-Id", session.userId)
                        addHeader("X-User-Role", session.role)
                    }
                }
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .addInterceptor(authHeadersInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.USER_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(UserApi::class.java)
    }
}
