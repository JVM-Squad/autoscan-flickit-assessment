package org.flickit.assessment.core.adapter.out.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.core.adapter.out.minio.MinioAdapter;
import org.flickit.assessment.core.application.domain.MaturityLevel;
import org.flickit.assessment.core.application.domain.report.AssessmentReportItem;
import org.flickit.assessment.core.application.domain.report.AssessmentReportItem.Space;
import org.flickit.assessment.core.application.domain.report.AssessmentSubjectReportItem;
import org.flickit.assessment.core.application.domain.report.AttributeReportItem;
import org.flickit.assessment.core.application.domain.report.QuestionnaireReportItem;
import org.flickit.assessment.core.application.port.out.assessmentresult.LoadAssessmentReportInfoPort;
import org.flickit.assessment.data.jpa.core.assessment.AssessmentJpaRepository;
import org.flickit.assessment.data.jpa.core.assessmentinsight.AssessmentInsightJpaRepository;
import org.flickit.assessment.data.jpa.core.assessmentresult.AssessmentResultJpaEntity;
import org.flickit.assessment.data.jpa.core.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.assessment.data.jpa.core.attributevalue.AttributeValueJpaRepository;
import org.flickit.assessment.data.jpa.core.attributevalue.SubjectIdAttributeValueView;
import org.flickit.assessment.data.jpa.core.subjectvalue.SubjectValueJpaEntity;
import org.flickit.assessment.data.jpa.core.subjectvalue.SubjectValueJpaRepository;
import org.flickit.assessment.data.jpa.kit.assessmentkit.AssessmentKitJpaEntity;
import org.flickit.assessment.data.jpa.kit.assessmentkit.AssessmentKitJpaRepository;
import org.flickit.assessment.data.jpa.kit.maturitylevel.MaturityLevelJpaRepository;
import org.flickit.assessment.data.jpa.kit.questionnaire.QuestionnaireJpaEntity;
import org.flickit.assessment.data.jpa.kit.questionnaire.QuestionnaireJpaRepository;
import org.flickit.assessment.data.jpa.kit.questionnaire.QuestionnaireListItemView;
import org.flickit.assessment.data.jpa.kit.subject.SubjectJpaEntity;
import org.flickit.assessment.data.jpa.kit.subject.SubjectJpaRepository;
import org.flickit.assessment.data.jpa.users.expertgroup.ExpertGroupJpaEntity;
import org.flickit.assessment.data.jpa.users.expertgroup.ExpertGroupJpaRepository;
import org.flickit.assessment.data.jpa.users.space.SpaceJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.flickit.assessment.core.adapter.out.persistence.kit.maturitylevel.MaturityLevelMapper.mapToDomainModel;
import static org.flickit.assessment.core.common.ErrorMessageKey.*;

@Slf4j
@Component
@AllArgsConstructor
public class LoadAssessmentReportInfoAdapter implements LoadAssessmentReportInfoPort {

    private static final Duration EXPIRY_DURATION = Duration.ofDays(1);

    private final AssessmentJpaRepository assessmentRepository;
    private final AssessmentResultJpaRepository assessmentResultRepo;
    private final SubjectValueJpaRepository subjectValueRepo;
    private final AssessmentKitJpaRepository assessmentKitRepository;
    private final ExpertGroupJpaRepository expertGroupRepository;
    private final MaturityLevelJpaRepository maturityLevelRepository;
    private final SubjectJpaRepository subjectRepository;
    private final AttributeValueJpaRepository attributeValueJpaRepository;
    private final MinioAdapter minioAdapter;
    private final SpaceJpaRepository spaceRepository;
    private final AssessmentInsightJpaRepository assessmentInsightRepository;
    private final QuestionnaireJpaRepository questionnaireRepository;

    @Override
    public Result load(UUID assessmentId, UUID currentUserId) {
        if (!assessmentRepository.existsByIdAndDeletedFalse(assessmentId))
            throw new ResourceNotFoundException(REPORT_ASSESSMENT_ASSESSMENT_ID_NOT_FOUND);

        var assessmentResultEntity = assessmentResultRepo.findFirstByAssessment_IdOrderByLastModificationTimeDesc(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_ASSESSMENT_RESULT_NOT_FOUND));

        var assessment = assessmentResultEntity.getAssessment();
        var assessmentKitEntity = assessmentKitRepository.findById(assessment.getAssessmentKitId())
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_ASSESSMENT_KIT_NOT_FOUND));

        var insightEntity = assessmentInsightRepository.findByAssessmentResultId(assessmentResultEntity.getId())
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_ASSESSMENT_KIT_NOT_FOUND));

        var questionnaireViews = questionnaireRepository.findAllWithQuestionCountByKitVersionId(assessmentResultEntity.getKitVersionId(), null).getContent();

        var expertGroupEntity = expertGroupRepository.findById(assessmentKitEntity.getExpertGroupId())
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_EXPERT_GROUP_NOT_FOUND));

        var kitVersionId = assessmentResultEntity.getKitVersionId();
        var maturityLevels = maturityLevelRepository.findAllByKitVersionIdOrderByIndex(kitVersionId)
            .stream().map(e -> mapToDomainModel(e, null)).toList();

        var idToMaturityLevel = maturityLevels.stream()
            .collect(toMap(MaturityLevel::getId, Function.identity()));

        var spaceEntity = spaceRepository.findById(assessment.getSpaceId())
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_SPACE_NOT_FOUND));

        var assessmentReportItem = new AssessmentReportItem(assessmentId,
            assessment.getTitle(),
            assessment.getShortTitle(),
            insightEntity.getInsight(),
            buildAssessmentKitItem(expertGroupEntity, assessmentKitEntity, maturityLevels, questionnaireViews),
            idToMaturityLevel.get(assessmentResultEntity.getMaturityLevelId()),
            assessmentResultEntity.getConfidenceValue(),
            assessmentResultEntity.getIsCalculateValid(),
            assessmentResultEntity.getIsConfidenceValid(),
            assessment.getCreationTime(),
            assessment.getLastModificationTime(),
            new Space(spaceEntity.getId(), spaceEntity.getTitle()));

        var subjects = buildSubjectReportItems(assessmentResultEntity, idToMaturityLevel);

        return new Result(assessmentReportItem, subjects);
    }

    private AssessmentReportItem.AssessmentKitItem buildAssessmentKitItem(ExpertGroupJpaEntity expertGroupEntity,
                                                                          AssessmentKitJpaEntity assessmentKitEntity,
                                                                          List<MaturityLevel> maturityLevels,
                                                                          List<QuestionnaireListItemView> questionnaireItemViews) {
        AssessmentReportItem.AssessmentKitItem.ExpertGroup expertGroup =
            new AssessmentReportItem.AssessmentKitItem.ExpertGroup(expertGroupEntity.getId(),
                expertGroupEntity.getTitle(),
                minioAdapter.createDownloadLink(expertGroupEntity.getPicture(), EXPIRY_DURATION));

        var questionnaireReportItems = questionnaireItemViews.stream().map(this::buildQuestionnaireReportItems).toList();
        int questionsCount = questionnaireReportItems.stream()
            .mapToInt(QuestionnaireReportItem::questionCount).reduce(0, Integer::sum);

        return new AssessmentReportItem.AssessmentKitItem(assessmentKitEntity.getId(),
            assessmentKitEntity.getTitle(),
            assessmentKitEntity.getSummary(),
            assessmentKitEntity.getAbout(),
            maturityLevels.size(),
            questionsCount,
            maturityLevels,
            questionnaireReportItems,
            expertGroup);
    }

    private QuestionnaireReportItem buildQuestionnaireReportItems(QuestionnaireListItemView itemView) {
        QuestionnaireJpaEntity questionnaire = itemView.getQuestionnaire();
        return new QuestionnaireReportItem(questionnaire.getId(),
            questionnaire.getTitle(),
            questionnaire.getDescription(),
            questionnaire.getIndex(),
            itemView.getQuestionCount());
    }

    private List<AssessmentSubjectReportItem> buildSubjectReportItems(AssessmentResultJpaEntity assessmentResult,
                                                                      Map<Long, MaturityLevel> idToMaturityLevel) {
        List<SubjectValueJpaEntity> subjectValueEntities = subjectValueRepo.findByAssessmentResultId(assessmentResult.getId());

        Set<Long> subjectIds = subjectValueEntities.stream()
            .map(SubjectValueJpaEntity::getSubjectId)
            .collect(Collectors.toSet());

        var subjectIdToSubjectValue = subjectValueEntities.stream()
            .collect(Collectors.toMap(SubjectValueJpaEntity::getSubjectId, Function.identity()));

        var subjectIdToAttributeValueMap = attributeValueJpaRepository.findByAssessmentResultIdAndSubjectIdIn(
                assessmentResult.getId(), subjectIds)
            .stream()
            .collect(groupingBy(SubjectIdAttributeValueView::getSubjectId));

        List<SubjectJpaEntity> subjectEntities = subjectRepository.findAllByIdInAndKitVersionId(subjectIds, assessmentResult.getKitVersionId());

        return subjectEntities.stream()
            .map(e -> {
                Long maturityLevelId = subjectIdToSubjectValue.get(e.getId()).getMaturityLevelId();
                MaturityLevel subjectMaturityLevel = idToMaturityLevel.get(maturityLevelId);
                var attributeValues = subjectIdToAttributeValueMap.get(e.getId());
                return new AssessmentSubjectReportItem(e.getId(),
                    e.getTitle(),
                    e.getIndex(),
                    e.getDescription(),
                    subjectIdToSubjectValue.get(e.getId()).getConfidenceValue(),
                    subjectMaturityLevel,
                    attributeValues == null ? List.of() : attributeValues.stream()
                        .map(x -> buildAttributeReportItem(idToMaturityLevel, x))
                        .toList());
            }).toList();
    }

    private AttributeReportItem buildAttributeReportItem(Map<Long, MaturityLevel> idToMaturityLevel,
                                                         SubjectIdAttributeValueView attributeValueView) {
        var attribute = attributeValueView.getAttribute();
        var attributeValue = attributeValueView.getAttributeValue();
        var maturityLevel = idToMaturityLevel.get(attributeValue.getMaturityLevelId());
        return new AttributeReportItem(attribute.getId(),
            attribute.getTitle(),
            attribute.getDescription(),
            attribute.getIndex(),
            attributeValue.getConfidenceValue(),
            maturityLevel);
    }
}
