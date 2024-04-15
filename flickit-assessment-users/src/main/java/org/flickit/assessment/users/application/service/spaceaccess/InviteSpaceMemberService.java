package org.flickit.assessment.users.application.service.spaceaccess;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ValidationException;
import org.flickit.assessment.users.application.port.in.spaceaccess.InviteSpaceMemberUseCase;
import org.flickit.assessment.users.application.port.out.spaceaccess.CheckMemberSpaceAccessPort;
import org.flickit.assessment.users.application.port.out.spaceaccess.CheckSpaceExistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.flickit.assessment.users.common.ErrorMessageKey.INVITE_SPACE_MEMBER_INVITER_ACCESS_NOT_FOUND;
import static org.flickit.assessment.users.common.ErrorMessageKey.INVITE_SPACE_MEMBER_SPACE_ID_NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class InviteSpaceMemberService implements InviteSpaceMemberUseCase {

    private final CheckSpaceExistencePort checkSpaceExistencePort;
    private final CheckMemberSpaceAccessPort checkMemberSpaceAccessPort;

    @Override
    public void inviteMember(Param param) {
        var currentUserId = param.getCurrentUserId();
        if (!checkSpaceExistencePort.existsById(param.getSpaceId()))
            throw new ValidationException(INVITE_SPACE_MEMBER_SPACE_ID_NOT_FOUND);

        if (!checkMemberSpaceAccessPort.checkAccess(currentUserId))
            throw new AccessDeniedException(INVITE_SPACE_MEMBER_INVITER_ACCESS_NOT_FOUND);

    }
}
