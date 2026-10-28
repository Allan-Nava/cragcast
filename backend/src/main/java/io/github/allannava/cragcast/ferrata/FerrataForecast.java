package io.github.allannava.cragcast.ferrata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CG-113: Open-Meteo's {@code hourly} block at a via ferrata's top, with what a crag's forecast
 * does not need: gusts, the WMO weather code (95, 96, 99 are thunderstorms), CAPE, snowfall and
 * whether it is light. Parallel lists, one entry per hour, local time; a precipitation or
 * snowfall value at time T is the hour ending at T. CAPE may be null where a model has none.
 */
public record FerrataForecast(
        List<LocalDateTime> time,
        List<Double> precipitation,
        @JsonProperty("temperature_2m") List<Double> temperature,
        @JsonProperty("wind_speed_10m") List<Double> windSpeed,
        @JsonProperty("wind_gusts_10m") List<Double> windGusts,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        List<Double> cape,
        List<Double> snowfall,
        @JsonProperty("is_day") List<Integer> isDay) {

    public FerrataForecast {
        int n = time.size();
        for (List<?> l : List.of(precipitation, temperature, windSpeed, windGusts, weatherCode, cape, snowfall, isDay)) {
            if (l.size() != n) {
                throw new IllegalArgumentException("hourly lists of different lengths");
            }
        }
    }
}
