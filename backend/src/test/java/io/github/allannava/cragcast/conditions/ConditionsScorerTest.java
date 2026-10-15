package io.github.allannava.cragcast.conditions;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.crag.Aspect;
import org.junit.jupiter.api.Test;

class ConditionsScorerTest {

    private final ConditionsScorer scorer = new ConditionsScorer();

    private static Weather dryMild() {
        return new Weather(0, 0, 18, 10);
    }

    @Test
    void dryMildDayIsGo() {
        var result = scorer.score(Aspect.S, dryMild());
        assertThat(result.verdict()).isEqualTo(Verdict.GO);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void rainDuringTheDayIsNoGo() {
        var result = scorer.score(Aspect.S, new Weather(0, 5, 18, 10));
        assertThat(result.verdict()).isEqualTo(Verdict.NO_GO);
        assertThat(result.reasons()).contains("rain forecast during the day");
    }

    @Test
    void recentRainIsForgivenBySunButNotByShade() {
        var eightMmYesterday = new Weather(8, 0, 18, 10);
        assertThat(scorer.score(Aspect.S, eightMmYesterday).verdict()).isEqualTo(Verdict.GO);
        assertThat(scorer.score(Aspect.N, eightMmYesterday).verdict()).isEqualTo(Verdict.MAYBE);
    }

    @Test
    void noGoIsNotSoftenedByLaterMaybes() {
        var windyColdAndWet = new Weather(30, 0, 2, 70);
        assertThat(scorer.score(Aspect.N, windyColdAndWet).verdict()).isEqualTo(Verdict.NO_GO);
    }

    @Test
    void anUnmappedAspectIsJudgedAsShadeAndSaysSo() {
        var eightMmYesterday = new Weather(8, 0, 18, 10);
        var result = scorer.score(null, eightMmYesterday);
        assertThat(result.verdict()).isEqualTo(Verdict.MAYBE);
        assertThat(result.reasons()).contains("wall aspect not mapped: judged as a shaded wall");
    }

    @Test
    void heatOnlyMattersOnSunnyWalls() {
        var hot = new Weather(0, 0, 35, 10);
        assertThat(scorer.score(Aspect.SW, hot).verdict()).isEqualTo(Verdict.MAYBE);
        assertThat(scorer.score(Aspect.N, hot).verdict()).isEqualTo(Verdict.GO);
    }
}
