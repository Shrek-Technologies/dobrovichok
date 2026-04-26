package ru.dobrovichek.user.util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import ru.dobrovichek.user.dto.VolunteerRequestHistoryResponse;
import ru.dobrovichek.user.entity.VolunteerRating;
import ru.dobrovichek.user.entity.VolunteerRequestHistory;
import ru.dobrovichek.user.repository.VolunteerRatingJpaRepository;
import ru.dobrovichek.user.dto.RequestRichSnapshot;
import ru.dobrovichek.user.service.VolunteerHistoryProjector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VolunteerRequestHistoryAssembler {

    private final VolunteerHistoryProjector volunteerHistoryProjector;
    private final RequestServiceClient requestServiceClient;
    private final VolunteerRatingJpaRepository volunteerRatingRepository;
    private final UserProfileMapper userProfileMapper;

    public VolunteerRequestHistoryAssembler(
            VolunteerHistoryProjector volunteerHistoryProjector,
            RequestServiceClient requestServiceClient,
            VolunteerRatingJpaRepository volunteerRatingRepository,
            UserProfileMapper userProfileMapper
    ) {
        this.volunteerHistoryProjector = volunteerHistoryProjector;
        this.requestServiceClient = requestServiceClient;
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional(readOnly = true)
    public List<VolunteerRequestHistoryResponse> completedForVolunteer(UUID volunteerId) {
        List<VolunteerRequestHistory> rows =
                volunteerHistoryProjector.findCompletedRequests(volunteerId);
        return rows.stream().map(row -> toResponse(row, volunteerId)).toList();
    }

    private VolunteerRequestHistoryResponse toResponse(VolunteerRequestHistory history, UUID volunteerId) {
        Integer wardRating = volunteerRatingRepository.findByRequestId(history.getRequestId())
                .filter(r -> r.getVolunteerId().equals(volunteerId))
                .map(VolunteerRating::getScore)
                .orElse(null);

        String category = null;
        String address = null;
        try {
            Optional<RequestRichSnapshot> snap = Optional.ofNullable(
                    requestServiceClient.getRequestAsVolunteer(history.getRequestId(), volunteerId)
            ).orElse(Optional.empty());
            if (snap.isPresent()) {
                String desc = snap.get().description();
                category = HelpRequestDescriptionParser.category(desc);
                address = HelpRequestDescriptionParser.address(desc);
            }
        } catch (RestClientException ignored) {}

        return userProfileMapper.toVolunteerRequestHistoryResponse(history, category, address, wardRating);
    }
}
