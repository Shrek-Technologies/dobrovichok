package ru.dobrovichek.user.util;

import org.springframework.stereotype.Component;
import ru.dobrovichek.user.dto.UserProfileResponse;
import ru.dobrovichek.user.dto.VolunteerProfileResponse;
import ru.dobrovichek.user.dto.VolunteerRatingResponse;
import ru.dobrovichek.user.dto.VolunteerRequestHistoryResponse;
import ru.dobrovichek.user.entity.UserProfile;
import ru.dobrovichek.user.entity.VolunteerRating;
import ru.dobrovichek.user.entity.VolunteerRequestHistory;

@Component
public class UserProfileMapper {

    public UserProfileResponse toUserProfileResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getRole(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPatronymic(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getBio(),
                profile.getCity(),
                profile.getRating(),
                profile.getRatingCount(),
                profile.getCompletedRequestsCount(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public VolunteerProfileResponse toVolunteerProfileResponse(UserProfile profile) {
        return new VolunteerProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPatronymic(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getBio(),
                profile.getCity(),
                profile.getRating(),
                profile.getRatingCount(),
                profile.getCompletedRequestsCount(),
                profile.getUpdatedAt()
        );
    }

    public VolunteerRequestHistoryResponse toVolunteerRequestHistoryResponse(
            VolunteerRequestHistory history,
            String category,
            String address,
            Integer wardRating
    ) {
        return new VolunteerRequestHistoryResponse(
                history.getRequestId(),
                history.getWardId(),
                history.getStatus(),
                history.getAcceptedAt(),
                history.getCompletedAt(),
                history.getCancelledAt(),
                history.getUpdatedAt(),
                category,
                address,
                wardRating
        );
    }

    public VolunteerRatingResponse toVolunteerRatingResponse(VolunteerRating rating) {
        return new VolunteerRatingResponse(
                rating.getId(),
                rating.getRequestId(),
                rating.getVolunteerId(),
                rating.getWardId(),
                rating.getScore(),
                rating.getCreatedAt()
        );
    }
}
