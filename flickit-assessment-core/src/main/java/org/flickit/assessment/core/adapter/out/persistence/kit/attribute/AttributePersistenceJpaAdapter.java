package org.flickit.assessment.core.adapter.out.persistence.kit.attribute;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.application.domain.crud.PaginatedResponse;
import org.flickit.assessment.common.application.domain.crud.Sort;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.core.application.domain.Attribute;
import org.flickit.assessment.core.application.port.out.attribute.LoadAttributePort;
import org.flickit.assessment.core.application.port.out.attribute.LoadAttributeScoreDetailPort;
import org.flickit.assessment.core.application.port.out.attribute.LoadAttributeScoresPort;
import org.flickit.assessment.data.jpa.core.answer.AnswerJpaEntity;
import org.flickit.assessment.data.jpa.core.assessmentresult.AssessmentResultJpaRepository;
import org.flickit.assessment.data.jpa.kit.asnweroptionimpact.AnswerOptionImpactJpaEntity;
import org.flickit.assessment.data.jpa.kit.attribute.AttributeJpaRepository;
import org.flickit.assessment.data.jpa.kit.questionimpact.QuestionImpactJpaEntity;
import org.flickit.assessment.data.jpa.kit.questionnaire.QuestionnaireJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import org.flickit.assessment.common.application.domain.crud.Order;

import java.util.List;
import java.util.UUID;

import static org.flickit.assessment.core.adapter.out.persistence.kit.attribute.AttributeMapper.mapToDomainModel;
import static org.flickit.assessment.core.common.ErrorMessageKey.*;

@Component("coreAttributePersistenceJpaAdapter")
@RequiredArgsConstructor
public class AttributePersistenceJpaAdapter implements
    LoadAttributeScoreDetailPort,
    LoadAttributePort,
    LoadAttributeScoresPort {

    private final AttributeJpaRepository repository;
    private final AssessmentResultJpaRepository assessmentResultRepository;

    @Override
    public PaginatedResponse<LoadAttributeScoreDetailPort.Result> loadScoreDetail(LoadAttributeScoreDetailPort.Param param) {
        var assessmentResult = assessmentResultRepository.findFirstByAssessment_IdOrderByLastModificationTimeDesc(param.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException(GET_ATTRIBUTE_SCORE_DETAIL_ASSESSMENT_RESULT_NOT_FOUND));

        var pageRequest = makePageRequest(param.page(), param.size(), param.sort(), param.order());
        var pageResult = repository.findImpactFullQuestionsScore(assessmentResult.getId(), assessmentResult.getKitVersionId(), param.attributeId(), param.maturityLevelId(), pageRequest);

        var items = pageResult.getContent().stream()
            .map(view -> new LoadAttributeScoreDetailPort.Result(view.getQuestionnaireTitle(),
                view.getQuestionTitle(),
                view.getQuestionIndex(),
                view.getOptionTitle(),
                view.getAnswer() == null ? null : view.getAnswer().getIsNotApplicable(),
                view.getQuestionImpact().getWeight(),
                getScore(view.getAnswer(), view.getOptionImpact(), view.getOptionValue()),
                view.getOptionImpact() == null ? 0 : getValue(view.getOptionImpact(), view.getOptionValue()) * view.getQuestionImpact().getWeight(),
                view.getAnswer() != null && view.getAnswer().getConfidenceLevelId() != null ? view.getAnswer().getConfidenceLevelId() : null))
            .toList();

        return new PaginatedResponse<>(
            items,
            pageRequest.getPageNumber(),
            pageRequest.getPageSize(),
            param.order().getTitle(),
            param.sort().getTitle(),
            (int) pageResult.getTotalElements()
        );
    }

    private PageRequest makePageRequest(int page, int size, Sort sort, Order order) {
        org.springframework.data.domain.Sort.Direction orderField = switch (order) {
            case Order.ASC -> org.springframework.data.domain.Sort.Direction.ASC;
            case Order.DESC -> org.springframework.data.domain.Sort.Direction.DESC;
        };

        String sortField = switch (sort) {
            case Sort.QUESTIONNAIRE_TITLE -> "qr." + QuestionnaireJpaEntity.Fields.title;
            case Sort.WEIGHT -> "qi." + QuestionImpactJpaEntity.Fields.weight;
            case Sort.CONFIDENCE -> "ans." + AnswerJpaEntity.Fields.confidenceLevelId;
            default -> QuestionnaireJpaEntity.Fields.title;
        };

        return PageRequest.of(page, size, orderField, sortField);
    }

    @Override
    public List<LoadAttributeScoresPort.Result> loadScores(UUID assessmentId, long attributeId, long maturityLevelId) {
        var assessmentResult = assessmentResultRepository.findFirstByAssessment_IdOrderByLastModificationTimeDesc(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException(GET_ATTRIBUTE_SCORE_STATS_ASSESSMENT_RESULT_NOT_FOUND));

        return repository.findScoreStats(assessmentResult.getId(), assessmentResult.getKitVersionId(), attributeId, maturityLevelId)
            .stream()
            .map(view -> new LoadAttributeScoresPort.Result(view.getQuestionId(),
                view.getQuestionWeight(),
                getScore(view.getAnswer(), view.getOptionImpact(), view.getOptionValue()),
                view.getAnswer() != null && view.getAnswerIsNotApplicable() != null && view.getAnswer().getIsNotApplicable()))
            .toList();
    }

    private Double getScore(AnswerJpaEntity answer, AnswerOptionImpactJpaEntity optionImpact, Double optionValue) {
        if (answer == null) // if no answer is submitted for the question
            return 0.0;
        if (Boolean.TRUE.equals(answer.getIsNotApplicable())) // if there is an answer and notApplicable == true
            return null;
        if (optionImpact == null) // if there exists an answer and notApplicable != true and no option is selected
            return 0.0;
        return getValue(optionImpact, optionValue);
    }

    private Double getValue(AnswerOptionImpactJpaEntity optionImpact, Double optionValue) {
        if (optionImpact.getValue() != null)
            return optionImpact.getValue();
        return optionValue != null ? optionValue : 0.0;
    }

    @Override
    public Attribute load(Long attributeId, Long kitVersionId) {
        var attribute = repository.findByIdAndKitVersionId(attributeId, kitVersionId)
            .orElseThrow(() -> new ResourceNotFoundException(ATTRIBUTE_ID_NOT_FOUND));
        return mapToDomainModel(attribute);
    }
}
