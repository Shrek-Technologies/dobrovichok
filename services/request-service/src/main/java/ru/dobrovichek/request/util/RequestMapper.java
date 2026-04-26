package ru.dobrovichek.request.util;

import org.springframework.stereotype.Component;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.request.dto.RequestResponse;
import ru.dobrovichek.request.dto.RequestSummaryResponse;
import ru.dobrovichek.request.entity.HelpRequest;

@Component
public class RequestMapper {

    public RequestResponse toResponse(HelpRequest request, boolean includeContactPhone) {
        return new RequestResponse(
                request.getId(),
                request.getWardId(),
                request.getVolunteerId(),
                request.getDescription(),
                includeContactPhone ? request.getContactPhone() : null,
                request.getWardFirstName(),
                request.getWardLastName(),
                request.getWardPatronymic(),
                PersonNameFormat.fullFormal(
                        request.getWardFirstName(),
                        request.getWardPatronymic(),
                        request.getWardLastName()
                ),
                request.getLocation(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getAcceptedAt(),
                request.getCompletedAt(),
                request.getCancelledAt()
        );
    }

    public RequestSummaryResponse toSummaryResponse(HelpRequest request, double distanceKm) {
        return new RequestSummaryResponse(
                request.getId(),
                request.getWardId(),
                PersonNameFormat.firstNameOnly(request.getWardFirstName()),
                request.getDescription(),
                request.getLocation(),
                request.getStatus(),
                request.getCreatedAt(),
                distanceKm
        );
    }
}
