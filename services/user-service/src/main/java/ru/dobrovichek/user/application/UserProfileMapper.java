package ru.dobrovichek.user.application;

import org.springframework.stereotype.Component;
import ru.dobrovichek.user.api.UserProfileResponse;
import ru.dobrovichek.user.api.VolunteerProfileResponse;
import ru.dobrovichek.user.api.VolunteerRequestHistoryResponse;
import ru.dobrovichek.user.domain.UserProfile;
import ru.dobrovichek.user.domain.VolunteerRequestHistory;

@Component
public class UserProfileMapper {

    public UserProfileResponse toUserProfileResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getRole(),
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
                profile.getFullName(),
                profile.getBio(),
                profile.getCity(),
                profile.getRating(),
                profile.getRatingCount(),
                profile.getCompletedRequestsCount(),
                profile.getUpdatedAt()
        );
    }

    public VolunteerRequestHistoryResponse toVolunteerRequestHistoryResponse(VolunteerRequestHistory history) {
        return new VolunteerRequestHistoryResponse(
                history.getRequestId(),
                history.getWardId(),
                history.getStatus(),
                history.getAcceptedAt(),
                history.getCompletedAt(),
                history.getCancelledAt(),
                history.getUpdatedAt()
        );
    }
}
