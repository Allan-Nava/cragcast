package io.github.allannava.cragcast.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Open-Meteo's {@code hourly} block: parallel lists, one entry per hour, times local to the
 * requested timezone. A precipitation value at time T is the rain of the hour ending at T.
 */
public record HourlyForecast(
        List<LocalDateTime> time,
        List<Double> precipitation,
        @JsonProperty("temperature_2m") List<Double> temperature,
        @JsonProperty("wind_speed_10m") List<Double> windSpeed) {

    public HourlyForecast {
        int n = time.size();
        if (precipitation.size() != n || temperature.size() != n || windSpeed.size() != n) {
            throw new IllegalArgumentException("hourly lists of different lengths");
        }
    }
}
