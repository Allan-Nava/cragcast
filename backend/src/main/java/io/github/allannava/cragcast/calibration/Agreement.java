package io.github.allannava.cragcast.calibration;

import io.github.allannava.cragcast.calibration.Observation.Rock;
import io.github.allannava.cragcast.conditions.Verdict;

/**
 * How a verdict compares with what was observed. The two ways of being wrong are not equal:
 * sending climbers to wet rock is worse than keeping them home from dry rock.
 */
public enum Agreement {
    /** dry → GO, damp → MAYBE, wet → MAYBE or NO_GO ("rock may still be wet" counts as a warning). */
    AGREES,
    /** The rock was dry, cragcast said MAYBE or NO_GO. */
    TOO_CAUTIOUS,
    /** The rock was damp or wet, cragcast said GO: the error that matters. */
    MISSED_WET_ROCK,
    /** The rock was damp, cragcast said NO_GO. */
    TOO_HARSH;

    public static Agreement of(Rock observed, Verdict verdict) {
        return switch (observed) {
            case DRY -> verdict == Verdict.GO ? AGREES : TOO_CAUTIOUS;
            case DAMP -> switch (verdict) {
                case GO -> MISSED_WET_ROCK;
                case MAYBE -> AGREES;
                case NO_GO -> TOO_HARSH;
            };
            case WET -> verdict == Verdict.GO ? MISSED_WET_ROCK : AGREES;
        };
    }
}
