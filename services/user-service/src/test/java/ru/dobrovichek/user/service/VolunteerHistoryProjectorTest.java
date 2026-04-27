package ru.dobrovichek.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.user.entity.UserProfile;
import ru.dobrovichek.user.entity.VolunteerRequestHistory;
import ru.dobrovichek.user.repository.UserProfileJpaRepository;
import ru.dobrovichek.user.repository.VolunteerRequestHistoryJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerHistoryProjectorTest {

    private static final Instant T0 = Instant.parse("2024-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2024-03-01T11:00:00Z");

    @Mock
    private VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository;
    @Mock
    private UserProfileJpaRepository userProfileRepository;
    @Mock
    private UserProfileService userProfileService;

    private VolunteerHistoryProjector projector;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(T1, ZoneOffset.UTC);
        projector = new VolunteerHistoryProjector(
                volunteerRequestHistoryRepository,
                userProfileRepository,
                userProfileService,
                clock
        );
    }

    @Test
    void project_noOpWhenVolunteerIdNull() {
        projector.project(new RequestStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, RequestStatus.ACCEPTED, T0));

        verify(volunteerRequestHistoryRepository, never()).save(any());
    }

    @Test
    void project_skipsWhenHistoryAlreadyCompleted() {
        UUID requestId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        VolunteerRequestHistory row = VolunteerRequestHistory.create(requestId, volunteerId, wardId, T0);
        row.apply(RequestStatus.COMPLETED, T0, wardId, volunteerId);
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.of(row));

        projector.project(new RequestStatusChangedEvent(
                requestId, wardId, volunteerId, RequestStatus.COMPLETED, T1));

        verify(volunteerRequestHistoryRepository, never()).save(any());
    }

    @Test
    void project_completedIncrementsVolunteerOnlyOnce() {
        UUID requestId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.empty());
        UserProfile volunteer = UserProfile.create(volunteerId, UserRole.VOLUNTEER, T0);
        when(userProfileService.getOrCreateVolunteerShell(eq(volunteerId), eq(T0))).thenReturn(volunteer);
        when(volunteerRequestHistoryRepository.save(any(VolunteerRequestHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        projector.project(new RequestStatusChangedEvent(
                requestId, wardId, volunteerId, RequestStatus.COMPLETED, T0));

        assertEquals(1, volunteer.getCompletedRequestsCount());
        verify(userProfileRepository).save(volunteer);
    }

    @Test
    void project_nonCompletedDoesNotIncrementCounter() {
        UUID requestId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.empty());
        UserProfile volunteer = UserProfile.create(volunteerId, UserRole.VOLUNTEER, T0);
        when(userProfileService.getOrCreateVolunteerShell(eq(volunteerId), eq(T0))).thenReturn(volunteer);
        when(volunteerRequestHistoryRepository.save(any(VolunteerRequestHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        projector.project(new RequestStatusChangedEvent(
                requestId, wardId, volunteerId, RequestStatus.ACCEPTED, T0));

        assertEquals(0, volunteer.getCompletedRequestsCount());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void project_updatesExistingRow() {
        UUID requestId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        VolunteerRequestHistory row = VolunteerRequestHistory.create(requestId, volunteerId, wardId, T0);
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.of(row));
        UserProfile volunteer = UserProfile.create(volunteerId, UserRole.VOLUNTEER, T0);
        when(userProfileService.getOrCreateVolunteerShell(eq(volunteerId), eq(T1))).thenReturn(volunteer);
        when(volunteerRequestHistoryRepository.save(any(VolunteerRequestHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        projector.project(new RequestStatusChangedEvent(
                requestId, wardId, volunteerId, RequestStatus.CANCELLED, T1));

        assertEquals(RequestStatus.CANCELLED, row.getStatus());
        verify(volunteerRequestHistoryRepository).save(row);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void findCompletedRequests_delegates() {
        UUID volunteerId = UUID.randomUUID();
        List<VolunteerRequestHistory> list = List.of();
        when(volunteerRequestHistoryRepository.findByVolunteerIdAndStatusOrderByCompletedAtDescUpdatedAtDesc(
                volunteerId, RequestStatus.COMPLETED)).thenReturn(list);

        assertEquals(list, projector.findCompletedRequests(volunteerId));
    }
}
