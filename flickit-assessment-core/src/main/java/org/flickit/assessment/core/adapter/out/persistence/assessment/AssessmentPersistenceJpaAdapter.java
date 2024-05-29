package org.flickit.assessment.core.adapter.out.persistence.assessment;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.application.domain.crud.PaginatedResponse;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.core.application.domain.Assessment;
import org.flickit.assessment.core.application.domain.AssessmentColor;
import org.flickit.assessment.core.application.domain.AssessmentListItem;
import org.flickit.assessment.core.application.port.out.assessment.*;
import org.flickit.assessment.data.jpa.core.answer.AnswerJpaRepository;
import org.flickit.assessment.data.jpa.core.assessment.AssessmentJpaEntity;
import org.flickit.assessment.data.jpa.core.assessment.AssessmentJpaRepository;
import org.flickit.assessment.data.jpa.core.assessment.AssessmentListItemView;
import org.flickit.assessment.data.jpa.core.assessmentresult.AssessmentResultJpaEntity;
import org.flickit.assessment.data.jpa.core.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.assessment.data.jpa.kit.assessmentkit.AssessmentKitJpaEntity;
import org.flickit.assessment.data.jpa.kit.assessmentkit.AssessmentKitJpaRepository;
import org.flickit.assessment.data.jpa.kit.maturitylevel.MaturityLevelJpaEntity;
import org.flickit.assessment.data.jpa.kit.maturitylevel.MaturityLevelJpaRepository;
import org.flickit.assessment.data.jpa.kit.question.QuestionJpaRepository;
import org.flickit.assessment.data.jpa.users.space.SpaceJpaEntity;
import org.flickit.assessment.data.jpa.users.space.SpaceJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.flickit.assessment.core.common.ErrorMessageKey.*;
import static org.flickit.assessment.data.jpa.core.assessment.AssessmentJpaEntity.Fields.ASSESSMENT_KIT_ID;
import static org.flickit.assessment.data.jpa.core.assessment.AssessmentJpaEntity.Fields.SPACE_ID;
import static org.springframework.data.jpa.domain.Specification.where;

@Component
@RequiredArgsConstructor
public class AssessmentPersistenceJpaAdapter implements
    CreateAssessmentPort,
    LoadAssessmentListItemsBySpacePort,
    UpdateAssessmentPort,
    GetAssessmentProgressPort,
    GetAssessmentPort,
    DeleteAssessmentPort,
    CountAssessmentsPort,
    CheckUserAssessmentAccessPort {

    private final AssessmentJpaRepository repository;
    private final AssessmentResultJpaRepository resultRepository;
    private final AnswerJpaRepository answerRepository;
    private final QuestionJpaRepository questionRepository;
    private final AssessmentKitJpaRepository kitRepository;
    private final SpaceJpaRepository spaceRepository;
    private final MaturityLevelJpaRepository maturityLevelRepository;

    @Override
    public UUID persist(CreateAssessmentPort.Param param) {
        AssessmentJpaEntity unsavedEntity = AssessmentMapper.mapCreateParamToJpaEntity(param);
        AssessmentJpaEntity entity = repository.save(unsavedEntity);
        return entity.getId();
    }

    @Override
    public PaginatedResponse<AssessmentListItem> loadNotDeletedAssessments(List<Long> spaceIds, Long kitId, int page, int size) {
        var pageResult = repository.findBySpaceIdAndDeletedFalseOrderByLastModificationTimeDesc(spaceIds, kitId, PageRequest.of(page, size));

        Map<UUID, Long> assessmentIdToMaturityLevelId = new HashMap<>();
        pageResult.getContent().forEach(e -> assessmentIdToMaturityLevelId.put(e.getAssessment().getId(), e.getMaturityLevelId()));

        List<AssessmentJpaEntity> assessmentEntities = pageResult.getContent().stream()
            .map(AssessmentListItemView::getAssessment)
            .toList();

        Set<Long> kitIds = assessmentEntities.stream()
            .map(AssessmentJpaEntity::getAssessmentKitId)
            .collect(Collectors.toSet());
        List<AssessmentKitJpaEntity> kitEntities = kitRepository.findAllById(kitIds);
        Map<Long, AssessmentKitJpaEntity> kitIdToKitEntity = kitEntities.stream()
            .collect(Collectors.toMap(AssessmentKitJpaEntity::getId, Function.identity()));

        Set<Long> assessmentSpaceIds = assessmentEntities.stream()
            .map(AssessmentJpaEntity::getSpaceId)
            .collect(Collectors.toSet());
        List<SpaceJpaEntity> spaceEntities = spaceRepository.findAllById(assessmentSpaceIds);
        Map<Long, SpaceJpaEntity> spaceIdToSpaceEntity = spaceEntities.stream()
            .collect(Collectors.toMap(SpaceJpaEntity::getId, Function.identity()));

        List<Long> kitVersionIds = kitEntities.stream()
            .map(AssessmentKitJpaEntity::getKitVersionId)
            .toList();
        List<MaturityLevelJpaEntity> kitMaturityLevelEntities = maturityLevelRepository.findAllByKitVersionIdIn(kitVersionIds);
        Map<Long, List<MaturityLevelJpaEntity>> kitVersionIdToMaturityLevelEntities = kitMaturityLevelEntities.stream()
            .collect(Collectors.groupingBy(MaturityLevelJpaEntity::getKitVersionId));

        List<Long> assessmentMaturityLevelIds = pageResult.getContent().stream()
            .map(AssessmentListItemView::getMaturityLevelId)
            .toList();

        Map<Long, MaturityLevelJpaEntity> maturityLevelIdToMaturityLevel =
            maturityLevelRepository.findAllById(assessmentMaturityLevelIds).stream()
                .collect(Collectors.toMap(MaturityLevelJpaEntity::getId, Function.identity()));

        List<AssessmentListItem> items = pageResult.getContent().stream()
            .map(e -> {
                AssessmentKitJpaEntity kitEntity = kitIdToKitEntity.get(e.getAssessment().getAssessmentKitId());
                List<MaturityLevelJpaEntity> kitLevelEntities = kitVersionIdToMaturityLevelEntities.get(kitEntity.getKitVersionId());
                AssessmentListItem.Kit kit = new AssessmentListItem.Kit(kitEntity.getId(), kitEntity.getTitle(), kitLevelEntities.size());
                SpaceJpaEntity spaceEntity = spaceIdToSpaceEntity.get(e.getAssessment().getSpaceId());
                AssessmentListItem.Space space = new AssessmentListItem.Space(spaceEntity.getId(), spaceEntity.getTitle());
                AssessmentListItem.MaturityLevel maturityLevel = null;
                if (e.getIsCalculateValid()) {
                    MaturityLevelJpaEntity maturityLevelEntity = maturityLevelIdToMaturityLevel.get(e.getMaturityLevelId());
                    maturityLevel = new AssessmentListItem.MaturityLevel(maturityLevelEntity.getId(),
                        maturityLevelEntity.getTitle(),
                        maturityLevelEntity.getValue(),
                        maturityLevelEntity.getIndex());
                }

                return new AssessmentListItem(e.getAssessment().getId(),
                    e.getAssessment().getTitle(),
                    kit,
                    space,
                    AssessmentColor.valueOfById(e.getAssessment().getColorId()),
                    e.getAssessment().getLastModificationTime(),
                    maturityLevel,
                    e.getIsCalculateValid(),
                    e.getIsConfidenceValid());
            }).toList();


        return new PaginatedResponse<>(
            items,
            pageResult.getNumber(),
            pageResult.getSize(),
            AssessmentJpaEntity.Fields.LAST_MODIFICATION_TIME,
            Sort.Direction.DESC.name().toLowerCase(),
            (int) pageResult.getTotalElements()
        );
    }

    @Override
    public PaginatedResponse<AssessmentListItem> loadNotDeletedAssessments(Long spaceId, int page, int size) {
        var pageResult = repository.findBySpaceId(spaceId, PageRequest.of(page, size,
            Sort.Direction.DESC, AssessmentResultJpaEntity.Fields.LAST_MODIFICATION_TIME));

        List<Long> kitVersionIds = pageResult.getContent().stream()
            .map(e -> e.getAssessmentResult().getKitVersionId())
            .toList();

        List<AssessmentKitJpaEntity> kitEntities = kitRepository.findAllByKitVersionIdIn(kitVersionIds);
        Map<Long, AssessmentKitJpaEntity> kitIdToKitEntity = kitEntities.stream()
            .collect(Collectors.toMap(AssessmentKitJpaEntity::getId, Function.identity()));

        List<MaturityLevelJpaEntity> kitMaturityLevelEntities = maturityLevelRepository.findAllByKitVersionIdIn(kitVersionIds);
        Map<Long, List<MaturityLevelJpaEntity>> kitVersionIdToMaturityLevelEntities = kitMaturityLevelEntities.stream()
            .collect(Collectors.groupingBy(MaturityLevelJpaEntity::getKitVersionId));

        Map<Long, MaturityLevelJpaEntity> maturityLevelIdToMaturityLevel =
            kitMaturityLevelEntities.stream()
                .collect(Collectors.toMap(MaturityLevelJpaEntity::getId, Function.identity()));

        List<AssessmentListItem> items = pageResult.getContent().stream()
            .map(e -> {
                AssessmentKitJpaEntity kitEntity = kitIdToKitEntity.get(e.getAssessment().getAssessmentKitId());
                List<MaturityLevelJpaEntity> kitLevelEntities = kitVersionIdToMaturityLevelEntities.get(kitEntity.getKitVersionId());
                AssessmentListItem.Kit kit = new AssessmentListItem.Kit(kitEntity.getId(), kitEntity.getTitle(), kitLevelEntities.size());
                AssessmentListItem.Space space = null;
                AssessmentListItem.MaturityLevel maturityLevel = null;
                if (e.getAssessmentResult().getIsCalculateValid()) {
                    MaturityLevelJpaEntity maturityLevelEntity = maturityLevelIdToMaturityLevel.get(e.getAssessmentResult().getMaturityLevelId());
                    maturityLevel = new AssessmentListItem.MaturityLevel(maturityLevelEntity.getId(),
                        maturityLevelEntity.getTitle(),
                        maturityLevelEntity.getValue(),
                        maturityLevelEntity.getIndex());
                }

                return new AssessmentListItem(e.getAssessment().getId(),
                    e.getAssessment().getTitle(),
                    kit,
                    space,
                    AssessmentColor.valueOfById(e.getAssessment().getColorId()),
                    e.getAssessment().getLastModificationTime(),
                    maturityLevel,
                    e.getAssessmentResult().getIsCalculateValid(),
                    e.getAssessmentResult().getIsConfidenceValid());
            }).toList();


        return new PaginatedResponse<>(
            items,
            pageResult.getNumber(),
            pageResult.getSize(),
            AssessmentJpaEntity.Fields.LAST_MODIFICATION_TIME,
            Sort.Direction.DESC.name().toLowerCase(),
            (int) pageResult.getTotalElements()
        );
    }

    @Override
    public UpdateAssessmentPort.Result update(UpdateAssessmentPort.AllParam param) {
        if (!repository.existsByIdAndDeletedFalse(param.id()))
            throw new ResourceNotFoundException(UPDATE_ASSESSMENT_ID_NOT_FOUND);

        repository.update(
            param.id(),
            param.title(),
            param.code(),
            param.colorId(),
            param.lastModificationTime(),
            param.lastModifiedBy());
        return new UpdateAssessmentPort.Result(param.id());
    }

    @Override
    public GetAssessmentProgressPort.Result getProgress(UUID assessmentId) {
        var assessmentResult = resultRepository.findFirstByAssessment_IdOrderByLastModificationTimeDesc(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException(GET_ASSESSMENT_PROGRESS_ASSESSMENT_NOT_FOUND));

        int answersCount = answerRepository.getCountByAssessmentResultId(assessmentResult.getId());
        int questionsCount = questionRepository.countByKitVersionId(assessmentResult.getKitVersionId());
        return new GetAssessmentProgressPort.Result(assessmentId, answersCount, questionsCount);
    }

    @Override
    public Optional<Assessment> getAssessmentById(UUID assessmentId) {
        Optional<AssessmentJpaEntity> entity = repository.findByIdAndDeletedFalse(assessmentId);
        return entity.map(AssessmentMapper::mapToDomainModel);
    }

    @Override
    public void deleteById(UUID id, Long deletionTime) {
        if (!repository.existsByIdAndDeletedFalse(id))
            throw new ResourceNotFoundException(DELETE_ASSESSMENT_ID_NOT_FOUND);

        repository.delete(id, deletionTime);
    }

    @Override
    public CountAssessmentsPort.Result count(CountAssessmentsPort.Param param) {
        Integer totalCount = null;
        Integer deletedCount = null;
        Integer notDeletedCount = null;

        Specification<AssessmentJpaEntity> baseCondition = (r, cq, cb) -> {
            Predicate p = cb.and();
            if (param.kitId() != null)
                p = cb.and(cb.equal(r.get(ASSESSMENT_KIT_ID), param.kitId()), p);
            if (param.spaceId() != null)
                p = cb.and(cb.equal(r.get(SPACE_ID), param.spaceId()), p);
            return p;
        };

        if (param.deleted()) {
            deletedCount = (int) repository.count(where(baseCondition).and(deletedCondition(true)));
        }
        if (param.notDeleted()) {
            notDeletedCount = (int) repository.count(where(baseCondition).and(deletedCondition(false)));
        }
        if (param.total()) {
            if (param.deleted() && param.notDeleted())
                totalCount = deletedCount + notDeletedCount;
            else
                totalCount = (int) repository.count(where(baseCondition));
        }

        return new CountAssessmentsPort.Result(totalCount, deletedCount, notDeletedCount);
    }

    private static Specification<AssessmentJpaEntity> deletedCondition(boolean deleted) {
        return (r, cq, cb) -> cb.equal(r.get("deleted"), deleted);
    }

    @Override
    public void updateLastModificationTime(UUID id, LocalDateTime lastModificationTime) {
        repository.updateLastModificationTime(id, lastModificationTime);
    }

    @Override
    public boolean hasAccess(UUID assessmentId, UUID userId) {
        return repository.checkUserAccess(assessmentId, userId).isPresent();
    }
}
