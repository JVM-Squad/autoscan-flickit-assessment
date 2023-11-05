package org.flickit.assessment.core.adapter.out.calculate;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.core.adapter.out.persistence.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.assessment.core.adapter.out.persistence.attributematurityscore.AttributeMaturityScorePersistenceJpaAdapter;
import org.flickit.assessment.core.adapter.out.persistence.qualityattributevalue.QualityAttributeValueJpaRepository;
import org.flickit.assessment.core.adapter.out.persistence.subjectvalue.SubjectValueJpaRepository;
import org.flickit.assessment.core.application.domain.AssessmentResult;
import org.flickit.assessment.core.application.domain.SubjectValue;
import org.flickit.assessment.core.application.port.out.assessmentresult.UpdateCalculatedResultPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssessmentCalculateResultPersistAdapter implements UpdateCalculatedResultPort {

    private final AssessmentResultJpaRepository assessmentResultRepo;
    private final SubjectValueJpaRepository subjectValueRepo;
    private final QualityAttributeValueJpaRepository attributeValueRepo;
    private final AttributeMaturityScorePersistenceJpaAdapter attributeMaturityScoreAdapter;

    @Override
    public void updateCalculatedResult(AssessmentResult assessmentResult) {
        assessmentResultRepo.updateAfterCalculate(assessmentResult.getId(),
            assessmentResult.getMaturityLevel().getId(),
            assessmentResult.isCalculateValid(),
            assessmentResult.getLastModificationTime());

        List<SubjectValue> subjectValues = assessmentResult.getSubjectValues();
        subjectValues.forEach(s -> subjectValueRepo.updateMaturityLevelById(s.getId(), s.getMaturityLevel().getId()));

        subjectValues.stream()
            .flatMap(x -> x.getQualityAttributeValues().stream())
            .forEach(qav -> {
                attributeValueRepo.updateMaturityLevelById(qav.getId(), qav.getMaturityLevel().getId());
                qav.getMaturityScores().forEach(maturityScore ->
                    attributeMaturityScoreAdapter.saveOrUpdate(qav.getId(), maturityScore)
                );
            });
    }
}
