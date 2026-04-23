package ru.dobrovichek.request.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.api.CurrentUser;
import ru.dobrovichek.request.application.port.out.HelpRequestRepository;
import ru.dobrovichek.request.domain.HelpRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RequestQueryService {

    private static final Set<RequestStatus> DEFAULT_NEARBY_STATUSES = Set.of(RequestStatus.CREATED);
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final HelpRequestRepository requestRepository;

    public RequestQueryService(HelpRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public HelpRequest getById(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    public void assertCanRead(HelpRequest request, CurrentUser currentUser) {
        if (currentUser.role() == UserRole.ADMIN) {
            return;
        }
        if (request.getWardId().equals(currentUser.userId())) {
            return;
        }
        if (currentUser.role() == UserRole.VOLUNTEER) {
            if (request.getStatus() == RequestStatus.CREATED) {
                return;
            }
            if (currentUser.userId().equals(request.getVolunteerId())) {
                return;
            }
        }
        throw new ForbiddenException("You do not have access to this request");
    }

    @Transactional(readOnly = true)
    public List<NearbyRequestCandidate> findNearby(
            CurrentUser currentUser,
            double latitude,
            double longitude,
            double radiusKm,
            Set<RequestStatus> statuses
    ) {
        if (currentUser.role() != UserRole.VOLUNTEER) {
            throw new ForbiddenException("Only volunteers can search nearby requests");
        }

        Set<RequestStatus> effectiveStatuses = statuses == null || statuses.isEmpty()
                ? DEFAULT_NEARBY_STATUSES
                : statuses;

        double latitudeDelta = radiusKm / 111.0d;
        double longitudeDivisor = Math.max(Math.cos(Math.toRadians(latitude)), 0.01d);
        double longitudeDelta = radiusKm / (111.320d * longitudeDivisor);

        return requestRepository.findNearby(
                        latitude - latitudeDelta,
                        latitude + latitudeDelta,
                        longitude - longitudeDelta,
                        longitude + longitudeDelta,
                        effectiveStatuses
                ).stream()
                .map(request -> new NearbyRequestCandidate(request, distance(latitude, longitude, request)))
                .filter(candidate -> candidate.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyRequestCandidate::distanceKm)
                        .thenComparing(candidate -> candidate.request().getCreatedAt()))
                .toList();
    }

    public boolean canViewContact(HelpRequest request, CurrentUser currentUser) {
        if (currentUser.role() == UserRole.ADMIN) {
            return true;
        }
        if (request.getWardId().equals(currentUser.userId())) {
            return true;
        }
        return currentUser.role() == UserRole.VOLUNTEER && currentUser.userId().equals(request.getVolunteerId());
    }

    private double distance(double latitude, double longitude, HelpRequest request) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(request.getLatitude());
        double latDelta = Math.toRadians(request.getLatitude() - latitude);
        double lonDelta = Math.toRadians(request.getLongitude() - longitude);

        double a = Math.sin(latDelta / 2.0d) * Math.sin(latDelta / 2.0d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lonDelta / 2.0d) * Math.sin(lonDelta / 2.0d);
        double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
        return EARTH_RADIUS_KM * c;
    }

    public record NearbyRequestCandidate(
            HelpRequest request,
            double distanceKm
    ) {
    }
}
