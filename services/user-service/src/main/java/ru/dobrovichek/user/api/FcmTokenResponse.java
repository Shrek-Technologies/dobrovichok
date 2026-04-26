package ru.dobrovichek.user.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ internal API для notification-service")
public record FcmTokenResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String fcmToken
) {
}
