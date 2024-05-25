package org.flickit.assessment.core.adapter.out.persistence.assessmentuserrole;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.core.application.domain.AssessmentUserRole;
import org.flickit.assessment.core.application.port.out.assessmentuserrole.DeleteUserAssessmentRolePort;
import org.flickit.assessment.core.application.port.out.assessmentuserrole.GrantUserAssessmentRolePort;
import org.flickit.assessment.core.application.port.out.assessmentuserrole.LoadUserRoleForAssessmentPort;
import org.flickit.assessment.data.jpa.core.assessmentuserrole.AssessmentUserRoleJpaEntity;
import org.flickit.assessment.data.jpa.core.assessmentuserrole.AssessmentUserRoleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.flickit.assessment.core.common.ErrorMessageKey.DELETE_ASSESSMENT_USER_ROLE_ROLE_NOT_FOUND;
import static org.flickit.assessment.core.common.ErrorMessageKey.GRANT_ASSESSMENT_USER_ROLE_ROLE_ID_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class AssessmentUserRolePersistenceJpaAdapter implements
    LoadUserRoleForAssessmentPort,
    GrantUserAssessmentRolePort,
    DeleteUserAssessmentRolePort {

    private final AssessmentUserRoleJpaRepository repository;

    @Override
    public AssessmentUserRole load(UUID assessmentId, UUID userId) {
        return repository.findByAssessmentIdAndUserId(assessmentId, userId)
            .map(x -> AssessmentUserRole.valueOfById(x.getRoleId()))
            .orElse(null);
    }

    @Override
    public void persist(UUID assessmentId, UUID userId, Integer roleId) {
        if (!AssessmentUserRole.isValidId(roleId))
            throw new ResourceNotFoundException(GRANT_ASSESSMENT_USER_ROLE_ROLE_ID_NOT_FOUND);

        var entity = new AssessmentUserRoleJpaEntity(assessmentId, userId, roleId);
        repository.save(entity);
    }

    @Override
    public void deleteUserAssessmentRole(UUID assessmentId, UUID userId) {
        if (!repository.existsByAssessmentIdAndUserId(assessmentId, userId))
            throw new ResourceNotFoundException(DELETE_ASSESSMENT_USER_ROLE_ROLE_NOT_FOUND);
        repository.deleteByAssessmentIdAndUserId(assessmentId, userId);
    }
}
