package ru.dobrovichek.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.entity.HelpRequest;
import ru.dobrovichek.request.exception.ForbiddenException;
import ru.dobrovichek.request.exception.RequestNotFoundException;
import ru.dobrovichek.request.repository.HelpRequestRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestQueryServiceTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2024-01-02T00:00:00Z");

    @Mock
    private HelpRequestRepository requestRepository;

    private RequestQueryService service;

    @BeforeEach
    void setUp() {
        service = new RequestQueryService(requestRepository);
    }

    @Test
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void findActive_wardDelegates() {
        UUID wardId = UUID.randomUUID();
        HelpRequest active = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        when(requestRepository.findActiveForWard(wardId)).thenReturn(Optional.of(active));

        Optional<HelpRequest> out = service.findActiveRequest(new CurrentUser(wardId, UserRole.WARD));

        assertTrue(out.isPresent());
        assertEquals(active.getId(), out.get().getId());
    }

    @Test
    void findActive_volunteerDelegates() {
        UUID volunteerId = UUID.randomUUID();
        HelpRequest active = accepted(UUID.randomUUID(), volunteerId, T0);
        when(requestRepository.findActiveAcceptedForVolunteer(volunteerId)).thenReturn(Optional.of(active));

        Optional<HelpRequest> out = service.findActiveRequest(new CurrentUser(volunteerId, UserRole.VOLUNTEER));

        assertTrue(out.isPresent());
    }

    @Test
    void findActive_otherRoleEmpty() {
        assertTrue(service.findActiveRequest(new CurrentUser(UUID.randomUUID(), UserRole.ADMIN)).isEmpty());
    }

    @Test
    void assertCanRead_adminAlways() {
        HelpRequest r = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        service.assertCanRead(r, new CurrentUser(UUID.randomUUID(), UserRole.ADMIN));
    }

    @Test
    void assertCanRead_wardOwner() {
        UUID wardId = UUID.randomUUID();
        HelpRequest r = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        service.assertCanRead(r, new CurrentUser(wardId, UserRole.WARD));
    }

    @Test
    void assertCanRead_volunteerSeesCreatedPool() {
        HelpRequest r = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        service.assertCanRead(r, new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER));
    }

    @Test
    void assertCanRead_volunteerAssignedAccepted() {
        UUID volunteerId = UUID.randomUUID();
        HelpRequest r = accepted(UUID.randomUUID(), volunteerId, T0);
        service.assertCanRead(r, new CurrentUser(volunteerId, UserRole.VOLUNTEER));
    }

    @Test
    void assertCanRead_volunteerCannotSeeOthersAccepted() {
        HelpRequest r = accepted(UUID.randomUUID(), UUID.randomUUID(), T0);

        assertThrows(ForbiddenException.class,
                () -> service.assertCanRead(r, new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    @Test
    void findNearby_forbiddenWhenNotVolunteer() {
        assertThrows(ForbiddenException.class,
                () -> service.findNearby(
                        new CurrentUser(UUID.randomUUID(), UserRole.WARD),
                        55.0, 37.0, 5.0, Set.of(RequestStatus.CREATED)));
    }

    @Test
    void findNearby_usesDefaultStatusesWhenNullOrEmpty() {
        CurrentUser v = new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER);
        when(requestRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.CREATED)))).thenReturn(List.of());

        service.findNearby(v, 55.0, 37.0, 5.0, null);
        service.findNearby(v, 55.0, 37.0, 5.0, Set.of());

        verify(requestRepository, times(2)).findNearby(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.CREATED)));
    }

    @Test
    void findNearby_respectsCustomStatuses() {
        CurrentUser v = new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER);
        when(requestRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.ACCEPTED)))).thenReturn(List.of());

        service.findNearby(v, 55.0, 37.0, 5.0, Set.of(RequestStatus.ACCEPTED));

        verify(requestRepository).findNearby(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.ACCEPTED)));
    }

    @Test
    void findNearby_filtersByRadiusAndSorts() {
        CurrentUser v = new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER);
        HelpRequest near = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(55.0001, 37.0001), T1);
        HelpRequest far = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(60.0, 37.0), T0);
        when(requestRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.CREATED)))).thenReturn(List.of(far, near));

        List<RequestQueryService.NearbyRequestCandidate> out =
                service.findNearby(v, 55.0, 37.0, 5.0, Set.of(RequestStatus.CREATED));

        assertEquals(1, out.size());
        assertEquals(near.getId(), out.getFirst().request().getId());
    }

    @Test
    void findNearby_sortsByCreatedAtWhenDistanceEqual() {
        CurrentUser v = new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER);
        HelpRequest older = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(55.0, 37.0), T0);
        HelpRequest newer = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(55.0, 37.0), T1);
        when(requestRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(Set.of(RequestStatus.CREATED)))).thenReturn(List.of(newer, older));

        List<RequestQueryService.NearbyRequestCandidate> out =
                service.findNearby(v, 55.0, 37.0, 50.0, Set.of(RequestStatus.CREATED));

        assertEquals(2, out.size());
        assertEquals(older.getId(), out.get(0).request().getId());
        assertEquals(newer.getId(), out.get(1).request().getId());
    }

    @Test
    void canViewContact_admin() {
        HelpRequest r = HelpRequest.create(
                UUID.randomUUID(), "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        assertTrue(service.canViewContact(r, new CurrentUser(UUID.randomUUID(), UserRole.ADMIN)));
    }

    @Test
    void canViewContact_wardOwner() {
        UUID wardId = UUID.randomUUID();
        HelpRequest r = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), T0);
        assertTrue(service.canViewContact(r, new CurrentUser(wardId, UserRole.WARD)));
    }

    @Test
    void canViewContact_assignedVolunteerOnly() {
        UUID volunteerId = UUID.randomUUID();
        HelpRequest r = accepted(UUID.randomUUID(), volunteerId, T0);
        assertTrue(service.canViewContact(r, new CurrentUser(volunteerId, UserRole.VOLUNTEER)));
        assertFalse(service.canViewContact(r, new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER)));
    }

    private static HelpRequest accepted(UUID wardId, UUID volunteerId, Instant createdAt) {
        HelpRequest r = HelpRequest.create(
                wardId, "d", "+71234567890", "A", "B", null, new GeoPoint(0.0, 0.0), createdAt);
        r.accept(volunteerId, createdAt);
        return r;
    }
}
