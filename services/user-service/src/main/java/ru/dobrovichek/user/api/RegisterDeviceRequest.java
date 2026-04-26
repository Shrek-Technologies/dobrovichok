package ru.dobrovichek.user.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Регистрация FCM-токена; пустое значение или null снимает токен")
public record RegisterDeviceRequest(
        @Schema(description = "FCM registration token")
        String fcmToken
) {
}
