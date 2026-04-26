package ru.dobrovichek.android.util

object PersonNameFormat {

    fun fullFormal(firstName: String, patronymic: String?, lastName: String): String {
        return listOfNotNull(
            firstName.trim().takeIf { it.isNotEmpty() },
            patronymic?.trim()?.takeIf { it.isNotEmpty() },
            lastName.trim().takeIf { it.isNotEmpty() }
        ).joinToString(" ")
    }

    fun firstNameOnly(firstName: String): String = firstName.trim()

    /** Имя и фамилия волонтёра для экрана подопечного. */
    fun volunteerForWard(firstName: String?, lastName: String?): String {
        return listOfNotNull(
            firstName?.trim()?.takeIf { it.isNotEmpty() },
            lastName?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(" ")
    }
}
