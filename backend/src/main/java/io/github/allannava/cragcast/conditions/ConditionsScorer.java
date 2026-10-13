package io.github.allannava.cragcast.conditions;

import io.github.allannava.cragcast.crag.Aspect;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a day's weather and a wall's aspect into a verdict, with the reasons.
 *
 * <p>The thresholds are a first heuristic, not a model: wet rock is the main risk, and a
 * sunny aspect forgives more recent rain than a shaded one. They are named constants so a
 * change to them is one reviewed line (CG-4 calibrates them against real days).
 */
public class ConditionsScorer {

    /** Rain during the day above this is a no-go on its own. */
    static final double RAIN_DURING_DAY_NO_GO_MM = 2.0;
    /** Recent rain a sunny wall (S, SE, SW) dries off; a shaded one gets half of it. */
    static final double RECENT_RAIN_SUNNY_MM = 10.0;
    static final double WIND_NO_GO_KMH = 50.0;
    static final double TOO_COLD_C = 5.0;
    static final double TOO_HOT_C = 32.0;

    public Result score(Aspect aspect, Weather weather) {
        List<String> reasons = new ArrayList<>();
        Verdict verdict = Verdict.GO;

        if (weather.rainDuringDayMm() > RAIN_DURING_DAY_NO_GO_MM) {
            reasons.add("rain forecast during the day");
            verdict = Verdict.NO_GO;
        }
        if (weather.maxWindKmh() > WIND_NO_GO_KMH) {
            reasons.add("strong wind");
            verdict = Verdict.NO_GO;
        }

        double recentRainLimit = isSunny(aspect) ? RECENT_RAIN_SUNNY_MM : RECENT_RAIN_SUNNY_MM / 2;
        if (weather.rainLast48hMm() > recentRainLimit) {
            reasons.add("rock may still be wet from recent rain");
            verdict = worst(verdict, Verdict.MAYBE);
        }
        if (weather.maxTemperatureC() < TOO_COLD_C) {
            reasons.add("cold");
            verdict = worst(verdict, Verdict.MAYBE);
        } else if (weather.maxTemperatureC() > TOO_HOT_C && isSunny(aspect)) {
            reasons.add("hot on a sunny wall");
            verdict = worst(verdict, Verdict.MAYBE);
        }

        return new Result(verdict, List.copyOf(reasons));
    }

    static boolean isSunny(Aspect aspect) {
        return aspect == Aspect.S || aspect == Aspect.SE || aspect == Aspect.SW;
    }

    private static Verdict worst(Verdict a, Verdict b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    public record Result(Verdict verdict, List<String> reasons) {
    }
}
