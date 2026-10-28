package io.github.allannava.cragcast.ferrata;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.client.RestClient;

/**
 * CG-113: the hourly forecast at many via ferratas' tops in one Open-Meteo request, as
 * {@code OpenMeteoClient} does for the crags. The top's DEM elevation goes with the coordinates,
 * so Open-Meteo corrects the temperature to the height of the route, not of its grid cell.
 */
public final class FerrataClient {

    static final String HOURLY =
            "precipitation,temperature_2m,wind_speed_10m,wind_gusts_10m,weather_code,cape,snowfall,is_day";
    static final String TIMEZONE = "Europe/Rome";

    /** A via ferrata's top: where the forecast is asked for. */
    public record Top(double latitude, double longitude, double elevation) {
    }

    private final RestClient rest;

    FerrataClient(RestClient.Builder builder) {
        this.rest = builder.build();
    }

    /** For callers outside Spring, such as the hourly export. */
    public static FerrataClient create(String baseUrl) {
        return new FerrataClient(RestClient.builder().baseUrl(baseUrl));
    }

    /** One forecast per top, in the same order: two past days and three ahead. */
    public List<FerrataForecast> hourly(List<Top> tops) {
        if (tops.isEmpty()) {
            return List.of();
        }
        Response[] responses = rest.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", join(tops, Top::latitude))
                        .queryParam("longitude", join(tops, Top::longitude))
                        .queryParam("elevation", join(tops, Top::elevation))
                        .queryParam("hourly", HOURLY)
                        .queryParam("past_days", 2)
                        .queryParam("forecast_days", 3)
                        .queryParam("timezone", TIMEZONE)
                        .build())
                .retrieve()
                .body(Response[].class);
        if (responses == null || responses.length != tops.size()) {
            throw new IllegalStateException("Open-Meteo returned " + (responses == null ? 0 : responses.length)
                    + " results for " + tops.size() + " via ferratas");
        }
        return Arrays.stream(responses).map(Response::hourly).toList();
    }

    private static String join(List<Top> tops, Function<Top, Double> value) {
        return tops.stream().map(t -> Double.toString(value.apply(t))).collect(Collectors.joining(","));
    }

    record Response(FerrataForecast hourly) {
    }
}
