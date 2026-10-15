package io.github.allannava.cragcast.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Against a real Open-Meteo response recorded on 2026-10-15 for central Milan
 * (open-meteo/milan-hourly.json): 96 hours from 2026-10-13T00:00, no rain in it.
 */
class OpenMeteoClientTest {

    private MockRestServiceServer server;
    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.open-meteo.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenMeteoClient(builder);
    }

    private void respondWithFixture() throws IOException {
        String body = new ClassPathResource("open-meteo/milan-hourly.json")
                .getContentAsString(StandardCharsets.UTF_8);
        server.expect(requestTo(startsWith("https://api.open-meteo.com/v1/forecast")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("latitude", "45.46"))
                .andExpect(queryParam("longitude", "9.19"))
                .andExpect(queryParam("hourly", "precipitation,temperature_2m,wind_speed_10m"))
                .andExpect(queryParam("past_days", "2"))
                .andExpect(queryParam("timezone", "Europe/Rome"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void parsesTheHourlyBlock() throws IOException {
        respondWithFixture();
        HourlyForecast f = client.hourly(45.46, 9.19);
        assertThat(f.time()).hasSize(96);
        assertThat(f.time().getFirst()).isEqualTo(LocalDateTime.of(2026, 10, 13, 0, 0));
        assertThat(f.temperature()).hasSize(96);
        server.verify();
    }

    @Test
    void reducesTheRecordedDayToTheScorersWeather() throws IOException {
        respondWithFixture();
        var w = client.weatherFor(45.46, 9.19, LocalDate.of(2026, 10, 15));
        // Values computed independently from the fixture.
        assertThat(w.rainLast48hMm()).isEqualTo(0.0);
        assertThat(w.rainDuringDayMm()).isEqualTo(0.0);
        assertThat(w.maxTemperatureC()).isEqualTo(20.5);
        assertThat(w.maxWindKmh()).isEqualTo(5.1);
        server.verify();
    }
}
