package ru.dobrovichek.user.infrastructure.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.security.ServiceHeaders;

import java.util.Optional;
import java.util.UUID;

@Component
public class RequestServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RequestServiceClient.class);

    private final RestClient restClient;

    public RequestServiceClient(
            @Value("${dobrovichek.request-service.base-url:http://localhost:8083}") String baseUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Читает заявку от имени подопечного (тот же доступ, что у клиента при оценке).
     */
    public Optional<RequestSnapshot> getRequestAsWard(UUID requestId, UUID wardId) {
        try {
            RequestSnapshot body = restClient.get()
                    .uri("/api/v1/requests/{requestId}", requestId)
                    .header(ServiceHeaders.USER_ID, wardId.toString())
                    .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name())
                    .retrieve()
                    .body(RequestSnapshot.class);
            return Optional.ofNullable(body);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 404 || code == 403) {
                return Optional.empty();
            }
            log.warn("request-service GET /requests/{} failed: {} {}", requestId, code, e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            log.warn("request-service unreachable for request {}", requestId, e);
            throw e;
        }
    }
}
