package org.flickit.assessment.common.adapter.out.novu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    GRANT_USER_ASSESSMENT_ROLE,
    CREATE_ASSESSMENT,
    COMPLETE_ASSESSMENT;

    final String code;

    NotificationType() {
        this.code = name().toLowerCase().replace("_", "");
    }
}
