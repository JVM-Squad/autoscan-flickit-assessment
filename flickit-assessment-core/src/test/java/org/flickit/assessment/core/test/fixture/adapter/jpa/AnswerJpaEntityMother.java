package org.flickit.assessment.core.test.fixture.adapter.jpa;

import org.flickit.assessment.core.adapter.out.persistence.answer.AnswerJpaEntity;
import org.flickit.assessment.core.adapter.out.persistence.assessmentresult.AssessmentResultJpaEntity;
import org.flickit.assessment.core.application.domain.ConfidenceLevel;

import java.util.UUID;

public class AnswerJpaEntityMother {

    public static AnswerJpaEntity answerEntityWithOption(AssessmentResultJpaEntity assessmentResultJpaEntity, Long questionId, Long answerOptionId) {
        return new AnswerJpaEntity(
            UUID.randomUUID(),
            assessmentResultJpaEntity,
            1L,
            questionId,
            answerOptionId,
            ConfidenceLevel.getDefault().getId(),
            null
        );
    }

    public static AnswerJpaEntity answerEntityWithNoOption(AssessmentResultJpaEntity assessmentResultJpaEntity, Long questionId) {
        return new AnswerJpaEntity(
            UUID.randomUUID(),
            assessmentResultJpaEntity,
            1L,
            questionId,
            null,
            ConfidenceLevel.getDefault().getId(),
            null
        );
    }

    public static AnswerJpaEntity answerEntityWithIsNotApplicableTrue(AssessmentResultJpaEntity assessmentResultJpaEntity, Long questionId) {
        return new AnswerJpaEntity(
            UUID.randomUUID(),
            assessmentResultJpaEntity,
            1L,
            questionId,
            null,
            ConfidenceLevel.getDefault().getId(),
            Boolean.TRUE
        );
    }
}
