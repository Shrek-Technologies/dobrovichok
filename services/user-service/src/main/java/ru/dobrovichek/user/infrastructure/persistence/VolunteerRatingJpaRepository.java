package ru.dobrovichek.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.dobrovichek.user.domain.VolunteerRating;

import java.util.UUID;

public interface VolunteerRatingJpaRepository extends JpaRepository<VolunteerRating, UUID> {

    boolean existsByRequestId(UUID requestId);

    boolean existsByRequestIdAndVolunteerId(UUID requestId, UUID volunteerId);

    @Query("""
            select new ru.dobrovichek.user.infrastructure.persistence.VolunteerRatingSnapshot(
                count(vr),
                coalesce(avg(vr.score), 0)
            )
            from VolunteerRating vr
            where vr.volunteerId = :volunteerId
            """)
    VolunteerRatingSnapshot getRatingSnapshot(@Param("volunteerId") UUID volunteerId);
}
