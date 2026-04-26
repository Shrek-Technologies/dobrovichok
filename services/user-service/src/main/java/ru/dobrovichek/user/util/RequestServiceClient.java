package ru.dobrovichek.user.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.dobrovichek.user.dto.RequestRichSnapshot;
import ru.dobrovichek.user.dto.RequestSnapshot;

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

    public Optional<RequestRichSnapshot> getRequestAsVolunteer(UUID requestId, UUID volunteerId) {
        try {
            RequestRichSnapshot body = restClient.get()
                    .uri("/api/v1/requests/{requestId}", requestId)
                    .headers(this::forwardAuthorization)
                    .retrieve()
                    .body(RequestRichSnapshot.class);
            return Optional.ofNullable(body);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 404 || code == 403) {
                return Optional.empty();
            }
            log.warn("request-service GET /requests/{} (volunteer) failed: {} {}", requestId, code, e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            log.warn("request-service unreachable for request {} (volunteer)", requestId, e);
            throw e;
        }
    }


    public Optional<RequestSnapshot> getRequestAsWard(UUID requestId, UUID wardId) {
        try {
            RequestSnapshot body = restClient.get()
                    .uri("/api/v1/requests/{requestId}", requestId)
                    .headers(this::forwardAuthorization)
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

    private void forwardAuthorization(HttpHeaders headers) {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String auth = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, auth);
            }
        }
    }
}
