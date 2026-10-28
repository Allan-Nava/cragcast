package io.github.allannava.cragcast.ferrata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * CG-113: reduces the hourly forecast at a via ferrata's top to one {@link FerrataWeather}.
 * The window is wider than a crag's (07:00–19:00, the hours ending 08:00 to 19:00): a via
 * ferrata is a longer day, and a storm at 18:00 still catches someone on the descent.
 */
public final class FerrataReducer {

    static final int FROM_HOUR = 7;
    static final int TO_HOUR = 19;
    /** WMO weather codes for a thunderstorm: slight or moderate, with hail, with heavy hail. */
    static final Set<Integer> THUNDERSTORM = Set.of(95, 96, 99);

    private FerrataReducer() {
    }

    public static FerrataWeather reduce(FerrataForecast f, LocalDate day) {
        LocalDateTime midnight = day.atStartOfDay();
        LocalDateTime pastFrom = midnight.minusHours(48);
        LocalDateTime from = day.atTime(FROM_HOUR, 0);
        LocalDateTime to = day.atTime(TO_HOUR, 0);
        double rain48 = 0, snow48 = 0, rainDay = 0, snowDay = 0, cape = 0;
        double wind = Double.NEGATIVE_INFINITY, gust = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        int storms = 0, light = 0, pastHours = 0, windowHours = 0, dayHours = 0;
        for (int i = 0; i < f.time().size(); i++) {
            LocalDateTime t = f.time().get(i);
            if (t.isAfter(pastFrom) && !t.isAfter(midnight)) {
                rain48 += f.precipitation().get(i);
                snow48 += f.snowfall().get(i);
                pastHours++;
            }
            if (t.toLocalDate().equals(day)) {
                light += f.isDay().get(i);
                dayHours++;
            }
            if (t.isAfter(from) && !t.isAfter(to)) {
                rainDay += f.precipitation().get(i);
                snowDay += f.snowfall().get(i);
                if (THUNDERSTORM.contains(f.weatherCode().get(i))) {
                    storms++;
                }
                Double c = f.cape().get(i);
                cape = Math.max(cape, c == null ? 0 : c);
                wind = Math.max(wind, f.windSpeed().get(i));
                gust = Math.max(gust, f.windGusts().get(i));
                min = Math.min(min, f.temperature().get(i));
                max = Math.max(max, f.temperature().get(i));
                windowHours++;
            }
        }
        if (pastHours < 48 || windowHours < TO_HOUR - FROM_HOUR || dayHours < 24) {
            throw new IllegalArgumentException("forecast does not cover " + day + " and the 48 hours before it ("
                    + pastHours + " of 48 past hours, " + windowHours + " window hours, " + dayHours + " of 24)");
        }
        return new FerrataWeather(rain48, snow48, rainDay, snowDay, storms, cape, wind, gust, min, max, light);
    }
}
