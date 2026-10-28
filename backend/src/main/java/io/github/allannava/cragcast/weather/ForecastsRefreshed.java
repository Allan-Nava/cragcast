package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.ferrata.FerrataForecast;
import java.time.Instant;
import java.util.Map;

/**
 * Published after each successful hourly refresh: the hourly forecast of every crag, by slug,
 * as fetched — so another part (CG-110: the page's data) builds on it without asking Open-Meteo
 * again.
 */
public record ForecastsRefreshed(Map<String, HourlyForecast> forecasts, Map<String, FerrataForecast> ferrate,
                                 Instant fetchedAt) {

    /** CG-115: {@code ferrate}, the via ferratas' forecasts at their tops by id — empty when none or failed. */
    public ForecastsRefreshed(Map<String, HourlyForecast> forecasts, Instant fetchedAt) {
        this(forecasts, Map.of(), fetchedAt);
    }
}
