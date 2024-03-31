package org.flickit.assessment.users.application.service.expertgroupaccess;

import lombok.AllArgsConstructor;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.users.application.port.in.expertgroupaccess.DeleteExpertGroupMemberUseCase;
import org.flickit.assessment.users.application.port.out.expertgroup.LoadExpertGroupOwnerPort;
import org.flickit.assessment.users.application.port.out.expertgroupaccess.DeleteExpertGroupMemberPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;
import static org.flickit.assessment.users.common.ErrorMessageKey.EXPERT_GROUP_ID_NOT_FOUND;

@Service
@Transactional
@AllArgsConstructor
public class DeleteExpertGroupMemberService implements DeleteExpertGroupMemberUseCase {

    private final LoadExpertGroupOwnerPort loadExpertGroupOwnerPort;
    private final DeleteExpertGroupMemberPort deleteExpertGroupMemberPort;

    @Override
    public void deleteMember(Param param) {
        validateCurrentUser(param.getExpertGroupId(), param.getCurrentUserId());
        deleteExpertGroupMemberPort.deleteMember(param.getUserId(), param.getExpertGroupId());
    }

    private void validateCurrentUser(Long expertGroupId, UUID currentUserId) {
        UUID expertGroupOwnerId = loadExpertGroupOwnerPort.loadOwnerId(expertGroupId);
        if (!Objects.equals(expertGroupOwnerId, currentUserId))
            throw new AccessDeniedException(COMMON_CURRENT_USER_NOT_ALLOWED);
    }
}
