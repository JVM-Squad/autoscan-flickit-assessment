package org.flickit.assessment.core.test.fixture.adapter.jpa;

import org.flickit.assessment.core.application.domain.AssessmentInvitee;
import org.flickit.assessment.core.application.domain.AssessmentUserRole;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class AssessmentInviteeJpaEntityMother {

    public static AssessmentInvitee createAssessmentInvitee(String email, Integer roleId) {
        return new AssessmentInvitee(
            UUID.randomUUID(),
            UUID.randomUUID(),
            email,
            AssessmentUserRole.valueOfById(roleId),
            LocalDateTime.now().plus(Duration.ofDays(7)),
            LocalDateTime.now(),
            UUID.randomUUID()
        );
    }
}
