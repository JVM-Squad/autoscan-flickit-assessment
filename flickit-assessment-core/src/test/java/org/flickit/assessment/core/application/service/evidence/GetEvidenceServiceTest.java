package org.flickit.assessment.core.application.service.evidence;

import org.flickit.assessment.common.exception.AccessDeniedException;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.core.application.port.out.answeroption.LoadAnswerOptionsByQuestionPort;
import org.flickit.assessment.core.application.port.in.evidence.GetEvidenceUseCase;
import org.flickit.assessment.core.application.port.in.evidence.GetEvidenceUseCase.Param;
import org.flickit.assessment.core.application.port.out.answer.LoadAnswerPort;
import org.flickit.assessment.core.application.port.out.assessment.CheckAssessmentSpaceMembershipPort;
import org.flickit.assessment.core.application.port.out.assessmentresult.LoadAssessmentResultPort;
import org.flickit.assessment.core.application.port.out.evidence.LoadEvidencePort;
import org.flickit.assessment.core.application.port.out.question.LoadQuestionPort;
import org.flickit.assessment.core.application.port.out.questionnaire.LoadQuestionnairePort;
import org.flickit.assessment.core.application.port.out.user.LoadUserPort;
import org.flickit.assessment.core.test.fixture.application.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.flickit.assessment.core.common.ErrorMessageKey.GET_EVIDENCE_ID_NOT_NULL;
import static org.flickit.assessment.common.error.ErrorMessageKey.COMMON_CURRENT_USER_NOT_ALLOWED;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetEvidenceServiceTest {

    @InjectMocks
    private GetEvidenceService service;

    @Mock
    private LoadEvidencePort loadEvidencePort;

    @Mock
    private CheckAssessmentSpaceMembershipPort checkAssessmentSpaceMembershipPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadAssessmentResultPort loadAssessmentResultPort;

    @Mock
    private LoadAnswerOptionsByQuestionPort loadAnswerOptionsByQuestionPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuestionnairePort loadQuestionnairePort;

    @Mock
    private LoadAnswerPort loadAnswerPort;

    @Test
    @DisplayName("For loading an evidence, the evidence should be exist or throw notFoundException.")
    void testLoadEvidence_evidenceNotExist_NotFoundException() {
        var id = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();
        var param = new Param(id, currentUserId);

        when(loadEvidencePort.loadNotDeletedEvidence(id)).thenThrow(new ResourceNotFoundException(GET_EVIDENCE_ID_NOT_NULL));

        var throwable = assertThrows(ResourceNotFoundException.class, () -> service.getEvidence(param));

        assertEquals(GET_EVIDENCE_ID_NOT_NULL, throwable.getMessage());

        verify(loadEvidencePort).loadNotDeletedEvidence(id);
    }

    @Test
    @DisplayName("If currentUser doesn't have access to the corresponding evidence's assessment,.")
    void testLoadEvidence_VUserDoesNotHaveAccess_Successful() {
        var id = UUID.randomUUID();
        var evidence = EvidenceMother.simpleEvidence();
        var currentUserId = UUID.randomUUID();
        var param = new Param(id, currentUserId);

        when(loadEvidencePort.loadNotDeletedEvidence(id)).thenReturn(evidence);
        when(checkAssessmentSpaceMembershipPort.isAssessmentSpaceMember(evidence.getAssessmentId(), currentUserId)).thenReturn(false);

        var throwable = assertThrows(AccessDeniedException.class, () -> service.getEvidence(param));

        assertEquals(COMMON_CURRENT_USER_NOT_ALLOWED, throwable.getMessage());
        verify(loadEvidencePort).loadNotDeletedEvidence(id);
        verify(checkAssessmentSpaceMembershipPort).isAssessmentSpaceMember(evidence.getAssessmentId(), currentUserId);
    }

    @Test
    @DisplayName("LoadEvidenceService with a valid evidenceId should return a valid response.")
    void testLoadEvidence_ValidEvidenceId_Successful() {
        var id = UUID.randomUUID();
        var evidence = EvidenceMother.simpleEvidence();
        var currentUserId = UUID.randomUUID();
        var param = new Param(id, currentUserId);
        var assessmentResult = AssessmentResultMother.resultWithValidations(true, true, LocalDateTime.now(), LocalDateTime.now());
        LoadQuestionPort.Result question = new LoadQuestionPort.Result(1L, "title", 1, 2L);

        when(loadEvidencePort.loadNotDeletedEvidence(id)).thenReturn(evidence);
        when(checkAssessmentSpaceMembershipPort.isAssessmentSpaceMember(evidence.getAssessmentId(), currentUserId)).thenReturn(true);
        when(loadUserPort.loadById(evidence.getCreatedById())).thenReturn(Optional.of(UserMother.createUser()));
        when(loadAssessmentResultPort.loadByAssessmentId(evidence.getAssessmentId())).thenReturn(Optional.of(assessmentResult));
        when(loadQuestionPort.loadByIdAndKitVersionId(evidence.getQuestionId(), assessmentResult.getKitVersionId())).thenReturn(Optional.of(question));
        when(loadQuestionnairePort.loadByIdAndKitVersionId(question.questionnaireId(), assessmentResult.getKitVersionId())).thenReturn(Optional.of(new LoadQuestionnairePort.Result(2L, "title")));

        var result = assertDoesNotThrow(() -> service.getEvidence(param));

        assertEquals(GetEvidenceUseCase.Result.class, result.getClass());
        assertEquals(GetEvidenceUseCase.Result.ResultEvidence.class, result.evidence().getClass());
        assertEquals(GetEvidenceUseCase.Result.ResultQuestion.class, result.question().getClass());

        verify(loadEvidencePort).loadNotDeletedEvidence(id);
        verify(checkAssessmentSpaceMembershipPort).isAssessmentSpaceMember(evidence.getAssessmentId(), currentUserId);
        verify(loadUserPort).loadById(evidence.getCreatedById());
        verify(loadAssessmentResultPort).loadByAssessmentId(evidence.getAssessmentId());
        verify(loadQuestionPort).loadByIdAndKitVersionId(evidence.getQuestionId(), assessmentResult.getKitVersionId());
        verify(loadQuestionnairePort).loadByIdAndKitVersionId(question.questionnaireId(), assessmentResult.getKitVersionId());
        verify(loadAnswerOptionsByQuestionPort).loadByQuestionId(question.id(), assessmentResult.getKitVersionId());
        verify(loadAnswerPort).load(assessmentResult.getId(), question.id());
    }
}
