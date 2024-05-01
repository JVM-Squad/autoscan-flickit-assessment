package org.flickit.assessment.core.test.fixture.application;

import org.flickit.assessment.core.application.domain.MaturityLevel;

import java.util.List;

public class MaturityLevelMother {

    public static final long LEVEL_ONE_ID = 10;
    public static final long LEVEL_TWO_ID = 20;
    public static final long LEVEL_THREE_ID = 30;
    public static final long LEVEL_FOUR_ID = 40;
    public static final long LEVEL_FIVE_ID = 50;

    public static List<MaturityLevel> allLevels() {
        return List.of(levelOne(), levelTwo(), levelThree(), levelFour(), levelFive());
    }

    public static MaturityLevel levelOne() {
        return new MaturityLevel(LEVEL_ONE_ID, "Undeveloped", 1, 1,
            List.of());
    }

    public static MaturityLevel levelTwo() {
        return new MaturityLevel(LEVEL_TWO_ID, "Partial", 2, 2,
            List.of(LevelCompetenceMother.onLevelTwo(60)));
    }

    public static MaturityLevel levelThree() {
        return new MaturityLevel(LEVEL_THREE_ID, "Discussable", 3, 3,
            List.of(LevelCompetenceMother.onLevelTwo(75),
                LevelCompetenceMother.onLevelThree(60)));
    }

    public static MaturityLevel levelFour() {
        return new MaturityLevel(LEVEL_FOUR_ID, "Acceptable", 4, 4,
            List.of(LevelCompetenceMother.onLevelTwo(85),
                LevelCompetenceMother.onLevelThree(75),
                LevelCompetenceMother.onLevelFour(60)));
    }

    public static MaturityLevel levelFive() {
        return new MaturityLevel(LEVEL_FIVE_ID, "Great", 5, 5,
            List.of(LevelCompetenceMother.onLevelTwo(95),
                LevelCompetenceMother.onLevelThree(85),
                LevelCompetenceMother.onLevelFour(70),
                LevelCompetenceMother.onLevelFive(60)));
    }
}
