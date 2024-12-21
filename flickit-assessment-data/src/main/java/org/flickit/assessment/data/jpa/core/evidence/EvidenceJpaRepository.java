package org.flickit.assessment.data.jpa.core.evidence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceJpaRepository extends JpaRepository<EvidenceJpaEntity, UUID> {

    boolean existsByIdAndDeletedFalse(UUID evidenceId);

    Optional<EvidenceJpaEntity> findByIdAndDeletedFalse(UUID id);

    @Query("""
            SELECT
                e.id as id,
                e.description as description,
                e.type as type,
                e.createdBy as createdBy,
                e.lastModificationTime as lastModificationTime,
                COUNT (a) as attachmentsCount
            FROM EvidenceJpaEntity e
            LEFT JOIN EvidenceAttachmentJpaEntity a on e.id = a.evidenceId
            WHERE e.questionId = :questionId AND e.assessmentId = :assessmentId AND e.deleted = false
            GROUP BY e.id, e.description, e.type, e.createdBy, e.lastModificationTime
        """)
    Page<EvidenceWithAttachmentsCountView> findByQuestionIdAndAssessmentId(@Param("questionId") Long questionId,
                                                                           @Param("assessmentId") UUID assessmentId,
                                                                           Pageable pageable);

    @Modifying
    @Query("""
            UPDATE EvidenceJpaEntity e
            SET e.description = :description,
                e.type = :type,
                e.lastModificationTime = :lastModificationTime,
                e.lastModifiedBy = :lastModifiedBy
            WHERE e.id = :id
        """)
    void update(@Param(value = "id") UUID id,
                @Param(value = "description") String description,
                @Param(value = "type") Integer type,
                @Param(value = "lastModificationTime") LocalDateTime lastModificationTime,
                @Param(value = "lastModifiedBy") UUID lastModifiedBy);

    @Modifying
    @Query("""
            UPDATE EvidenceJpaEntity e
            SET e.deleted = true
            WHERE e.id = :id
        """)
    void delete(@Param(value = "id") UUID id);

    @Query("""
            SELECT
                qsn.id as questionId,
                COUNT(e.id) as evidenceCount
            FROM QuestionJpaEntity qsn
            LEFT JOIN EvidenceJpaEntity e on qsn.id = e.questionId
            WHERE qsn.id IN :questionIds
                AND e.assessmentId = :assessmentId
                AND e.deleted = false
                AND e.type IS NOT NULL
        """)
    List<CountQuestionEvidenceView> countByAssessmentIdAndQuestionIds(@Param("assessmentId") UUID assessmentId,
                                                                      @Param("questionIds") Collection<Long> questionIds);

    @Modifying
    @Query("""
            UPDATE EvidenceJpaEntity e
            SET e.resolved = true,
                e.lastModifiedBy = :lastModifiedBy,
                e.lastModificationTime = :lastModificationTime
            WHERE e.id = :evidenceId
        """)
    void resolveComment(@Param("evidenceId") UUID evidenceId,
                        @Param("lastModifiedBy") UUID lastModifiedBy,
                        @Param("lastModificationTime") LocalDateTime lastModificationTime);
}
