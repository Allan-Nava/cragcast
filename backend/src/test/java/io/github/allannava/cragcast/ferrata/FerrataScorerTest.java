package io.github.allannava.cragcast.ferrata;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.conditions.Verdict;
import org.junit.jupiter.api.Test;

/** One made-up day per threshold, each changing one number of a fine day. */
class FerrataScorerTest {

    private final FerrataScorer scorer = new FerrataScorer();

    /** Dry, calm, mild, no instability, a long day. */
    private static FerrataWeather fine() {
        return new FerrataWeather(0, 0, 0, 0, 0, 50, 15, 25, 8, 16, 12);
    }

    private static FerrataWeather with(double rain48, double snow48, double rainDay, double snowDay, int storms,
                                       double cape, double wind, double gust, double min, double max, int light) {
        return new FerrataWeather(rain48, snow48, rainDay, snowDay, storms, cape, wind, gust, min, max, light);
    }

    private void assertVerdict(FerrataWeather w, Verdict verdict, String reason) {
        var r = scorer.score(w);
        assertThat(r.verdict()).isEqualTo(verdict);
        assertThat(r.reasons()).contains(reason);
    }

    @Test
    void aFineDayIsGo() {
        var r = scorer.score(fine());
        assertThat(r.verdict()).isEqualTo(Verdict.GO);
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void oneThunderstormHourIsNoGoWhateverElseTheDaySays() {
        assertVerdict(with(0, 0, 0, 0, 1, 0, 5, 10, 10, 18, 14), Verdict.NO_GO, "thunderstorm forecast");
    }

    @Test
    void capeThresholds() {
        assertThat(scorer.score(with(0, 0, 0, 0, 0, 299, 15, 25, 8, 16, 12)).verdict()).isEqualTo(Verdict.GO);
        assertVerdict(with(0, 0, 0, 0, 0, 300, 15, 25, 8, 16, 12), Verdict.MAYBE, "some instability: storms possible");
        assertVerdict(with(0, 0, 0, 0, 0, 1000, 15, 25, 8, 16, 12), Verdict.NO_GO, "unstable air: storms can build");
    }

    @Test
    void rainInTheDayIsStricterThanAtACrag() {
        assertThat(scorer.score(with(0, 0, 1.0, 0, 0, 50, 15, 25, 8, 16, 12)).verdict()).isEqualTo(Verdict.GO);
        assertVerdict(with(0, 0, 1.5, 0, 0, 50, 15, 25, 8, 16, 12), Verdict.NO_GO, "rain forecast during the day");
    }

    @Test
    void freshSnowBeforeOrDuringTheDayIsNoGo() {
        assertVerdict(with(0, 1, 0, 0, 0, 50, 15, 25, -2, 4, 12), Verdict.NO_GO, "fresh snow at the top");
        assertVerdict(with(0, 0.5, 0, 0.5, 0, 50, 15, 25, -2, 4, 12), Verdict.NO_GO, "fresh snow at the top");
    }

    @Test
    void rainThenFrostIsIce() {
        assertVerdict(with(3, 0, 0, 0, 0, 50, 15, 25, -1, 6, 12), Verdict.NO_GO, "ice likely: rain, then frost at the top");
        assertThat(scorer.score(with(3, 0, 0, 0, 0, 50, 15, 25, 1, 6, 12)).reasons())
                .doesNotContain("ice likely: rain, then frost at the top");
    }

    @Test
    void gustsAreNoGoAndSustainedWindIsMaybe() {
        assertVerdict(with(0, 0, 0, 0, 0, 50, 30, 61, 8, 16, 12), Verdict.NO_GO, "strong gusts at the top");
        assertVerdict(with(0, 0, 0, 0, 0, 50, 36, 50, 8, 16, 12), Verdict.MAYBE, "windy at the top");
    }

    @Test
    void recentRainColdHeatAndAShortDayAreMaybe() {
        assertVerdict(with(6, 0, 0, 0, 0, 50, 15, 25, 8, 16, 12), Verdict.MAYBE,
                "rock and cables may still be wet from recent rain");
        assertVerdict(with(0, 0, 0, 0, 0, 50, 15, 25, 0.5, 2.5, 12), Verdict.MAYBE, "cold at the top");
        assertVerdict(with(0, 0, 0, 0, 0, 50, 15, 25, 20, 31, 14), Verdict.MAYBE, "hot: the cables burn in the sun");
        assertVerdict(with(0, 0, 0, 0, 0, 50, 15, 25, 8, 16, 9), Verdict.MAYBE, "short day: little light for the route");
    }

    @Test
    void aMaybeNeverSoftensANoGo() {
        var r = scorer.score(with(6, 0, 0, 0, 1, 50, 36, 50, 1, 2, 9));
        assertThat(r.verdict()).isEqualTo(Verdict.NO_GO);
        assertThat(r.reasons()).contains("thunderstorm forecast", "windy at the top", "cold at the top");
    }
}
