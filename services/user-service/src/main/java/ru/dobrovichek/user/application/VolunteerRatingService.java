package ru.dobrovichek.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.user.api.CreateVolunteerRatingRequest;
import ru.dobrovichek.user.api.CurrentUser;
import ru.dobrovichek.user.domain.UserProfile;
import ru.dobrovichek.user.domain.VolunteerRating;
import ru.dobrovichek.user.domain.VolunteerRequestHistory;
import ru.dobrovichek.user.infrastructure.persistence.UserProfileJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRatingJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRatingSnapshot;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRequestHistoryJpaRepository;
import ru.dobrovichek.user.infrastructure.request.RequestServiceClient;
import ru.dobrovichek.user.infrastructure.request.RequestSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class VolunteerRatingService {

    private final VolunteerRatingJpaRepository volunteerRatingRepository;
    private final VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository;
    private final UserProfileJpaRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final RequestServiceClient requestServiceClient;
    private final Clock clock;

    public VolunteerRatingService(
            VolunteerRatingJpaRepository volunteerRatingRepository,
            VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository,
            UserProfileJpaRepository userProfileRepository,
            UserProfileService userProfileService,
            RequestServiceClient requestServiceClient,
            Clock clock
    ) {
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.volunteerRequestHistoryRepository = volunteerRequestHistoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
        this.requestServiceClient = requestServiceClient;
        this.clock = clock;
    }

    /**
     * Оценка разрешена только если заявка в request-service в статусе COMPLETED для этого подопечного и волонтёра.
     * Локальная volunteer_request_history — проекция для списков; не источник истины для этого сценария.
     */
    @Transactional
    public VolunteerRating create(CurrentUser currentUser, UUID volunteerId, CreateVolunteerRatingRequest request) {
        requireWard(currentUser);
        userProfileService.ensureVolunteerExists(volunteerId);

        RequestSnapshot live = fetchRequestOrThrow(request.requestId(), currentUser.userId());
        assertRatingAllowed(live, volunteerId, currentUser.userId());

        if (volunteerRatingRepository.existsByRequestIdAndVolunteerId(request.requestId(), volunteerId)) {
            throw new ConflictException("Rating for this request already exists");
        }

        VolunteerRating rating = VolunteerRating.create(
                request.requestId(),
                volunteerId,
                currentUser.userId(),
                request.score(),
                Instant.now(clock)
        );
        VolunteerRating saved = volunteerRatingRepository.save(rating);
        refreshVolunteerRating(volunteerId, saved.getCreatedAt());
        syncVolunteerRequestHistory(live, volunteerId);
        return saved;
    }

    private RequestSnapshot fetchRequestOrThrow(UUID requestId, UUID wardId) {
        try {
            return requestServiceClient.getRequestAsWard(requestId, wardId)
                    .orElseThrow(() -> new ConflictException("Request not found or access denied"));
        } catch (RestClientException e) {
            throw new ConflictException(
                    "Cannot reach request-service to verify the request. "
                            + "Set dobrovichek.request-service.base-url (e.g. REQUEST_SERVICE_BASE_URL) so user-service can call it.",
                    e
            );
        }
    }

    private static void assertRatingAllowed(RequestSnapshot live, UUID volunteerId, UUID wardId) {
        if (live.status() != RequestStatus.COMPLETED) {
            throw new ConflictException("Rating can be left only for completed requests");
        }
        if (live.volunteerId() == null || !live.volunteerId().equals(volunteerId)) {
            throw new ConflictException("Rating request does not belong to the specified volunteer");
        }
        if (!live.wardId().equals(wardId)) {
            throw new ForbiddenException("You can rate only your own completed requests");
        }
    }

    private void refreshVolunteerRating(UUID volunteerId, Instant now) {
        VolunteerRatingSnapshot snapshot = volunteerRatingRepository.getRatingSnapshot(volunteerId);
        UserProfile volunteer = userProfileService.getVolunteerProfile(volunteerId);
        volunteer.updateRating(toBigDecimal(snapshot.averageScore()), Math.toIntExact(snapshot.ratingCount()), now);
        userProfileRepository.save(volunteer);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void applyAbandonmentPenalty(UUID volunteerId, UUID requestId, UUID wardId) {
        if (volunteerRatingRepository.existsByRequestIdAndVolunteerId(requestId, volunteerId)) {
            return;
        }
        VolunteerRating rating = VolunteerRating.create(
                requestId,
                volunteerId,
                wardId,
                1,
                Instant.now(clock)
        );
        volunteerRatingRepository.save(rating);
        refreshVolunteerRating(volunteerId, rating.getCreatedAt());
        volunteerRequestHistoryRepository.deleteById(requestId);
    }

    private void requireWard(CurrentUser currentUser) {
        if (currentUser.role() != UserRole.WARD) {
            throw new ForbiddenException("Only wards can leave volunteer ratings");
        }
    }

    /** Подтягивает read-model истории; счётчик завершённых у волонтёра — только при первом переходе в COMPLETED. */
    private void syncVolunteerRequestHistory(RequestSnapshot live, UUID volunteerId) {
        UUID requestId = live.id();
        UUID wardId = live.wardId();
        Instant completedAt = live.completedAt() != null ? live.completedAt() : Instant.now(clock);
        VolunteerRequestHistory row = volunteerRequestHistoryRepository.findById(requestId).orElse(null);
        if (row == null) {
            Instant acceptedAt = live.acceptedAt() != null ? live.acceptedAt() : completedAt.minusSeconds(1);
            row = VolunteerRequestHistory.create(requestId, volunteerId, wardId, acceptedAt);
        }
        RequestStatus previous = row.getStatus();
        row.apply(RequestStatus.COMPLETED, completedAt, wardId, volunteerId);
        volunteerRequestHistoryRepository.save(row);
        if (previous != RequestStatus.COMPLETED) {
            UserProfile volunteer = userProfileService.getOrCreateVolunteerShell(volunteerId, completedAt);
            volunteer.registerCompletedRequest(completedAt);
            userProfileRepository.save(volunteer);
        }
    }
}
