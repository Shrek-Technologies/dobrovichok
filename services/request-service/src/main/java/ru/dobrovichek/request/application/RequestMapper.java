package ru.dobrovichek.request.application;

import org.springframework.stereotype.Component;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.request.api.RequestResponse;
import ru.dobrovichek.request.api.RequestSummaryResponse;
import ru.dobrovichek.request.domain.HelpRequest;

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
