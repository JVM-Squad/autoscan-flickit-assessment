package org.flickit.assessment.core.application.port.out.subjectvalue;

import org.flickit.assessment.core.application.domain.SubjectValue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoadSubjectValuePort {

    SubjectValue load(long subjectId, UUID assessmentResultId);

    List<SubjectValue> loadAll(UUID assessmentResultId, Collection<Long> subjectId);
}
