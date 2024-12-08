package org.flickit.assessment.core.application.service.attribute;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.application.domain.assessment.AssessmentAccessChecker;
import org.flickit.assessment.common.application.domain.crud.Order;
import org.flickit.assessment.common.application.domain.crud.PaginatedResponse;
import org.flickit.assessment.common.application.domain.crud.Sort;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.core.application.port.in.attribute.GetAttributeScoreDetailUseCase;
import org.flickit.assessment.core.application.port.out.attribute.LoadAttributeScoreDetailPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.flickit.assessment.common.application.domain.assessment.AssessmentPermission.VIEW_ATTRIBUTE_SCORE_DETAIL;
import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetAttributeScoreDetailService implements GetAttributeScoreDetailUseCase {

    private final LoadAttributeScoreDetailPort loadAttributeScoreDetailPort;
    private final AssessmentAccessChecker assessmentAccessChecker;

    @Override
    public PaginatedResponse<QuestionScore> getAttributeScoreDetail(Param param) {
        checkUserAccess(param.getAssessmentId(), param.getCurrentUserId());

        var result = loadAttributeScoreDetailPort.loadScoreDetail(
            toParam(
                param.getAssessmentId(),
                param.getAttributeId(),
                param.getMaturityLevelId(),
                param.getSort(),
                param.getOrder(),
                param.getSize(),
                param.getPage()
            )
        );

        var items = result.getItems().stream().map(this::toQuestionScore).toList();
        return new PaginatedResponse<>(items, result.getPage(), result.getSize(), result.getSort(), result.getOrder(), result.getTotal());
    }

    private LoadAttributeScoreDetailPort.Param toParam(UUID assessmentId, Long attributeId, Long maturityLevelId, String sort, String order, int size, int page) {
        return new LoadAttributeScoreDetailPort.Param(
            assessmentId,
            attributeId,
            maturityLevelId,
            Sort.valueOf(sort),
            Order.valueOf(order),
            size,
            page);
    }

    private QuestionScore toQuestionScore(LoadAttributeScoreDetailPort.Result item) {
        return new QuestionScore(item.questionnaireTitle(), item.index(), item.questionTitle(),
            item.questionWeight(),
            item.index(),
            item.answer(),
            item.answerIsNotApplicable(),
            item.answerScore(),
            item.weightedScore(),
            item.confidence());
    }

    private void checkUserAccess(UUID assessmentId, UUID currentUserId) {
        if (!assessmentAccessChecker.isAuthorized(assessmentId, currentUserId, VIEW_ATTRIBUTE_SCORE_DETAIL))
            throw new AccessDeniedException(COMMON_CURRENT_USER_NOT_ALLOWED);
    }
}
