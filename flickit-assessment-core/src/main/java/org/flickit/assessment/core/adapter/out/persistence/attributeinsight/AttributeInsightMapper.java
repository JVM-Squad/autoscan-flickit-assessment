package org.flickit.assessment.core.adapter.out.persistence.attributeinsight;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.flickit.assessment.core.application.domain.AttributeInsight;
import org.flickit.assessment.core.application.domain.assessmentdashboard.DashboardInsights;
import org.flickit.assessment.data.jpa.core.attributeinsight.AttributeInsightJpaEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeInsightMapper {

    static AttributeInsight mapToDomain(AttributeInsightJpaEntity entity) {
        return new AttributeInsight(entity.getAssessmentResultId(),
            entity.getAttributeId(),
            entity.getAiInsight(),
            entity.getAssessorInsight(),
            entity.getAiInsightTime(),
            entity.getAssessorInsightTime(),
            entity.getAiInputPath());
    }

    public static AttributeInsightJpaEntity mapToJpaEntity(AttributeInsight attributeInsight) {
        return new AttributeInsightJpaEntity(
            attributeInsight.getAssessmentResultId(),
            attributeInsight.getAttributeId(),
            attributeInsight.getAiInsight(),
            attributeInsight.getAssessorInsight(),
            attributeInsight.getAiInsightTime(),
            attributeInsight.getAssessorInsightTime(),
            attributeInsight.getAiInputPath()
        );
    }

    public static DashboardInsights.InsightTime mapToAttributeInsightTime(AttributeInsightJpaEntity entity) {
        var insightTime = entity.getAssessorInsightTime() == null ? entity.getAiInsightTime() : entity.getAssessorInsightTime();
        return new DashboardInsights.InsightTime(insightTime);
    }
}
