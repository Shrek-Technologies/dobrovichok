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
import retrofit2.http.POST
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

data class VolunteerRequestHistoryItemDto(
    val requestId: String,
    val wardId: String,
    val status: String,
    val acceptedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val updatedAt: String? = null,
    val category: String? = null,
    val address: String? = null,
    val wardRating: Int? = null
)

data class CreateVolunteerRatingPayload(
    val requestId: String,
    val score: Int
)

data class RegisterDevicePayload(
    val fcmToken: String? = null
)

interface UserApi {
    @PUT("api/v1/users/me")
    suspend fun updateMyProfile(@Body payload: UpdateMyProfilePayload): UserProfileMeDto

    @PUT("api/v1/users/me/device")
    suspend fun registerDevice(@Body payload: RegisterDevicePayload)

    @GET("api/v1/volunteers/{volunteerId}")
    suspend fun getVolunteerProfile(@Path("volunteerId") volunteerId: String): VolunteerProfileDto

    @GET("api/v1/volunteers/{volunteerId}/requests/history")
    suspend fun getVolunteerRequestHistory(@Path("volunteerId") volunteerId: String): List<VolunteerRequestHistoryItemDto>

    @POST("api/v1/volunteers/{volunteerId}/ratings")
    suspend fun createVolunteerRating(
        @Path("volunteerId") volunteerId: String,
        @Body payload: CreateVolunteerRatingPayload
    ): VolunteerRatingResponseDto
}

data class VolunteerRatingResponseDto(
    val id: String? = null,
    val requestId: String? = null,
    val volunteerId: String? = null,
    val wardId: String? = null,
    val score: Int? = null,
    val createdAt: String? = null
)

object UserApiFactory {
    fun create(sessionProvider: () -> UserSession?): UserApi {
        val authHeadersInterceptor = Interceptor { chain ->
            val session = sessionProvider()
            val request = chain.request().newBuilder()
                .apply {
                    val token = session?.accessToken?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
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
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(UserApi::class.java)
    }
}
