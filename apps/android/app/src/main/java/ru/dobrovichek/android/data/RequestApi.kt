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
import retrofit2.http.Path
import ru.dobrovichek.android.BuildConfig

data class GeoPointDto(
    val latitude: Double,
    val longitude: Double
)

data class CreateRequestPayload(
    val description: String,
    val contactPhone: String,
    val location: GeoPointDto
)

data class RequestResponseDto(
    val id: String,
    val status: String
)

interface RequestApi {
    @POST("api/v1/requests")
    suspend fun createRequest(@Body payload: CreateRequestPayload): RequestResponseDto

    @POST("api/v1/requests/{requestId}/cancel")
    suspend fun cancelRequest(@Path("requestId") requestId: String): RequestResponseDto
}

object RequestApiFactory {
    fun create(sessionProvider: () -> UserSession?): RequestApi {
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
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(RequestApi::class.java)
    }
}
