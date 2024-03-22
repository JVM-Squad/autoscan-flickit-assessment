package org.flickit.assessment.users.application.service.expertgroupaccess;

import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.users.application.port.in.expertgroupaccess.InviteExpertGroupMemberUseCase;
import org.flickit.assessment.users.application.port.out.expertgroup.CheckExpertGroupOwnerPort;
import org.flickit.assessment.users.application.port.out.expertgroupaccess.InviteExpertGroupMemberPort;
import org.flickit.assessment.users.application.port.out.user.LoadUserEmailByUserIdPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteExpertGroupMemberServiceTest {

    @InjectMocks
    private InviteExpertGroupMemberService service;
    @Mock
    private LoadUserEmailByUserIdPort loadUserEmailByUserIdPort;
    @Mock
    private CheckExpertGroupOwnerPort checkExpertGroupOwnerPort;
    @Mock
    private InviteExpertGroupMemberPort inviteExpertGroupMemberPort;

    @Test
    void inviteMember_validParameters_success() {
        UUID userId = UUID.randomUUID();
        long expertGroupId = 0L;
        UUID currentUserId = UUID.randomUUID();
        InviteExpertGroupMemberUseCase.Param param = new InviteExpertGroupMemberUseCase.Param(expertGroupId, userId, currentUserId);
        String email = "test@example.com";

        when(loadUserEmailByUserIdPort.loadEmail(userId)).thenReturn(email);
        when(checkExpertGroupOwnerPort.checkIsOwner(any(Long.class), any(UUID.class))).thenReturn(true);
        when(inviteExpertGroupMemberPort.invite(any(InviteExpertGroupMemberPort.Param.class))).thenReturn(expertGroupId);

        service.inviteMember(param);

        verify(loadUserEmailByUserIdPort).loadEmail(any(UUID.class));
        verify(checkExpertGroupOwnerPort).checkIsOwner(any(Long.class), any(UUID.class));
        verify(inviteExpertGroupMemberPort).invite(any());
    }

    @Test
    void inviteMember_expertGroupNotExist_fail() {
        UUID userId = UUID.randomUUID();
        long expertGroupId = 0L;
        UUID currentUserId = UUID.randomUUID();
        InviteExpertGroupMemberUseCase.Param param = new InviteExpertGroupMemberUseCase.Param(expertGroupId, userId, currentUserId);
        String email = "test@example.com";

        when(loadUserEmailByUserIdPort.loadEmail(userId)).thenReturn(email);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.inviteMember(param));
    }

    @Test
    void inviteMember_expertGroupInviterNotOwner_fail() {
        UUID userId = UUID.randomUUID();
        long expertGroupId = 0L;
        UUID currentUserId = UUID.randomUUID();
        InviteExpertGroupMemberUseCase.Param param = new InviteExpertGroupMemberUseCase.Param(expertGroupId, userId, currentUserId);
        String email = "test@example.com";

        when(loadUserEmailByUserIdPort.loadEmail(userId)).thenReturn(email);
        when(checkExpertGroupOwnerPort.checkIsOwner(any(Long.class), any(UUID.class))).thenReturn(false);

        Assertions.assertThrows(AccessDeniedException.class, () -> service.inviteMember(param));
    }
}
