package ru.dobrovichek.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.dto.CreateRequestRequest;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.entity.HelpRequest;
import ru.dobrovichek.request.events.RequestEventPublisher;
import ru.dobrovichek.request.exception.ConflictException;
import ru.dobrovichek.request.exception.ForbiddenException;
import ru.dobrovichek.request.exception.RequestNotFoundException;
import ru.dobrovichek.request.repository.HelpRequestRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestCommandServiceTest {

    private static final Instant NOW = Instant.parse("2024-06-01T10:15:30Z");

    @Mock
    private HelpRequestRepository requestRepository;
    @Mock
    private RequestEventPublisher eventPublisher;

    private RequestCommandService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new RequestCommandService(requestRepository, eventPublisher, clock);
    }

    @Test
    void create_forbiddenWhenNotWard() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER);
        CreateRequestRequest cmd = new CreateRequestRequest(
                "help",
                "+71234567890",
                "Иван",
                "Иванов",
                null,
                new GeoPoint(55.75, 37.62)
        );

        assertThrows(ForbiddenException.class, () -> service.create(user, cmd));
        verifyNoInteractions(requestRepository, eventPublisher);
    }

    @Test
    void create_savesAndPublishes() {
        UUID wardId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(wardId, UserRole.WARD);
        CreateRequestRequest cmd = new CreateRequestRequest(
                " help ",
                "+7 (123) 456-78-90",
                "Иван",
                "Иванов",
                "  ",
                new GeoPoint(55.75, 37.62)
        );

        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest saved = service.create(user, cmd);

        assertEquals(wardId, saved.getWardId());
        assertEquals("help", saved.getDescription());
        assertEquals(RequestStatus.CREATED, saved.getStatus());
        assertEquals(NOW, saved.getCreatedAt());

        verify(requestRepository).save(saved);
        verify(eventPublisher).publishCreated(saved);
    }

    @Test
    void accept_forbiddenWhenNotVolunteer() {
        UUID id = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UserRole.WARD);

        assertThrows(ForbiddenException.class, () -> service.accept(id, user));
        verifyNoInteractions(requestRepository, eventPublisher);
    }

    @Test
    void accept_notFound() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class,
                () -> service.accept(id, new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void accept_conflictWhenNotCreated() {
        UUID wardId = UUID.randomUUID();
        HelpRequest request = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        request.accept(UUID.randomUUID(), NOW);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class,
                () -> service.accept(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void accept_success() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest out = service.accept(request.getId(), new CurrentUser(volunteerId, UserRole.VOLUNTEER));

        assertEquals(RequestStatus.ACCEPTED, out.getStatus());
        assertEquals(volunteerId, out.getVolunteerId());
        verify(eventPublisher).publishStatusChanged(out);
    }

    @Test
    void cancel_forbiddenWhenNotWard() {
        assertThrows(ForbiddenException.class,
                () -> service.cancel(UUID.randomUUID(), new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
        verifyNoInteractions(requestRepository, eventPublisher);
    }

    @Test
    void cancel_forbiddenWhenOtherWard() {
        HelpRequest request = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> service.cancel(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.WARD)));
    }

    @Test
    void cancel_success() {
        UUID wardId = UUID.randomUUID();
        HelpRequest request = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest out = service.cancel(request.getId(), new CurrentUser(wardId, UserRole.WARD));

        assertEquals(RequestStatus.CANCELLED, out.getStatus());
        verify(eventPublisher).publishStatusChanged(out);
    }

    @Test
    void complete_conflictWhenNotAccepted() {
        UUID wardId = UUID.randomUUID();
        HelpRequest request = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class,
                () -> service.complete(request.getId(), new CurrentUser(wardId, UserRole.WARD)));
    }

    @Test
    void complete_forbiddenForAdmin() {
        HelpRequest request = acceptedRequest(UUID.randomUUID(), UUID.randomUUID(), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> service.complete(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.ADMIN)));
    }

    @Test
    void complete_forbiddenWhenVolunteerNotAssigned() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> service.complete(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void complete_forbiddenWhenWardNotOwner() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> service.complete(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.WARD)));
    }

    @Test
    void complete_successForAssignedVolunteer() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest out = service.complete(request.getId(), new CurrentUser(volunteerId, UserRole.VOLUNTEER));

        assertEquals(RequestStatus.COMPLETED, out.getStatus());
        verify(eventPublisher).publishStatusChanged(out);
    }

    @Test
    void complete_successForWardOwner() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest out = service.complete(request.getId(), new CurrentUser(wardId, UserRole.WARD));

        assertEquals(RequestStatus.COMPLETED, out.getStatus());
        verify(eventPublisher).publishStatusChanged(out);
    }

    @Test
    void abandon_forbiddenWhenNotVolunteer() {
        assertThrows(ForbiddenException.class,
                () -> service.abandonByVolunteer(UUID.randomUUID(), new CurrentUser(UUID.randomUUID(), UserRole.WARD)));
    }

    @Test
    void abandon_conflictWhenNotAccepted() {
        UUID wardId = UUID.randomUUID();
        HelpRequest request = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class,
                () -> service.abandonByVolunteer(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void abandon_forbiddenWhenNotAssignedVolunteer() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> service.abandonByVolunteer(request.getId(), new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void abandon_success() {
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        HelpRequest request = acceptedRequest(wardId, volunteerId, NOW);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(HelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HelpRequest out = service.abandonByVolunteer(request.getId(), new CurrentUser(volunteerId, UserRole.VOLUNTEER));

        assertEquals(RequestStatus.CREATED, out.getStatus());
        assertNull(out.getVolunteerId());
        verify(eventPublisher).publishVolunteerAbandoned(eq(out.getId()), eq(wardId), eq(volunteerId), eq(NOW));
        verify(eventPublisher).publishStatusChanged(out);
    }

    private static HelpRequest acceptedRequest(UUID wardId, UUID volunteerId, Instant createdAt) {
        HelpRequest r = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), createdAt);
        r.accept(volunteerId, createdAt);
        return r;
    }
}
