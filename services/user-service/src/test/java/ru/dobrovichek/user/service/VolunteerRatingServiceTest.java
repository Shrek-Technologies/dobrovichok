package ru.dobrovichek.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.user.dto.CreateVolunteerRatingRequest;
import ru.dobrovichek.user.dto.CurrentUser;
import ru.dobrovichek.user.dto.RequestSnapshot;
import ru.dobrovichek.user.dto.VolunteerRatingSnapshot;
import ru.dobrovichek.user.entity.UserProfile;
import ru.dobrovichek.user.entity.VolunteerRating;
import ru.dobrovichek.user.entity.VolunteerRequestHistory;
import ru.dobrovichek.user.exception.ConflictException;
import ru.dobrovichek.user.exception.ForbiddenException;
import ru.dobrovichek.user.repository.UserProfileJpaRepository;
import ru.dobrovichek.user.repository.VolunteerRatingJpaRepository;
import ru.dobrovichek.user.repository.VolunteerRequestHistoryJpaRepository;
import ru.dobrovichek.user.util.RequestServiceClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerRatingServiceTest {

    private static final Instant NOW = Instant.parse("2024-08-01T12:00:00Z");

    @Mock
    private VolunteerRatingJpaRepository volunteerRatingRepository;
    @Mock
    private VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository;
    @Mock
    private UserProfileJpaRepository userProfileRepository;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private RequestServiceClient requestServiceClient;

    private VolunteerRatingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new VolunteerRatingService(
                volunteerRatingRepository,
                volunteerRequestHistoryRepository,
                userProfileRepository,
                userProfileService,
                requestServiceClient,
                clock,
                new BigDecimal("0.8")
        );
    }

    @Test
    void create_forbiddenWhenNotWard() {
        assertThrows(ForbiddenException.class,
                () -> service.create(
                        new CurrentUser(UUID.randomUUID(), UserRole.VOLUNTEER),
                        UUID.randomUUID(),
                        new CreateVolunteerRatingRequest(UUID.randomUUID(), 5)));
    }

    @Test
    void create_conflictWhenDuplicate() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        stubLiveRequest(requestId, wardId, volunteerId, RequestStatus.COMPLETED);
        when(volunteerRatingRepository.existsByRequestIdAndVolunteerId(requestId, volunteerId)).thenReturn(true);
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        assertThrows(ConflictException.class,
                () -> service.create(
                        new CurrentUser(wardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));
    }

    @Test
    void create_conflictWhenNotCompleted() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        stubLiveRequest(requestId, wardId, volunteerId, RequestStatus.ACCEPTED);
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        assertThrows(ConflictException.class,
                () -> service.create(
                        new CurrentUser(wardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));
    }

    @Test
    void create_conflictWhenVolunteerMismatch() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        stubLiveRequest(requestId, wardId, UUID.randomUUID(), RequestStatus.COMPLETED);
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        assertThrows(ConflictException.class,
                () -> service.create(
                        new CurrentUser(wardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));
    }

    @Test
    void create_forbiddenWhenOtherWard() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID ownerWardId = UUID.randomUUID();
        UUID otherWardId = UUID.randomUUID();
        RequestSnapshot live = new RequestSnapshot(
                requestId, ownerWardId, volunteerId, RequestStatus.COMPLETED, NOW.minusSeconds(60), NOW.minusSeconds(30));
        when(requestServiceClient.getRequestAsWard(requestId, otherWardId)).thenReturn(Optional.of(live));
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        assertThrows(ForbiddenException.class,
                () -> service.create(
                        new CurrentUser(otherWardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));
    }

    @Test
    void create_conflictWhenRequestMissingInRequestService() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        when(requestServiceClient.getRequestAsWard(requestId, wardId)).thenReturn(Optional.empty());
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        assertThrows(ConflictException.class,
                () -> service.create(
                        new CurrentUser(wardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));
    }

    @Test
    void create_wrapsRestClientException() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        when(requestServiceClient.getRequestAsWard(requestId, wardId)).thenThrow(new RestClientException("boom"));
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.create(
                        new CurrentUser(wardId, UserRole.WARD),
                        volunteerId,
                        new CreateVolunteerRatingRequest(requestId, 5)));

        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("request-service"));
    }

    @Test
    void create_successUpdatesProfileAndHistory() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        Instant completedAt = NOW.minusSeconds(30);
        RequestSnapshot live = new RequestSnapshot(
                requestId, wardId, volunteerId, RequestStatus.COMPLETED, NOW.minusSeconds(120), completedAt);
        when(requestServiceClient.getRequestAsWard(requestId, wardId)).thenReturn(Optional.of(live));
        when(volunteerRatingRepository.existsByRequestIdAndVolunteerId(requestId, volunteerId)).thenReturn(false);
        when(volunteerRatingRepository.save(any(VolunteerRating.class))).thenAnswer(inv -> inv.getArgument(0));
        when(volunteerRatingRepository.getRatingSnapshot(volunteerId)).thenReturn(new VolunteerRatingSnapshot(2L, 3.5));
        UserProfile volunteer = UserProfile.create(volunteerId, UserRole.VOLUNTEER, NOW);
        when(userProfileService.getVolunteerProfile(volunteerId)).thenReturn(volunteer);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.empty());
        when(userProfileService.getOrCreateVolunteerShell(volunteerId, completedAt)).thenReturn(volunteer);
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        VolunteerRating saved = service.create(
                new CurrentUser(wardId, UserRole.WARD),
                volunteerId,
                new CreateVolunteerRatingRequest(requestId, 4));

        assertEquals(requestId, saved.getRequestId());
        assertEquals(4, saved.getScore());
        verify(volunteerRequestHistoryRepository).save(any(VolunteerRequestHistory.class));
        verify(userProfileService).getOrCreateVolunteerShell(volunteerId, completedAt);
    }

    @Test
    void create_doesNotIncrementCompletedWhenHistoryAlreadyCompleted() {
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        Instant completedAt = NOW.minusSeconds(10);
        RequestSnapshot live = new RequestSnapshot(
                requestId, wardId, volunteerId, RequestStatus.COMPLETED, NOW.minusSeconds(50), completedAt);
        when(requestServiceClient.getRequestAsWard(requestId, wardId)).thenReturn(Optional.of(live));
        when(volunteerRatingRepository.existsByRequestIdAndVolunteerId(requestId, volunteerId)).thenReturn(false);
        when(volunteerRatingRepository.save(any(VolunteerRating.class))).thenAnswer(inv -> inv.getArgument(0));
        when(volunteerRatingRepository.getRatingSnapshot(volunteerId)).thenReturn(new VolunteerRatingSnapshot(1L, 5.0));
        UserProfile volunteer = UserProfile.create(volunteerId, UserRole.VOLUNTEER, NOW);
        volunteer.registerCompletedRequest(NOW.minusSeconds(100));
        int countBefore = volunteer.getCompletedRequestsCount();
        when(userProfileService.getVolunteerProfile(volunteerId)).thenReturn(volunteer);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        VolunteerRequestHistory row = VolunteerRequestHistory.create(requestId, volunteerId, wardId, NOW.minusSeconds(80));
        row.apply(RequestStatus.COMPLETED, NOW.minusSeconds(20), wardId, volunteerId);
        when(volunteerRequestHistoryRepository.findById(requestId)).thenReturn(Optional.of(row));
        doNothing().when(userProfileService).ensureVolunteerExists(volunteerId);

        UserProfile same = volunteer;
        service.create(
                new CurrentUser(wardId, UserRole.WARD),
                volunteerId,
                new CreateVolunteerRatingRequest(requestId, 5));

        assertEquals(countBefore, same.getCompletedRequestsCount());
        verify(userProfileService, never()).getOrCreateVolunteerShell(eq(volunteerId), any());
    }

    @Test
    void applyAbandonmentPenalty_noOpWhenHistoryMissing() {
        UUID requestId = UUID.randomUUID();
        when(volunteerRequestHistoryRepository.existsById(requestId)).thenReturn(false);

        service.applyAbandonmentPenalty(UUID.randomUUID(), requestId, UUID.randomUUID());

        verify(userProfileService, never()).getOrCreateVolunteerShell(any(), any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void applyAbandonmentPenalty_appliesMultiplierAndDeletesHistory() {
        UUID volunteerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(volunteerRequestHistoryRepository.existsById(requestId)).thenReturn(true);
        UserProfile shell = UserProfile.create(volunteerId, UserRole.VOLUNTEER, NOW);
        when(userProfileService.getOrCreateVolunteerShell(volunteerId, NOW)).thenReturn(shell);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.applyAbandonmentPenalty(volunteerId, requestId, UUID.randomUUID());

        assertEquals(new BigDecimal("0.00"), shell.getRating());
        verify(volunteerRequestHistoryRepository).deleteById(requestId);
        verify(userProfileRepository).save(eq(shell));
    }

    private void stubLiveRequest(UUID requestId, UUID wardId, UUID volunteerId, RequestStatus status) {
        RequestSnapshot live = new RequestSnapshot(
                requestId, wardId, volunteerId, status, NOW.minusSeconds(60), NOW.minusSeconds(30));
        when(requestServiceClient.getRequestAsWard(requestId, wardId)).thenReturn(Optional.of(live));
    }
}
