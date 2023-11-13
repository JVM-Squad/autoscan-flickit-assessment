package org.flickit.assessment.kit.adapter.out.updatekitbydsl;

import lombok.AllArgsConstructor;
import org.flickit.assessment.kit.application.domain.AssessmentKit;
import org.flickit.assessment.kit.application.domain.Level;
import org.flickit.assessment.kit.application.port.out.assessmentkit.LoadAssessmentKitInfoPort;
import org.flickit.assessment.kit.application.port.out.levelcomptenece.LoadLevelCompetenceAsMapByMaturityLevelPort;
import org.flickit.assessment.kit.application.port.out.maturitylevel.LoadAssessmentKitMaturityLevelModelsByKitPort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@AllArgsConstructor
public class LoadAssessmentKitInfoAdapter implements LoadAssessmentKitInfoPort {

    private final LoadAssessmentKitMaturityLevelModelsByKitPort loadAssessmentKitMaturityLevelModelsByKitPort;
    private final LoadLevelCompetenceAsMapByMaturityLevelPort loadLevelCompetenceAsMapByMaturityLevelPort;

    @Override
    public AssessmentKit load(Long kitId) {
        List<Level> levels = loadAssessmentKitMaturityLevelModelsByKitPort.load(kitId);
        setLevelIndexes(levels);
        setLevelCompetences(levels);

        return new AssessmentKit(
            levels,
            Boolean.FALSE
        );
    }

    private void setLevelIndexes(List<Level> levels) {
        levels.sort(Comparator.comparing(Level::getValue));
        for (int i = 0; i < levels.size(); i++) {
            levels.get(i).setIndex(i);
        }
    }

    private void setLevelCompetences(List<Level> levels) {
        levels.forEach(level -> level.setLevelCompetence(
            loadLevelCompetenceAsMapByMaturityLevelPort.loadByMaturityLevelId(level.getId())));
    }
}
