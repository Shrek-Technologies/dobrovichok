package ru.dobrovichek.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.notification.infrastructure.user.UserDirectoryClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    private static final UUID REQUEST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID WARD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VOLUNTEER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private UserDirectoryClient userDirectoryClient;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    void requestCreatedDoesNotThrow() {
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, null);
        service.handle(new RequestCreatedEvent(
                REQUEST_ID,
                WARD_ID,
                new GeoPoint(59.93, 30.33),
                Instant.parse("2025-01-01T12:00:00Z")
        ));
        verify(userDirectoryClient, never()).findFcmToken(any());
    }

    @Test
    void volunteerAbandonedSkipsWhenNoToken() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.empty());
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new VolunteerAbandonedRequestEvent(REQUEST_ID, WARD_ID, VOLUNTEER_ID, Instant.now()));
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void volunteerAbandonedLogsWhenFirebaseDisabled() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.of("device-token"));
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, null);
        service.handle(new VolunteerAbandonedRequestEvent(REQUEST_ID, WARD_ID, VOLUNTEER_ID, Instant.now()));
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void volunteerAbandonedSendsFcmWhenEnabled() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.of("device-token"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("mid");
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new VolunteerAbandonedRequestEvent(REQUEST_ID, WARD_ID, VOLUNTEER_ID, Instant.now()));
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void volunteerAbandonedSwallowsFcmFailure() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.of("device-token"));
        when(firebaseMessaging.send(any(Message.class))).thenThrow(mockMessagingException());
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new VolunteerAbandonedRequestEvent(REQUEST_ID, WARD_ID, VOLUNTEER_ID, Instant.now()));
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void statusAcceptedNotifiesWard() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.of("t"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("mid");
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, RequestStatus.ACCEPTED, Instant.now()));
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void statusCancelledNotifiesVolunteerWhenPresent() throws Exception {
        when(userDirectoryClient.findFcmToken(VOLUNTEER_ID)).thenReturn(Optional.of("t"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("mid");
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, RequestStatus.CANCELLED, Instant.now()));
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void statusCancelledWithoutVolunteerSkipsPush() throws Exception {
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, null, RequestStatus.CANCELLED, Instant.now()));
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void statusCompletedNotifiesWard() throws Exception {
        when(userDirectoryClient.findFcmToken(WARD_ID)).thenReturn(Optional.of("t"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("mid");
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, RequestStatus.COMPLETED, Instant.now()));
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void statusAcceptedWithoutVolunteerDoesNothing() throws Exception {
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, null, RequestStatus.ACCEPTED, Instant.now()));
        verifyNoInteractions(userDirectoryClient);
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void statusCreatedDoesNotNotify() throws Exception {
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, RequestStatus.CREATED, Instant.now()));
        verifyNoInteractions(userDirectoryClient);
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void statusCompletedWithoutVolunteerDoesNothing() throws Exception {
        NotificationDispatchService service = new NotificationDispatchService(userDirectoryClient, firebaseMessaging);
        service.handle(new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, null, RequestStatus.COMPLETED, Instant.now()));
        verifyNoInteractions(userDirectoryClient);
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    private static com.google.firebase.messaging.FirebaseMessagingException mockMessagingException() {
        return mock(com.google.firebase.messaging.FirebaseMessagingException.class);
    }
}
