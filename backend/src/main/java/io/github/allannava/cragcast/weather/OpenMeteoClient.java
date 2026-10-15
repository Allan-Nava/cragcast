package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.conditions.Weather;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Hourly forecast from Open-Meteo (free, no API key): two past days for the rain that may still
 * wet the rock, three days ahead for tomorrow and the day after.
 */
@Component
public class OpenMeteoClient {

    public static final String TIMEZONE = "Europe/Rome";

    private final RestClient rest;

    @Autowired
    public OpenMeteoClient(@Value("${cragcast.open-meteo.base-url}") String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl));
    }

    OpenMeteoClient(RestClient.Builder builder) {
        this.rest = builder.build();
    }

    public HourlyForecast hourly(double latitude, double longitude) {
        Response response = rest.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "precipitation,temperature_2m,wind_speed_10m")
                        .queryParam("past_days", 2)
                        .queryParam("forecast_days", 3)
                        .queryParam("timezone", TIMEZONE)
                        .build())
                .retrieve()
                .body(Response.class);
        if (response == null || response.hourly() == null) {
            throw new IllegalStateException("Open-Meteo returned no hourly data");
        }
        return response.hourly();
    }

    /**
     * One request for many places: Open-Meteo takes comma-separated coordinates and answers
     * with one result per place, in the same order. Keeps an hourly run of every crag at one
     * call instead of one per crag.
     */
    public List<HourlyForecast> hourly(List<double[]> places) {
        if (places.isEmpty()) {
            return List.of();
        }
        String lats = places.stream().map(p -> Double.toString(p[0])).collect(Collectors.joining(","));
        String lons = places.stream().map(p -> Double.toString(p[1])).collect(Collectors.joining(","));
        Response[] responses = rest.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", lats)
                        .queryParam("longitude", lons)
                        .queryParam("hourly", "precipitation,temperature_2m,wind_speed_10m")
                        .queryParam("past_days", 2)
                        .queryParam("forecast_days", 3)
                        .queryParam("timezone", TIMEZONE)
                        .build())
                .retrieve()
                .body(Response[].class);
        if (responses == null || responses.length != places.size()) {
            throw new IllegalStateException("Open-Meteo returned " + (responses == null ? 0 : responses.length)
                    + " results for " + places.size() + " places");
        }
        return Arrays.stream(responses).map(Response::hourly).toList();
    }

    /** For callers outside Spring, such as the hourly export. */
    public static OpenMeteoClient create(String baseUrl) {
        return new OpenMeteoClient(RestClient.builder().baseUrl(baseUrl));
    }

    public Weather weatherFor(double latitude, double longitude, LocalDate day) {
        return WeatherReducer.reduce(hourly(latitude, longitude), day);
    }

    record Response(String timezone, HourlyForecast hourly) {
    }
}
