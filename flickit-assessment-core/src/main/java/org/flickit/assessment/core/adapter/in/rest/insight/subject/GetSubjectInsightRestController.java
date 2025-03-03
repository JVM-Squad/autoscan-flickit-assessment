package org.flickit.assessment.core.adapter.in.rest.insight.subject;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.config.jwt.UserContext;
import org.flickit.assessment.core.application.port.in.insight.subject.GetSubjectInsightUseCase;
import org.flickit.assessment.core.application.port.in.insight.subject.GetSubjectInsightUseCase.Param;
import org.flickit.assessment.core.application.port.in.insight.subject.GetSubjectInsightUseCase.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GetSubjectInsightRestController {

    private final GetSubjectInsightUseCase useCase;
    private final UserContext userContext;

    @GetMapping("/assessments/{assessmentId}/subjects/{subjectId}/insight")
    public ResponseEntity<Result> getSubjectInsight(
        @PathVariable("assessmentId") UUID assessmentId,
        @PathVariable("subjectId") Long subjectId) {
        UUID currentUserId = userContext.getUser().id();
        var response = useCase.getSubjectInsight(toParam(assessmentId, subjectId, currentUserId));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Param toParam(UUID assessmentId, Long subjectId, UUID currentUserId) {
        return new Param(assessmentId, subjectId, currentUserId);
    }
}
