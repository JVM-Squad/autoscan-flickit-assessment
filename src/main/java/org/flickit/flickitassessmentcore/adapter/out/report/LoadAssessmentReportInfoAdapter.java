package org.flickit.flickitassessmentcore.adapter.out.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flickit.flickitassessmentcore.adapter.out.persistence.assessment.AssessmentJpaEntity;
import org.flickit.flickitassessmentcore.adapter.out.persistence.assessmentresult.AssessmentResultJpaEntity;
import org.flickit.flickitassessmentcore.adapter.out.persistence.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.flickitassessmentcore.adapter.out.persistence.subjectvalue.SubjectValueJpaEntity;
import org.flickit.flickitassessmentcore.adapter.out.persistence.subjectvalue.SubjectValueJpaRepository;
import org.flickit.flickitassessmentcore.adapter.out.rest.maturitylevel.MaturityLevelRestAdapter;
import org.flickit.flickitassessmentcore.application.domain.*;
import org.flickit.flickitassessmentcore.application.exception.CalculateNotValidException;
import org.flickit.flickitassessmentcore.application.port.out.assessmentresult.LoadAssessmentReportInfoPort;
import org.flickit.flickitassessmentcore.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static org.flickit.flickitassessmentcore.adapter.out.persistence.assessment.AssessmentMapper.mapToDomainModel;
import static org.flickit.flickitassessmentcore.common.ErrorMessageKey.*;

@Slf4j
@Component
@AllArgsConstructor
public class LoadAssessmentReportInfoAdapter implements LoadAssessmentReportInfoPort {

    private final AssessmentResultJpaRepository assessmentResultRepo;
    private final SubjectValueJpaRepository subjectValueRepo;

    private final MaturityLevelRestAdapter maturityLevelRestAdapter;

    @Override
    public AssessmentResult load(UUID assessmentId) {
        AssessmentResultJpaEntity assessmentResultEntity = assessmentResultRepo.findFirstByAssessment_IdOrderByLastModificationTimeDesc(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException(REPORT_ASSESSMENT_ASSESSMENT_RESULT_NOT_FOUND));

        if (!assessmentResultEntity.getIsValid())
            throw new CalculateNotValidException(REPORT_ASSESSMENT_ASSESSMENT_RESULT_NOT_VALID);

        UUID assessmentResultId = assessmentResultEntity.getId();
        List<SubjectValueJpaEntity> subjectValueEntities = subjectValueRepo.findByAssessmentResultId(assessmentResultId);

        Map<Long, MaturityLevel> maturityLevels = maturityLevelRestAdapter.loadByKitId(assessmentResultEntity.getAssessment().getAssessmentKitId())
            .stream()
            .collect(toMap(MaturityLevel::getId, x -> x));
        List<SubjectValue> subjectValues = buildSubjectValues(subjectValueEntities, maturityLevels);

        return new AssessmentResult(
            assessmentResultId,
            buildAssessment(assessmentResultEntity.getAssessment(), maturityLevels),
            subjectValues,
            findMaturityLevelById(maturityLevels, assessmentResultEntity.getMaturityLevelId()),
            assessmentResultEntity.getIsValid(),
            assessmentResultEntity.getLastModificationTime());
    }

    private List<SubjectValue> buildSubjectValues(List<SubjectValueJpaEntity> subjectValueEntities, Map<Long, MaturityLevel> maturityLevels) {
        return subjectValueEntities.stream()
            .map(x ->
                new SubjectValue(
                    x.getId(),
                    new Subject(x.getSubjectId()),
                    null,
                    findMaturityLevelById(maturityLevels, x.getMaturityLevelId()))
            ).toList();
    }

    private Assessment buildAssessment(AssessmentJpaEntity assessmentEntity, Map<Long, MaturityLevel> maturityLevels) {
        AssessmentKit kit = new AssessmentKit(assessmentEntity.getAssessmentKitId(), new ArrayList<>(maturityLevels.values()));
        return mapToDomainModel(assessmentEntity, kit);
    }

    private MaturityLevel findMaturityLevelById(Map<Long, MaturityLevel> maturityLevels, long id) {
        if (!maturityLevels.containsKey(id)) {
            log.error("No maturityLevel found with id={}", id);
            throw new ResourceNotFoundException(REPORT_ASSESSMENT_MATURITY_LEVEL_NOT_FOUND);
        }
        return maturityLevels.get(id);
    }
}
