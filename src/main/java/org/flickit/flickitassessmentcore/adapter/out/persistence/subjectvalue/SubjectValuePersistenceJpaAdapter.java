package org.flickit.flickitassessmentcore.adapter.out.persistence.subjectvalue;

import lombok.RequiredArgsConstructor;
import org.flickit.flickitassessmentcore.adapter.out.persistence.assessmentresult.AssessmentResultJpaEntity;
import org.flickit.flickitassessmentcore.adapter.out.persistence.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.flickitassessmentcore.application.port.out.subjectvalue.CreateSubjectValuePort;
import org.flickit.flickitassessmentcore.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.flickit.flickitassessmentcore.common.ErrorMessageKey.CREATE_SUBJECT_VALUE_ASSESSMENT_RESULT_ID_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class SubjectValuePersistenceJpaAdapter implements
    CreateSubjectValuePort {

    private final SubjectValueJpaRepository repository;
    private final AssessmentResultJpaRepository assessmentResultRepository;

    @Override
    public void persistAll(List<Long> subjectIds, UUID assessmentResultId) {
        AssessmentResultJpaEntity assessmentResult = assessmentResultRepository.findById(assessmentResultId)
            .orElseThrow(() -> new ResourceNotFoundException(CREATE_SUBJECT_VALUE_ASSESSMENT_RESULT_ID_NOT_FOUND));

        List<SubjectValueJpaEntity> entities = subjectIds.stream().map(subjectId -> {
            SubjectValueJpaEntity subjectValue = SubjectValueMapper.mapToJpaEntity(subjectId);
            subjectValue.setAssessmentResult(assessmentResult);
            return subjectValue;
        }).toList();

        repository.saveAll(entities);
    }

}
