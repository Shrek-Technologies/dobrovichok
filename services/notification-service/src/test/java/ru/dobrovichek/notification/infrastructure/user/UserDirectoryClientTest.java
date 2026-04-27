package ru.dobrovichek.notification.infrastructure.user;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.dobrovichek.security.ServiceHeaders;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserDirectoryClientTest {

    private final MockWebServer server = new MockWebServer();

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void returnsEmptyWhenInternalTokenBlank() {
        UserDirectoryClient client = new UserDirectoryClient("http://localhost:8082", "  ");
        assertThat(client.findFcmToken(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsTokenOn200() throws Exception {
        server.start();
        server.enqueue(new MockResponse()
                .setBody("{\"fcmToken\":\"abc-token\"}")
                .addHeader("Content-Type", "application/json"));

        String base = "http://" + server.getHostName() + ":" + server.getPort();
        UserDirectoryClient client = new UserDirectoryClient(base, "secret");
        UUID userId = UUID.randomUUID();

        Optional<String> token = client.findFcmToken(userId);
        assertThat(token).contains("abc-token");

        var recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/api/v1/internal/users/" + userId + "/fcm-token");
        assertThat(recorded.getHeader(ServiceHeaders.INTERNAL_API_TOKEN)).isEqualTo("secret");
    }

    @Test
    void returnsEmptyOn404() throws Exception {
        server.start();
        server.enqueue(new MockResponse().setResponseCode(404));

        String base = "http://" + server.getHostName() + ":" + server.getPort();
        UserDirectoryClient client = new UserDirectoryClient(base, "secret");
        assertThat(client.findFcmToken(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsEmptyWhenBodyHasNoToken() throws Exception {
        server.start();
        server.enqueue(new MockResponse()
                .setBody("{\"fcmToken\":null}")
                .addHeader("Content-Type", "application/json"));

        String base = "http://" + server.getHostName() + ":" + server.getPort();
        UserDirectoryClient client = new UserDirectoryClient(base, "secret");
        assertThat(client.findFcmToken(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsEmptyOnServerError() throws Exception {
        server.start();
        server.enqueue(new MockResponse().setResponseCode(500).setBody("err"));

        String base = "http://" + server.getHostName() + ":" + server.getPort();
        UserDirectoryClient client = new UserDirectoryClient(base, "secret");
        assertThat(client.findFcmToken(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsEmptyOnConnectionFailure() {
        UserDirectoryClient client = new UserDirectoryClient("http://127.0.0.1:1", "secret");
        assertThat(client.findFcmToken(UUID.randomUUID())).isEmpty();
    }
}
