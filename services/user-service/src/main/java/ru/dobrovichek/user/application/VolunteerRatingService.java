package ru.dobrovichek.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final Clock clock;

    public VolunteerRatingService(
            VolunteerRatingJpaRepository volunteerRatingRepository,
            VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository,
            UserProfileJpaRepository userProfileRepository,
            UserProfileService userProfileService,
            Clock clock
    ) {
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.volunteerRequestHistoryRepository = volunteerRequestHistoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
        this.clock = clock;
    }

    @Transactional
    public VolunteerRating create(CurrentUser currentUser, UUID volunteerId, CreateVolunteerRatingRequest request) {
        requireWard(currentUser);
        userProfileService.ensureVolunteerExists(volunteerId);

        VolunteerRequestHistory history = volunteerRequestHistoryRepository.findById(request.requestId())
                .orElseThrow(() -> new ConflictException("Completed request history not found for rating"));

        if (!history.getVolunteerId().equals(volunteerId)) {
            throw new ConflictException("Rating request does not belong to the specified volunteer");
        }
        if (history.getStatus() != RequestStatus.COMPLETED) {
            throw new ConflictException("Rating can be left only for completed requests");
        }
        if (!history.getWardId().equals(currentUser.userId())) {
            throw new ForbiddenException("You can rate only your own completed requests");
        }
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
        return saved;
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
}
