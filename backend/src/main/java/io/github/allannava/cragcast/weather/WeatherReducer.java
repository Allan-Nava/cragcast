package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.conditions.Weather;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reduces an hourly forecast to the four numbers the scorer reads for one day.
 *
 * <ul>
 *   <li>Rain in the last 48 h: the hours ending after midnight two days before, up to the
 *       day's midnight.</li>
 *   <li>The climbing window: the hours ending from 09:00 to 18:00, i.e. 08:00–18:00. Rain is
 *       summed over it; temperature and wind take its maximum.</li>
 * </ul>
 */
public final class WeatherReducer {

    static final int CLIMB_FROM_HOUR = 8;
    static final int CLIMB_TO_HOUR = 18;

    private WeatherReducer() {
    }

    public static Weather reduce(HourlyForecast f, LocalDate day) {
        LocalDateTime midnight = day.atStartOfDay();
        LocalDateTime rainFrom = midnight.minusHours(48);
        LocalDateTime climbFrom = day.atTime(CLIMB_FROM_HOUR, 0);
        LocalDateTime climbTo = day.atTime(CLIMB_TO_HOUR, 0);

        double rain48 = 0;
        double rainDay = 0;
        double maxTemp = Double.NEGATIVE_INFINITY;
        double maxWind = Double.NEGATIVE_INFINITY;
        int rainHours = 0;
        int climbHours = 0;

        for (int i = 0; i < f.time().size(); i++) {
            LocalDateTime t = f.time().get(i);
            if (t.isAfter(rainFrom) && !t.isAfter(midnight)) {
                rain48 += f.precipitation().get(i);
                rainHours++;
            }
            if (t.isAfter(climbFrom) && !t.isAfter(climbTo)) {
                rainDay += f.precipitation().get(i);
                maxTemp = Math.max(maxTemp, f.temperature().get(i));
                maxWind = Math.max(maxWind, f.windSpeed().get(i));
                climbHours++;
            }
        }

        if (rainHours < 48 || climbHours < CLIMB_TO_HOUR - CLIMB_FROM_HOUR) {
            throw new IllegalArgumentException("forecast does not cover " + day
                    + " and the 48 hours before it (" + rainHours + " of 48 past hours, "
                    + climbHours + " climbing hours)");
        }
        return new Weather(rain48, rainDay, maxTemp, maxWind);
    }
}
