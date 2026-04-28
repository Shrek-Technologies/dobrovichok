package ru.dobrovichek.android.data

class VolunteerRatingHttpException(
    val statusCode: Int,
    val errorBody: String,
    cause: Throwable? = null
) : Exception(cause)
