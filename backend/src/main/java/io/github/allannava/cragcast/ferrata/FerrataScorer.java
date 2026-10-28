package io.github.allannava.cragcast.ferrata;

import io.github.allannava.cragcast.conditions.Verdict;
import java.util.ArrayList;
import java.util.List;

/**
 * CG-113: the via ferrata verdict, from the weather at the route's top. Not a crag's: the risk
 * is the storm on the cable, ice and snow on the steps and wind on an exposed ridge, not wet
 * rock on a wall, and a wrong "si va" costs more (ADR 0024). So it is stricter, and a
 * thunderstorm is never "si va" whatever else the day says.
 *
 * <p>The thresholds are a cautious first heuristic, named so each change is one reviewed line
 * with a test for the day it fixes; observed days (CG-27) will calibrate them. The avalanche
 * bulletin informs and never scores (CG-11).
 */
public class FerrataScorer {

    /** Any hour with a thunderstorm code: lightning on a steel cable. */
    static final int THUNDERSTORM_HOURS_NO_GO = 1;
    /** Unstable enough that storms build in the afternoon even when no hour shows one. */
    static final double CAPE_NO_GO_JKG = 1000;
    /** Some instability: storms possible; an early start and an eye on the sky. */
    static final double CAPE_MAYBE_JKG = 300;
    /** Stricter than a crag's 2 mm: wet steps and cables, and no lowering off. */
    static final double RAIN_DURING_DAY_NO_GO_MM = 1.0;
    /** Fresh snow at the top, in the 48 hours before or during the day, covers steps and pins. */
    static final double FRESH_SNOW_NO_GO_CM = 1.0;
    /** Rain before a frost: verglas on rock and cable. */
    static final double RAIN_BEFORE_FROST_MM = 2.0;
    static final double FROST_C = 0.0;
    /** A gust that can unbalance someone on an exposed traverse. */
    static final double GUST_NO_GO_KMH = 60;
    static final double WIND_MAYBE_KMH = 35;
    /** Recent rain: rock and cables may still be wet in the gullies. */
    static final double RECENT_RAIN_MAYBE_MM = 5.0;
    /** Too cold for bare hands on steel all day. */
    static final double COLD_MAYBE_C = 3.0;
    /** A cable in full sun burns, and water runs out. */
    static final double HOT_MAYBE_C = 30.0;
    /** Fewer hours of light than a via ferrata day with its approach and descent asks for. */
    static final int SHORT_DAY_H = 10;

    public Result score(FerrataWeather w) {
        List<String> reasons = new ArrayList<>();
        Verdict verdict = Verdict.GO;

        if (w.thunderstormHours() >= THUNDERSTORM_HOURS_NO_GO) {
            reasons.add("thunderstorm forecast");
            verdict = Verdict.NO_GO;
        } else if (w.maxCapeJkg() >= CAPE_NO_GO_JKG) {
            reasons.add("unstable air: storms can build");
            verdict = Verdict.NO_GO;
        } else if (w.maxCapeJkg() >= CAPE_MAYBE_JKG) {
            reasons.add("some instability: storms possible");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        if (w.rainDuringDayMm() > RAIN_DURING_DAY_NO_GO_MM) {
            reasons.add("rain forecast during the day");
            verdict = Verdict.NO_GO;
        }
        if (w.snowLast48hCm() + w.snowDuringDayCm() >= FRESH_SNOW_NO_GO_CM) {
            reasons.add("fresh snow at the top");
            verdict = Verdict.NO_GO;
        }
        if (w.minTemperatureC() <= FROST_C && w.rainLast48hMm() > RAIN_BEFORE_FROST_MM) {
            reasons.add("ice likely: rain, then frost at the top");
            verdict = Verdict.NO_GO;
        }
        if (w.maxGustKmh() > GUST_NO_GO_KMH) {
            reasons.add("strong gusts at the top");
            verdict = Verdict.NO_GO;
        } else if (w.maxWindKmh() > WIND_MAYBE_KMH) {
            reasons.add("windy at the top");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        if (w.rainLast48hMm() > RECENT_RAIN_MAYBE_MM) {
            reasons.add("rock and cables may still be wet from recent rain");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        if (w.maxTemperatureC() < COLD_MAYBE_C) {
            reasons.add("cold at the top");
            verdict = worst(verdict, Verdict.MAYBE);
        } else if (w.maxTemperatureC() > HOT_MAYBE_C) {
            reasons.add("hot: the cables burn in the sun");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        if (w.daylightHours() < SHORT_DAY_H) {
            reasons.add("short day: little light for the route");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        return new Result(verdict, List.copyOf(reasons));
    }

    private static Verdict worst(Verdict a, Verdict b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    public record Result(Verdict verdict, List<String> reasons) {
    }
}
