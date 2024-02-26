package org.flickit.assessment.data.jpa.kit.maturitylevel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaturityLevelJpaRepository extends JpaRepository<MaturityLevelJpaEntity, Long> {

    List<MaturityLevelJpaEntity> findAllByKitId(Long kitId);


    @Query("""
            SELECT l as maturityLevel, c as levelCompetence
            FROM MaturityLevelJpaEntity l
            LEFT JOIN LevelCompetenceJpaEntity c ON l.id = c.affectedLevel.id
            WHERE l.kitId = :kitId
        """)
    List<MaturityJoinCompetenceView> findAllByKitIdWithCompetence(Long kitId);

    @Query("""
            FROM MaturityLevelJpaEntity ml WHERE
            ml.kitId = (SELECT l.kitId FROM MaturityLevelJpaEntity AS l WHERE l.id = :id)
        """)
    List<MaturityLevelJpaEntity> findAllInKitWithOneId(Long id);
}
