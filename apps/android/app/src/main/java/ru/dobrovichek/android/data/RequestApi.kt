package ru.dobrovichek.android.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.dobrovichek.android.BuildConfig

data class GeoPointDto(
    val latitude: Double,
    val longitude: Double
)

data class CreateRequestPayload(
    val description: String,
    val contactPhone: String,
    val wardFirstName: String,
    val wardLastName: String,
    val wardPatronymic: String? = null,
    val location: GeoPointDto
)

data class RequestResponseDto(
    val id: String,
    val wardId: String? = null,
    val volunteerId: String? = null,
    val description: String? = null,
    val contactPhone: String? = null,
    val wardFirstName: String? = null,
    val wardLastName: String? = null,
    val wardPatronymic: String? = null,
    val wardFullName: String? = null,
    val location: GeoPointDto? = null,
    val status: String,
    val createdAt: String? = null
)

data class RequestSummaryDto(
    val id: String,
    val wardId: String,
    val wardFirstName: String? = null,
    val description: String,
    val location: GeoPointDto,
    val status: String,
    val createdAt: String,
    val distanceKm: Double
)

interface RequestApi {
    @POST("api/v1/requests")
    suspend fun createRequest(@Body payload: CreateRequestPayload): RequestResponseDto

    @GET("api/v1/requests/nearby")
    suspend fun findNearby(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 1.0
    ): List<RequestSummaryDto>

    @POST("api/v1/requests/{requestId}/accept")
    suspend fun acceptRequest(@Path("requestId") requestId: String): RequestResponseDto

    @GET("api/v1/requests/{requestId}")
    suspend fun getById(@Path("requestId") requestId: String): RequestResponseDto

    @GET("api/v1/requests/active")
    suspend fun getActive(): RequestResponseDto

    @POST("api/v1/requests/{requestId}/cancel")
    suspend fun cancelRequest(@Path("requestId") requestId: String): RequestResponseDto

    @POST("api/v1/requests/{requestId}/complete")
    suspend fun completeRequest(@Path("requestId") requestId: String): RequestResponseDto

    @POST("api/v1/requests/{requestId}/abandon-volunteer")
    suspend fun abandonVolunteer(@Path("requestId") requestId: String): RequestResponseDto
}

object RequestApiFactory {
    fun create(sessionProvider: () -> UserSession?): RequestApi {
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
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authHeadersInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(RequestApi::class.java)
    }
}
