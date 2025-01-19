package org.flickit.assessment.core.application.service.assessmentreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.flickit.assessment.common.application.domain.assessment.AssessmentAccessChecker;
import org.flickit.assessment.common.application.domain.assessment.AssessmentPermission;
import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.core.application.domain.AssessmentReportMetadata;
import org.flickit.assessment.core.application.port.in.assessmentreport.GetAssessmentReportMetaDataUseCase;
import org.flickit.assessment.core.application.port.out.assessmentreport.LoadAssessmentReportMetaDataPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAssessmentReportMetaDataServiceTest {

    @InjectMocks
    private GetAssessmentReportMetaDataService service;

    @Mock
    private AssessmentAccessChecker assessmentAccessChecker;

    @Mock
    private LoadAssessmentReportMetaDataPort loadAssessmentReportMetaDataPort;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void testAssessmentReportMetaData_UserDoesNotHaveEnoughAccess_AccessDeniedException() {
        var param = createParam(GetAssessmentReportMetaDataUseCase.Param.ParamBuilder::build);

        when(assessmentAccessChecker.isAuthorized(param.getAssessmentId(), param.getCurrentUserId(), AssessmentPermission.MANAGE_REPORT_METADATA))
            .thenReturn(false);

        var throwable = assertThrows(AccessDeniedException.class, () -> service.getAssessmentReportMetaData(param));
        assertEquals(COMMON_CURRENT_USER_NOT_ALLOWED, throwable.getMessage());
    }

    @Test
    void testAssessmentReportMetaData_AssessmentReportDoesNotExists_SuccessfulEmptyAssessmentReport() {
        var param = createParam(GetAssessmentReportMetaDataUseCase.Param.ParamBuilder::build);

        when(assessmentAccessChecker.isAuthorized(param.getAssessmentId(), param.getCurrentUserId(), AssessmentPermission.MANAGE_REPORT_METADATA))
            .thenReturn(true);
        when(loadAssessmentReportMetaDataPort.loadMetadata(param.getAssessmentId())).thenReturn(null);

        var result = service.getAssessmentReportMetaData(param);
        assertNull(result.intro());
        assertNull(result.prosAndCons());
        assertNull(result.steps());
        assertNull(result.participants());

        verifyNoInteractions(objectMapper);
    }


    @SneakyThrows
    @Test
    void testAssessmentReportMetaData_AssessmentReportExists_SuccessfulEmptyAssessmentReport() {
        var param = createParam(GetAssessmentReportMetaDataUseCase.Param.ParamBuilder::build);
        String portResult = "{\"intro\": \"introduction of assessment report\", " +
            "\"prosAndCons\": \"pros and cons of assessment\", " +
            "\"steps\": \"description of steps taken to perform the assessment\", " +
            "\"participants\": \"list of assessment participants and their participation's\"}";
        var expectedMetaData = new ObjectMapper().readValue(portResult, AssessmentReportMetadata.class);

        when(assessmentAccessChecker.isAuthorized(param.getAssessmentId(), param.getCurrentUserId(), AssessmentPermission.MANAGE_REPORT_METADATA))
            .thenReturn(true);
        when(loadAssessmentReportMetaDataPort.loadMetadata(param.getAssessmentId())).thenReturn(portResult);
        when(objectMapper.readValue(portResult, AssessmentReportMetadata.class)).thenReturn(expectedMetaData);

        var result = service.getAssessmentReportMetaData(param);
        assertEquals(expectedMetaData.intro(), result.intro());
        assertEquals(expectedMetaData.prosAndCons(), result.prosAndCons());
        assertEquals(expectedMetaData.steps(), result.steps());
        assertEquals(expectedMetaData.participants(), result.participants());
    }

    private GetAssessmentReportMetaDataUseCase.Param createParam(Consumer<GetAssessmentReportMetaDataUseCase.Param.ParamBuilder> changer) {
        var paramBuilder = paramBuilder();
        changer.accept(paramBuilder);
        return paramBuilder.build();
    }

    private GetAssessmentReportMetaDataUseCase.Param.ParamBuilder paramBuilder() {
        return GetAssessmentReportMetaDataUseCase.Param.builder()
            .assessmentId(UUID.randomUUID())
            .currentUserId(UUID.randomUUID());
    }
}
