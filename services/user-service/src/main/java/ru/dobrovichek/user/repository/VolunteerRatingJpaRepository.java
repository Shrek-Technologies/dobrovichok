package ru.dobrovichek.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.dobrovichek.user.entity.VolunteerRating;
import ru.dobrovichek.user.dto.VolunteerRatingSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface VolunteerRatingJpaRepository extends JpaRepository<VolunteerRating, UUID> {

    Optional<VolunteerRating> findByRequestId(UUID requestId);

    Optional<VolunteerRating> findByRequestIdAndVolunteerId(UUID requestId, UUID volunteerId);

    boolean existsByRequestId(UUID requestId);

    boolean existsByRequestIdAndVolunteerId(UUID requestId, UUID volunteerId);

    @Query("""
            select new ru.dobrovichek.user.dto.VolunteerRatingSnapshot(
                count(vr),
                coalesce(avg(vr.score), 0)
            )
            from VolunteerRating vr
            where vr.volunteerId = :volunteerId
            """)
    VolunteerRatingSnapshot getRatingSnapshot(@Param("volunteerId") UUID volunteerId);
}
