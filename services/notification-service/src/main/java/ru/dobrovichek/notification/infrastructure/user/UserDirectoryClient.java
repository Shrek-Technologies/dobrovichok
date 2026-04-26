package ru.dobrovichek.notification.infrastructure.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.dobrovichek.security.ServiceHeaders;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserDirectoryClient {

    private static final Logger log = LoggerFactory.getLogger(UserDirectoryClient.class);

    private final RestClient restClient;
    private final String internalToken;

    public UserDirectoryClient(
            @Value("${dobrovichek.user-service.base-url:http://localhost:8082}") String baseUrl,
            @Value("${dobrovichek.internal-api-token:}") String internalToken
    ) {
        this.internalToken = internalToken;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<String> findFcmToken(UUID userId) {
        if (internalToken == null || internalToken.isBlank()) {
            log.debug("dobrovichek.internal-api-token is empty; skip FCM lookup");
            return Optional.empty();
        }
        try {
            FcmTokenPayload body = restClient.get()
                    .uri("/api/v1/internal/users/{userId}/fcm-token", userId)
                    .header(ServiceHeaders.INTERNAL_API_TOKEN, internalToken)
                    .retrieve()
                    .body(FcmTokenPayload.class);
            if (body == null || body.fcmToken() == null || body.fcmToken().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(body.fcmToken());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.warn("user-service FCM lookup failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("user-service FCM lookup failed", e);
            return Optional.empty();
        }
    }
}
