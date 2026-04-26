package ru.dobrovichek.android.data

/**
 * Сохраняет тело ошибки: у [retrofit2.HttpException] после чтения [okhttp3.ResponseBody] повторное чтение пустое.
 */
class VolunteerRatingHttpException(
    val statusCode: Int,
    val errorBody: String,
    cause: Throwable? = null
) : Exception(cause)
