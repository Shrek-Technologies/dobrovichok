package ru.dobrovichek.user.api;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.dobrovichek.user.application.UserProfileMapper;
import ru.dobrovichek.user.application.UserProfileService;
import ru.dobrovichek.user.application.VolunteerRatingService;
import ru.dobrovichek.user.application.VolunteerHistoryProjector;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserProfileService userProfileService;
    private final VolunteerHistoryProjector volunteerHistoryProjector;
    private final VolunteerRatingService volunteerRatingService;
    private final UserProfileMapper mapper;

    public UserController(
            UserProfileService userProfileService,
            VolunteerHistoryProjector volunteerHistoryProjector,
            VolunteerRatingService volunteerRatingService,
            UserProfileMapper mapper
    ) {
        this.userProfileService = userProfileService;
        this.volunteerHistoryProjector = volunteerHistoryProjector;
        this.volunteerRatingService = volunteerRatingService;
        this.mapper = mapper;
    }

    @GetMapping("/users/me")
    public UserProfileResponse getMyProfile(CurrentUser currentUser) {
        return mapper.toUserProfileResponse(userProfileService.getOrCreateCurrent(currentUser));
    }

    @PutMapping("/users/me")
    public UserProfileResponse updateMyProfile(
            CurrentUser currentUser,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return mapper.toUserProfileResponse(userProfileService.upsertCurrent(currentUser, request));
    }

    @GetMapping("/volunteers/{volunteerId}")
    public VolunteerProfileResponse getVolunteerProfile(@PathVariable UUID volunteerId) {
        return mapper.toVolunteerProfileResponse(userProfileService.getVolunteerProfile(volunteerId));
    }

    @GetMapping("/volunteers/{volunteerId}/requests/history")
    public List<VolunteerRequestHistoryResponse> getVolunteerHistory(@PathVariable UUID volunteerId) {
        userProfileService.ensureVolunteerExists(volunteerId);
        return volunteerHistoryProjector.findCompletedRequests(volunteerId).stream()
                .map(mapper::toVolunteerRequestHistoryResponse)
                .toList();
    }

    @PostMapping("/volunteers/{volunteerId}/ratings")
    public VolunteerRatingResponse createVolunteerRating(
            @PathVariable UUID volunteerId,
            CurrentUser currentUser,
            @Valid @RequestBody CreateVolunteerRatingRequest request
    ) {
        return mapper.toVolunteerRatingResponse(volunteerRatingService.create(currentUser, volunteerId, request));
    }
}
