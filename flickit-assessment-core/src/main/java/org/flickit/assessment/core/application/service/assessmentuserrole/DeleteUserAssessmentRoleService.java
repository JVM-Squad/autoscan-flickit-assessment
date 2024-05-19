package org.flickit.assessment.core.application.service.assessmentuserrole;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.common.permission.AssessmentPermissionChecker;
import org.flickit.assessment.core.application.port.in.assessmentuserrole.DeleteUserAssessmentRoleUseCase;
import org.flickit.assessment.core.application.port.out.assessment.CheckUserAssessmentAccessPort;
import org.flickit.assessment.core.application.port.out.assessmentuserrole.DeleteUserAssessmentRolePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;
import static org.flickit.assessment.common.permission.AssessmentPermission.DELETE_USER_ASSESSMENT_ROLE;
import static org.flickit.assessment.core.common.ErrorMessageKey.DELETE_ASSESSMENT_USER_ROLE_USER_ID_NOT_MEMBER;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteUserAssessmentRoleService implements DeleteUserAssessmentRoleUseCase {

    private final AssessmentPermissionChecker permissionChecker;
    private final CheckUserAssessmentAccessPort checkUserAssessmentAccessPort;
    private final DeleteUserAssessmentRolePort deleteUserAssessmentRolePort;

    @Override
    public void deleteAssessmentUserRole(Param param) {
        if (!permissionChecker.isAuthorized(param.getAssessmentId(), param.getCurrentUserId(), DELETE_USER_ASSESSMENT_ROLE))
            throw new AccessDeniedException(COMMON_CURRENT_USER_NOT_ALLOWED);

        if (!checkUserAssessmentAccessPort.hasAccess(param.getAssessmentId(), param.getUserId()))
            throw new ResourceNotFoundException(DELETE_ASSESSMENT_USER_ROLE_USER_ID_NOT_MEMBER);

        deleteUserAssessmentRolePort.deleteUserAssessmentRole(param.getAssessmentId(), param.getUserId());
    }
}
