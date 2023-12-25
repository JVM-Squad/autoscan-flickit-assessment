package org.flickit.assessment.core.application.port.in.assessment;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.flickit.assessment.common.application.SelfValidating;

import java.util.List;
import java.util.UUID;

public interface GetAttributeScoreDetailUseCase {

    Result getAttributeScoreDetail(Param param);

    @Value
    @EqualsAndHashCode(callSuper = false)
    class Param extends SelfValidating<Param> {

        @NotNull(message = "")
        UUID assessmentId;

        @NotNull(message = "")
        Long attributeId;

        @NotNull(message = "")
        Long maturityLevelId;

        @NotNull(message = COMMON_CURRENT_USER_ID_NOT_NULL)
        UUID currentUserId;

        public Param(UUID assessmentId, Long attributeId, Long maturityLevelId, UUID currentUserId) {
            this.assessmentId = assessmentId;
            this.attributeId = attributeId;
            this.maturityLevelId = maturityLevelId;
            this.currentUserId = currentUserId;
            this.validateSelf();
        }
    }

    record Result(double totalScore, double gainedScore, List<QuestionScore> questionScores) {
    }

    record QuestionScore(String questionnaireTitle,
                         int questionIndex,
                         String questionTitle,
                         int questionWeight,
                         int answerOptionIndex,
                         String answerOptionTitle,
                         Double answerScore,
                         double weightedScore) {
    }
}
