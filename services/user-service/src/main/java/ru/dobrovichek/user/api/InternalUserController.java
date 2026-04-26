package ru.dobrovichek.user.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.dobrovichek.security.ServiceHeaders;
import ru.dobrovichek.user.application.ForbiddenException;
import ru.dobrovichek.user.application.UserProfileService;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalUserController {

    private final UserProfileService userProfileService;
    private final String expectedInternalToken;

    public InternalUserController(
            UserProfileService userProfileService,
            @Value("${dobrovichek.internal-api-token:}") String expectedInternalToken
    ) {
        this.userProfileService = userProfileService;
        this.expectedInternalToken = expectedInternalToken;
    }

    @GetMapping("/users/{userId}/fcm-token")
    public ResponseEntity<FcmTokenResponse> getFcmToken(
            @PathVariable UUID userId,
            @RequestHeader(ServiceHeaders.INTERNAL_API_TOKEN) String token
    ) {
        requireValidToken(token);
        return userProfileService.findFcmToken(userId)
                .map(t -> ResponseEntity.ok(new FcmTokenResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    private void requireValidToken(String provided) {
        if (expectedInternalToken == null || expectedInternalToken.isBlank()) {
            throw new ForbiddenException("Internal API is not configured");
        }
        if (provided == null || !Objects.equals(provided.trim(), expectedInternalToken)) {
            throw new ForbiddenException("Invalid internal token");
        }
    }
}
