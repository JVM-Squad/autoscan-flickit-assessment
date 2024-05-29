package org.flickit.assessment.core.application.service.assessment;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.application.domain.crud.PaginatedResponse;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.core.application.domain.AssessmentListItem;
import org.flickit.assessment.core.application.port.in.assessment.GetSpaceAssessmentListUseCase;
import org.flickit.assessment.core.application.port.out.assessment.LoadAssessmentListItemsBySpacePort;
import org.flickit.assessment.core.application.port.out.space.CheckSpaceAccessPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSpaceAssessmentListService implements GetSpaceAssessmentListUseCase {

    private final LoadAssessmentListItemsBySpacePort loadAssessmentsBySpace;
    private final CheckSpaceAccessPort checkSpaceAccessPort;

    @Override
    public PaginatedResponse<AssessmentListItem> getAssessmentList(Param param) {
        UUID currentUserId = param.getCurrentUserId();

        Long spaceId = param.getSpaceId();
        if (!checkSpaceAccessPort.checkIsMember(spaceId, currentUserId))
            throw new AccessDeniedException(COMMON_CURRENT_USER_NOT_ALLOWED);

        return loadAssessmentsBySpace.loadNotDeletedAssessments(
            spaceId,
            param.getPage(),
            param.getSize()
        );
    }
}
