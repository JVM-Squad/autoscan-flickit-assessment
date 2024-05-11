package org.flickit.assessment.data.jpa.users.space;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpaceJpaRepository extends JpaRepository<SpaceJpaEntity, Long> {

    @Query("""
            SELECT
                s.id as id,
                s.code as code,
                s.title as title,
                s.ownerId as ownerId,
                s.lastModificationTime as lastModificationTime,
                COUNT(DISTINCT sua.userId) as membersCount,
                COUNT(DISTINCT fa.id) as assessmentsCount,
                MAX(sua.lastSeen) as lastSeen
            FROM SpaceJpaEntity s
            LEFT JOIN AssessmentJpaEntity fa on s.id = fa.spaceId
            LEFT JOIN SpaceUserAccessJpaEntity sua on s.id = sua.spaceId
            WHERE EXISTS (
                SELECT 1 FROM SpaceUserAccessJpaEntity sua
                WHERE sua.spaceId = s.id AND sua.userId = :userId
            )
            GROUP BY s.id
            ORDER BY lastSeen DESC
        """)
    Page<SpaceWithDetailsView> findByUserId(@Param(value = "userId") UUID userId, Pageable pageable);
}
