package io.github.allannava.cragcast.ferrata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Against a real Open-Meteo answer recorded on 2026-10-28 for two arbitrary points, not via
 * ferratas (open-meteo/two-tops-ferrata.json): 45.9,9.4 at 1500 m and 46.1,9.6 at 2500 m,
 * 120 hours from 2026-10-26T00:00.
 */
class FerrataClientTest {

    private static final List<FerrataClient.Top> TWO =
            List.of(new FerrataClient.Top(45.9, 9.4, 1500), new FerrataClient.Top(46.1, 9.6, 2500));

    private MockRestServiceServer server;
    private FerrataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.open-meteo.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FerrataClient(builder);
    }

    private void respond(String body) {
        server.expect(requestTo(startsWith("https://api.open-meteo.com/v1/forecast")))
                .andExpect(queryParam("latitude", "45.9,46.1"))
                .andExpect(queryParam("longitude", "9.4,9.6"))
                .andExpect(queryParam("elevation", "1500.0,2500.0"))
                .andExpect(queryParam("hourly", FerrataClient.HOURLY))
                .andExpect(queryParam("past_days", "2"))
                .andExpect(queryParam("forecast_days", "3"))
                .andExpect(queryParam("timezone", "Europe/Rome"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void oneForecastPerTopReducibleForTodayToTheDayAfterTomorrow() throws IOException {
        respond(new ClassPathResource("open-meteo/two-tops-ferrata.json").getContentAsString(StandardCharsets.UTF_8));

        List<FerrataForecast> f = client.hourly(TWO);

        server.verify();
        assertThat(f).hasSize(2);
        assertThat(f.getFirst().time()).hasSize(120).first().isEqualTo(LocalDateTime.of(2026, 10, 26, 0, 0));
        for (int d = 0; d < 3; d++) {
            FerrataWeather w = FerrataReducer.reduce(f.getFirst(), LocalDate.of(2026, 10, 28).plusDays(d));
            assertThat(w.daylightHours()).isBetween(9, 12);
            assertThat(new FerrataScorer().score(w).verdict()).isNotNull();
        }
        // The higher top is colder: the elevation was applied.
        assertThat(FerrataReducer.reduce(f.get(1), LocalDate.of(2026, 10, 28)).maxTemperatureC())
                .isLessThan(FerrataReducer.reduce(f.getFirst(), LocalDate.of(2026, 10, 28)).maxTemperatureC());
    }

    @Test
    void noTopsNoRequest() {
        assertThat(client.hourly(List.of())).isEmpty();
        server.verify();
    }

    @Test
    void aShortAnswerIsRefused() throws IOException {
        String one = "[" + new ClassPathResource("open-meteo/two-tops-ferrata.json").getContentAsString(StandardCharsets.UTF_8)
                .substring(1).split("\\},\\{\"latitude\"")[0] + "}]";
        respond(one);
        assertThatThrownBy(() -> client.hourly(TWO)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createBuildsAClientForCallersOutsideSpring() {
        assertThat(FerrataClient.create("https://api.open-meteo.com")).isNotNull();
    }
}
