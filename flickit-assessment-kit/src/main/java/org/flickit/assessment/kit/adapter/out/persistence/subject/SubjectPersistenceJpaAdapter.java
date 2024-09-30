package org.flickit.assessment.kit.adapter.out.persistence.subject;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.data.jpa.kit.attribute.AttributeJpaEntity;
import org.flickit.assessment.data.jpa.kit.attribute.AttributeJpaRepository;
import org.flickit.assessment.data.jpa.kit.subject.SubjectJpaEntity;
import org.flickit.assessment.data.jpa.kit.subject.SubjectJpaRepository;
import org.flickit.assessment.kit.adapter.out.persistence.attribute.AttributeMapper;
import org.flickit.assessment.kit.application.domain.Subject;
import org.flickit.assessment.kit.application.port.out.subject.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.flickit.assessment.data.jpa.kit.subject.SubjectJpaEntity.Fields.INDEX;
import static org.flickit.assessment.kit.adapter.out.persistence.subject.SubjectMapper.mapToDomainModel;
import static org.flickit.assessment.kit.common.ErrorMessageKey.GET_KIT_SUBJECT_DETAIL_SUBJECT_ID_NOT_FOUND;


@Component
@RequiredArgsConstructor
public class SubjectPersistenceJpaAdapter implements
    UpdateSubjectPort,
    CreateSubjectPort,
    LoadSubjectsPort,
    LoadSubjectPort,
    UpdateSubjectIndexPort,
    IncreaseSubjectIndexPort,
    DecreaseSubjectIndexPort {

    private final SubjectJpaRepository repository;
    private final AttributeJpaRepository attributeRepository;

    @Override
    public void update(UpdateSubjectPort.Param param) {
        repository.update(param.id(),
            param.kitVersionId(),
            param.title(),
            param.index(),
            param.description(),
            param.lastModificationTime(),
            param.lastModifiedBy()
        );
    }

    @Override
    public Long persist(CreateSubjectPort.Param param) {
        return repository.save(SubjectMapper.mapToJpaEntity(param)).getId();
    }

    @Override
    public List<Subject> loadByKitVersionId(long kitVersionId) {
        List<SubjectJpaEntity> subjectEntities = repository.findAllByKitVersionIdOrderByIndex(kitVersionId);
        List<Long> subjectEntityIds = subjectEntities.stream().map(SubjectJpaEntity::getId).toList();
        List<AttributeJpaEntity> attributeEntities = attributeRepository.findAllBySubjectIdInAndKitVersionId(subjectEntityIds, kitVersionId);
        Map<Long, List<AttributeJpaEntity>> subjectIdToAttrEntities = attributeEntities.stream()
            .collect(Collectors.groupingBy(AttributeJpaEntity::getSubjectId));

        return subjectEntities.stream()
            .map(e -> SubjectMapper.mapToDomainModel(e,
                subjectIdToAttrEntities.get(e.getId()).stream()
                    .map(AttributeMapper::mapToDomainModel)
                    .toList()))
            .toList();
    }

    @Override
    public Subject load(long subjectId, long kitVersionId) {
        var subjectEntity = repository.findByIdAndKitVersionId (subjectId, kitVersionId)
            .orElseThrow(() -> new ResourceNotFoundException(GET_KIT_SUBJECT_DETAIL_SUBJECT_ID_NOT_FOUND));
        List<AttributeJpaEntity> attributeEntities = attributeRepository.findAllBySubjectIdAndKitVersionId(subjectId, kitVersionId);
        return mapToDomainModel(subjectEntity,
            attributeEntities.stream().map(AttributeMapper::mapToDomainModel).toList());
    }

    @Override
    public void updateIndex(Long kitVersionId, Long subjectId, int index) {
        repository.updateIndex(kitVersionId, subjectId, index);
    }

    @Override
    public void decreaseSubjectsIndexes(long kitVersionId, int fromIndex, int toIndex) {
        var subjects = repository.findAllByKitVersionIdAndIndexes(kitVersionId, fromIndex, toIndex,
            PageRequest.of(0, toIndex - fromIndex, Sort.Direction.ASC, INDEX));
        subjects.forEach(s -> s.setIndex(s.getIndex() - 1));
        repository.saveAll(subjects);
    }

    @Override
    public void increaseSubjectsIndexes(long kitVersionId, int fromIndex, int toIndex) {
        var subjects = repository.findAllByKitVersionIdAndIndexes(kitVersionId, fromIndex, toIndex,
            PageRequest.of(0, toIndex - fromIndex, Sort.Direction.DESC, INDEX));
        subjects.forEach(s -> s.setIndex(s.getIndex() + 1));
        repository.saveAll(subjects);
    }
}
