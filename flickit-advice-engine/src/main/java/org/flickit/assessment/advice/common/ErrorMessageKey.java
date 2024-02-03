package org.flickit.assessment.advice.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorMessageKey {

    public static final String SUGGEST_ADVICE_ASSESSMENT_ID_NOT_NULL = "suggest-advice.assessmentId.notNull";
    public static final String SUGGEST_ADVICE_TARGETS_NOT_NULL = "suggest-advice.targets.notNull";
    public static final String SUGGEST_ADVICE_TARGETS_SIZE_MIN = "suggest-advice.targets.size.min";
    public static final String SUGGEST_ADVICE_ASSESSMENT_RESULT_NOT_FOUND = "suggest-advice.assessmentResult.notFound";
    public static final String SUGGEST_ADVICE_ASSESSMENT_RESULT_NOT_VALID = "suggest-advice.assessmentResult.notValid";
    public static final String SUGGEST_ADVICE_FINDING_BEST_SOLUTION_EXCEPTION = "suggest-advice.finding-best-solution.execution";
}
