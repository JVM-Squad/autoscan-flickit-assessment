package org.flickit.assessment.kit.application.service.assessmentkit;

import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ValidationException;
import org.flickit.assessment.kit.application.domain.AssessmentKit;
import org.flickit.assessment.kit.application.domain.KitVersionStatus;
import org.flickit.assessment.kit.application.port.in.assessmentkit.CloneKitUseCase;
import org.flickit.assessment.kit.application.port.out.assessmentkit.CloneKitPort;
import org.flickit.assessment.kit.application.port.out.assessmentkit.LoadAssessmentKitPort;
import org.flickit.assessment.kit.application.port.out.expertgroup.LoadExpertGroupOwnerPort;
import org.flickit.assessment.kit.application.port.out.kitversion.CreateKitVersionPort;
import org.flickit.assessment.kit.application.port.out.kitversion.ExistKitVersionByKitIdAndStatusPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;
import static org.flickit.assessment.kit.common.ErrorMessageKey.CLONE_KIT_NOT_ALLOWED;
import static org.flickit.assessment.kit.test.fixture.application.AssessmentKitMother.simpleKit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloneKitServiceTest {

    @InjectMocks
    private CloneKitService service;

    @Mock
    private LoadAssessmentKitPort loadAssessmentKitPort;

    @Mock
    private LoadExpertGroupOwnerPort loadExpertGroupOwnerPort;

    @Mock
    private CreateKitVersionPort createKitVersionPort;

    @Mock
    private ExistKitVersionByKitIdAndStatusPort existKitVersionByKitIdAndStatusPort;

    @Mock
    private CloneKitPort cloneKitPort;

    private final AssessmentKit kit = simpleKit();

    private final UUID ownerId = UUID.randomUUID();

    @Test
    void testCloneKitService_WhenCurrentUserIsNotExpertGroupOwner_ThenThrowAccessDeniedException() {
        CloneKitUseCase.Param param = createParam(CloneKitUseCase.Param.ParamBuilder::build);

        when(loadAssessmentKitPort.load(param.getKitId())).thenReturn(kit);
        when(loadExpertGroupOwnerPort.loadOwnerId(kit.getExpertGroupId())).thenReturn(ownerId);

        var throwable = assertThrows(AccessDeniedException.class, () -> service.cloneKitUseCase(param));
        assertEquals(COMMON_CURRENT_USER_NOT_ALLOWED, throwable.getMessage());
    }

    @Test
    void testCloneKitService_WhenCurrenUserIsExpertGroupOwnerAndKitHasUpdatingKitVersion_ThenThrowValidationException() {
        CloneKitUseCase.Param param = createParam(b -> b.currentUserId(ownerId));
        int updating = KitVersionStatus.UPDATING.getId();

        when(loadAssessmentKitPort.load(param.getKitId())).thenReturn(kit);
        when(loadExpertGroupOwnerPort.loadOwnerId(kit.getExpertGroupId())).thenReturn(ownerId);
        when(existKitVersionByKitIdAndStatusPort.exists(kit.getId(), updating)).thenReturn(true);

        var throwable = assertThrows(ValidationException.class, () -> service.cloneKitUseCase(param));
        assertEquals(CLONE_KIT_NOT_ALLOWED, throwable.getMessageKey());
    }

    @Test
    void testCloneKitService_WhenCurrentUserIsExpertGroupOwnerAndKitDoesntHaveUpdatingKitVersion_ThenCloneKit() {
        CloneKitUseCase.Param param = createParam(b -> b.currentUserId(ownerId));
        int updating = KitVersionStatus.UPDATING.getId();
        CreateKitVersionPort.Param outPortParam = new CreateKitVersionPort.Param(param.getKitId(),
            KitVersionStatus.UPDATING,
            param.getCurrentUserId());
        long updatingKitVersionId = 1L;

        when(loadAssessmentKitPort.load(param.getKitId())).thenReturn(kit);
        when(loadExpertGroupOwnerPort.loadOwnerId(kit.getExpertGroupId())).thenReturn(ownerId);
        when(existKitVersionByKitIdAndStatusPort.exists(kit.getId(), updating)).thenReturn(false);
        when(createKitVersionPort.persist(outPortParam)).thenReturn(updatingKitVersionId);
        doNothing().when(cloneKitPort).cloneKit(kit.getActiveVersionId(), updatingKitVersionId, param.getCurrentUserId());

        long result = service.cloneKitUseCase(param);
        assertEquals(updatingKitVersionId, result);
    }

    private CloneKitUseCase.Param createParam(Consumer<CloneKitUseCase.Param.ParamBuilder> changer) {
        var param = paramBuilder();
        changer.accept(param);
        return param.build();
    }

    private CloneKitUseCase.Param.ParamBuilder paramBuilder() {
        return CloneKitUseCase.Param.builder()
            .kitId(1L)
            .currentUserId(UUID.randomUUID());
    }
}
