package ru.dobrovichek.notification.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.notification.service.NotificationDispatchService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestEventListenerTest {

    private static final UUID REQUEST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID WARD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VOLUNTEER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private NotificationDispatchService dispatchService;

    @InjectMocks
    private RequestEventListener listener;

    @Test
    void delegatesRequestCreated() {
        var event = new RequestCreatedEvent(
                REQUEST_ID, WARD_ID, new GeoPoint(1.0, 2.0), Instant.now());
        listener.handleRequestCreated(event);
        verify(dispatchService).handle(event);
    }

    @Test
    void delegatesStatusChanged() {
        var event = new RequestStatusChangedEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, RequestStatus.ACCEPTED, Instant.now());
        listener.handleRequestStatusChanged(event);
        verify(dispatchService).handle(event);
    }

    @Test
    void delegatesVolunteerAbandoned() {
        var event = new VolunteerAbandonedRequestEvent(
                REQUEST_ID, WARD_ID, VOLUNTEER_ID, Instant.now());
        listener.handleVolunteerAbandoned(event);
        verify(dispatchService).handle(event);
    }
}
