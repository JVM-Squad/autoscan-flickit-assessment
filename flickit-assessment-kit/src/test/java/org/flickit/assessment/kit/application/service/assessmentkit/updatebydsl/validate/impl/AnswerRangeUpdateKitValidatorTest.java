package org.flickit.assessment.kit.application.service.assessmentkit.updatebydsl.validate.impl;

import org.flickit.assessment.common.exception.api.Notification;
import org.flickit.assessment.kit.application.domain.dsl.AssessmentKitDslModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.COLLECTION;
import static org.flickit.assessment.kit.application.service.assessmentkit.updatebydsl.validate.impl.DslFieldNames.ANSWER_RANGE;
import static org.flickit.assessment.kit.test.fixture.application.AnswerRangeMother.createReusableAnswerRangeWithTwoOptions;
import static org.flickit.assessment.kit.test.fixture.application.AssessmentKitMother.kitWithAnswerRanges;
import static org.flickit.assessment.kit.test.fixture.application.dsl.AnswerRangeDslModelMother.domainToDslModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class AnswerRangeUpdateKitValidatorTest {

    @InjectMocks
    private AnswerRangeUpdateKitValidator validator;

    @Test
    void testValidate_SameAnswerRangeCodesInDbAndDsl_Valid() {
        var rangeOne = createReusableAnswerRangeWithTwoOptions();
        var rangeTwo = createReusableAnswerRangeWithTwoOptions();
        var savedKit = kitWithAnswerRanges(List.of(rangeOne, rangeTwo));

        var dslRangeOne = domainToDslModel(rangeOne, b -> b.title("new title"));
        var dslRangeTwo = domainToDslModel(rangeTwo, b -> b.title("new title2"));

        var dslKit = AssessmentKitDslModel.builder()
            .answerRanges(List.of(dslRangeOne, dslRangeTwo))
            .build();

        Notification notification = validator.validate(savedKit, dslKit);

        assertFalse(notification.hasErrors());
    }

    @Test
    void testValidate_dslHasTwoNewAnswerRanges_valid() {
        var rangeOne = createReusableAnswerRangeWithTwoOptions();
        var rangeTwo = createReusableAnswerRangeWithTwoOptions();
        var savedKit = kitWithAnswerRanges(List.of(rangeOne, rangeTwo));

        var rangeThree = createReusableAnswerRangeWithTwoOptions();
        var rangeFour = createReusableAnswerRangeWithTwoOptions();
        var dslRangeOne = domainToDslModel(rangeOne);
        var dslRangeTwo = domainToDslModel(rangeTwo);
        var dslRangeThree = domainToDslModel(rangeThree);
        var dslRangeFour = domainToDslModel(rangeFour);

        var dslKit = AssessmentKitDslModel.builder()
            .answerRanges(List.of(dslRangeOne, dslRangeTwo, dslRangeThree, dslRangeFour))
            .build();

        Notification notification = validator.validate(savedKit, dslKit);

        assertThat(notification)
            .returns(false, Notification::hasErrors);
        assertEquals(0, notification.getErrors().size());
    }

    @Test
    void testValidate_dslHasTwoAnswerRangesLessThanDb_Invalid() {
        var rangeOne = createReusableAnswerRangeWithTwoOptions();
        var rangeTwo = createReusableAnswerRangeWithTwoOptions();
        var rangeThree = createReusableAnswerRangeWithTwoOptions();
        var rangeFour = createReusableAnswerRangeWithTwoOptions();
        var savedKit = kitWithAnswerRanges(List.of(rangeOne, rangeTwo, rangeThree, rangeFour));

        var dslRangeOne = domainToDslModel(rangeOne);
        var dslRangeTwo = domainToDslModel(rangeTwo);

        var dslKit = AssessmentKitDslModel.builder()
            .answerRanges(List.of(dslRangeOne, dslRangeTwo))
            .build();

        Notification notification = validator.validate(savedKit, dslKit);

        assertThat(notification)
            .returns(true, Notification::hasErrors)
            .extracting(Notification::getErrors, as(COLLECTION))
            .singleElement()
            .isInstanceOfSatisfying(InvalidDeletionError.class, x -> {
                assertThat(x.fieldName()).isEqualTo(ANSWER_RANGE);
                assertThat(x.deletedItems()).contains(rangeThree.getCode());
                assertThat(x.deletedItems()).contains(rangeFour.getCode());
            });
    }
}
