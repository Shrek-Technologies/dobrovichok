package ru.dobrovichek.notification.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "dobrovichek.notifications.firebase-enabled", havingValue = "true")
public class FirebaseConfiguration {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials = resolveCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    private static GoogleCredentials resolveCredentials() throws IOException {
        String raw = firstNonBlank(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"), System.getProperty("GOOGLE_APPLICATION_CREDENTIALS"));
        if (raw != null && !raw.isBlank()) {
            String pathStr = stripQuotes(raw.trim());
            if (pathStr.isEmpty()) {
                return GoogleCredentials.getApplicationDefault();
            }
            Path p = Path.of(pathStr);
            if (!Files.isRegularFile(p)) {
                throw new IOException(
                        "GOOGLE_APPLICATION_CREDENTIALS: файл не найден: " + p.toAbsolutePath()
                                + ". Проверьте путь и уберите кавычки вокруг значения в Environment variables (IDEA)."
                );
            }
            try (InputStream in = Files.newInputStream(p)) {
                return GoogleCredentials.fromStream(in);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2) {
            if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
