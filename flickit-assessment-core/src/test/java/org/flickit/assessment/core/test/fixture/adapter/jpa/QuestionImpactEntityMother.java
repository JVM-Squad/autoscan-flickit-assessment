package org.flickit.assessment.core.test.fixture.adapter.jpa;

import org.flickit.assessment.data.jpa.kit.questionimpact.QuestionImpactJpaEntity;

public class QuestionImpactEntityMother {

    private static long questionImpactId = 134L;

    public static QuestionImpactJpaEntity questionImpactEntity(Long maturityLevelId, Long questionId, Long qualityAttributeId) {
        return new QuestionImpactJpaEntity(
            questionImpactId++,
            1,
            questionId,
            qualityAttributeId,
            MaturityLevelJpaEntityMother.maturityLevelEntity(maturityLevelId)
        );
    }
}
