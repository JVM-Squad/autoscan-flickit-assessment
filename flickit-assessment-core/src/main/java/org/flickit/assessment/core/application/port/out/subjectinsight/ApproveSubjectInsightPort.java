package org.flickit.assessment.core.application.port.out.subjectinsight;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ApproveSubjectInsightPort {

    void approve(UUID assessmentId, long subjectId, LocalDateTime lastModificationTime);

    void approveAll(UUID assessmentId, LocalDateTime lastModificationTime);
}
