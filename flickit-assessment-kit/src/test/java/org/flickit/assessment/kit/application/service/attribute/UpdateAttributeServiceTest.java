package org.flickit.assessment.kit.application.service.attribute;

import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.util.SlugCodeUtil;
import org.flickit.assessment.kit.application.port.in.attribute.UpdateAttributeUseCase.Param;
import org.flickit.assessment.kit.application.port.out.attribute.UpdateAttributePort;
import org.flickit.assessment.kit.application.port.out.expertgroup.LoadExpertGroupOwnerPort;
import org.flickit.assessment.kit.application.port.out.kitversion.LoadKitVersionPort;
import org.flickit.assessment.kit.test.fixture.application.AssessmentKitMother;
import org.flickit.assessment.kit.test.fixture.application.KitVersionMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateAttributeServiceTest {

    @InjectMocks
    private UpdateAttributeService service;

    @Mock
    private LoadKitVersionPort loadKitVersionPort;

    @Mock
    private LoadExpertGroupOwnerPort loadExpertGroupOwnerPort;

    @Mock
    private UpdateAttributePort updateAttributePort;

    @Test
    void testUpdateAttribute_CurrentUserIsNotOwnerOfKitExpertGroup_ThrowsException() {
        Param param = createParam(Param.ParamBuilder::build);
        var expertGroupOwnerId = UUID.randomUUID();

        var kitVersion = KitVersionMother.createKitVersion(AssessmentKitMother.simpleKit());
        when(loadKitVersionPort.load(param.getKitVersionId())).thenReturn(kitVersion);
        when(loadExpertGroupOwnerPort.loadOwnerId(kitVersion.getKit().getExpertGroupId())).thenReturn(expertGroupOwnerId);

        var exception = assertThrows(AccessDeniedException.class, () -> service.updateAttribute(param));
        assertEquals(COMMON_CURRENT_USER_NOT_ALLOWED, exception.getMessage());

        verifyNoInteractions(updateAttributePort);
    }

    @Test
    void testUpdateAttribute_ValidParam_UpdateAttributeAndKitVersion() {
        Param param = createParam(Param.ParamBuilder::build);

        var kitVersion = KitVersionMother.createKitVersion(AssessmentKitMother.simpleKit());
        when(loadKitVersionPort.load(param.getKitVersionId())).thenReturn(kitVersion);
        when(loadExpertGroupOwnerPort.loadOwnerId(kitVersion.getKit().getExpertGroupId())).thenReturn(param.getCurrentUserId());
        doNothing().when(updateAttributePort).update(any());
        service.updateAttribute(param);

        var attributeUpdateParam = ArgumentCaptor.forClass(UpdateAttributePort.Param.class);
        verify(updateAttributePort, times(1)).update(attributeUpdateParam.capture());

        assertEquals(param.getKitVersionId(), attributeUpdateParam.getValue().kitVersionId());
        assertEquals(param.getAttributeId(), attributeUpdateParam.getValue().id());
        assertEquals(SlugCodeUtil.generateSlugCode(param.getTitle()), attributeUpdateParam.getValue().code());
        assertEquals(param.getTitle(), attributeUpdateParam.getValue().title());
        assertEquals(param.getDescription(), attributeUpdateParam.getValue().description());
        assertEquals(param.getSubjectId(), attributeUpdateParam.getValue().subjectId());
        assertEquals(param.getIndex(), attributeUpdateParam.getValue().index());
        assertEquals(param.getWeight(), attributeUpdateParam.getValue().weight());
        assertEquals(param.getCurrentUserId(), attributeUpdateParam.getValue().lastModifiedBy());
        assertNotNull(attributeUpdateParam.getValue().lastModificationTime());
    }

    private Param createParam(Consumer<Param.ParamBuilder> changer) {
        var paramBuilder = paramBuilder();
        changer.accept(paramBuilder);
        return paramBuilder.build();
    }

    private Param.ParamBuilder paramBuilder() {
        return Param.builder()
            .kitVersionId(16L)
            .attributeId(25L)
            .title("title")
            .description("description")
            .subjectId(18L)
            .index(2)
            .weight(1)
            .currentUserId(UUID.randomUUID());
    }
}