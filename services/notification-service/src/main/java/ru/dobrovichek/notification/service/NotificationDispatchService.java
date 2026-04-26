package ru.dobrovichek.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.notification.infrastructure.user.UserDirectoryClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final UserDirectoryClient userDirectoryClient;
    private final Optional<FirebaseMessaging> firebaseMessaging;

    public NotificationDispatchService(
            UserDirectoryClient userDirectoryClient,
            @Autowired(required = false) FirebaseMessaging firebaseMessaging
    ) {
        this.userDirectoryClient = userDirectoryClient;
        this.firebaseMessaging = Optional.ofNullable(firebaseMessaging);
    }

    public void handle(RequestCreatedEvent event) {
        log.info(
                "New request {} (nearby volunteer push not implemented)",
                event.requestId()
        );
    }

    public void handle(VolunteerAbandonedRequestEvent event) {
        sendToUser(
                event.wardId(),
                "Волонтёр отказался",
                "Заявка снова в поиске. Откройте приложение.",
                data("VOLUNTEER_ABANDONED", event.requestId())
        );
    }

    public void handle(RequestStatusChangedEvent event) {
        RequestStatus status = event.status();
        if (status == RequestStatus.ACCEPTED && event.volunteerId() != null) {
            sendToUser(
                    event.wardId(),
                    "Волонтёр нашёлся",
                    "Заявка принята. Откройте приложение.",
                    data("REQUEST_ACCEPTED", event.requestId())
            );
            return;
        }
        if (status == RequestStatus.CANCELLED) {
            if (event.volunteerId() != null) {
                sendToUser(
                        event.volunteerId(),
                        "Заявка отменена",
                        "Подопечный отменил заявку.",
                        data("REQUEST_CANCELLED", event.requestId())
                );
            } else {
                log.debug(
                        "Request {} cancelled with no volunteer on event; skip volunteer push",
                        event.requestId()
                );
            }
            return;
        }
        if (status == RequestStatus.COMPLETED && event.volunteerId() != null) {
            sendToUser(
                    event.wardId(),
                    "Помощь завершена",
                    "Заявка закрыта. При необходимости оцените волонтёра.",
                    data("REQUEST_COMPLETED", event.requestId())
            );
        }
    }

    private static Map<String, String> data(String type, UUID requestId) {
        Map<String, String> m = new HashMap<>();
        m.put("type", type);
        m.put("requestId", requestId.toString());
        return m;
    }

    private void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        Optional<String> token = userDirectoryClient.findFcmToken(userId);
        if (token.isEmpty()) {
            log.debug("No FCM token for user {}, skip push", userId);
            return;
        }
        if (firebaseMessaging.isEmpty()) {
            log.info("Firebase disabled, would push to {}: {}", userId, title);
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(token.get())
                    .putAllData(data)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build();
            firebaseMessaging.get().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed for user {}: {}", userId, e.getMessagingErrorCode(), e);
        }
    }
}
