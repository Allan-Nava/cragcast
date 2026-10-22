package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import io.github.allannava.cragcast.weather.ForecastsRefreshed;
import io.github.allannava.cragcast.weather.HourlyForecast;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/** CG-110: the page's data from the service. Synthetic crags and forecasts, made up for the test. */
class SiteDataTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Clock JUNE_2 = Clock.fixed(LocalDateTime.of(2026, 6, 2, 10, 0).atZone(ROME).toInstant(), ROME);
    private static final List<LocalDate> DAYS = List.of(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 4));

    private static HourlyForecast dry() {
        List<LocalDateTime> t = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            t.add(LocalDateTime.of(2026, 5, 31, 0, 0).plusHours(i));
        }
        return new HourlyForecast(t, new ArrayList<>(Collections.nCopies(120, 0.0)),
                new ArrayList<>(Collections.nCopies(120, 17.0)), new ArrayList<>(Collections.nCopies(120, 9.0)));
    }

    private static ConditionsExport.CragsFile threeCrags() {
        return new ConditionsExport.CragsFile("Test", "test", "test licence", "test elevation", List.of(
                new ConditionsExport.Crag("t-1", "Sunny Test Wall", 45.0, 9.0, Aspect.S, "https://example.org/1",
                        Map.of("gradeMax", "7b"), 820, List.of(), "Testville", "Testshire", "TT", null),
                new ConditionsExport.Crag("t-2", "Forecastless Test Wall", 45.1, 9.1, null, "https://example.org/2",
                        null, null, null, null, null, null, null),
                new ConditionsExport.Crag("t-3", "Shady Test Wall", 45.2, 9.2, Aspect.N, "https://example.org/3",
                        null, 1400, null, null, null, null, null)));
    }

    private static SiteData site() {
        return new SiteData(threeCrags(), AvalancheRegions.none(), "http://127.0.0.1:1", "https://x.test/cragcast", JUNE_2);
    }

    @Test
    void theSameJsonAsTheStaticExportForTheCragsItHasForecastsFor() {
        SiteData site = site();
        HourlyForecast f1 = dry(), f3 = dry();
        Map<String, HourlyForecast> forecasts = new LinkedHashMap<>();
        forecasts.put("t-3", f3);
        forecasts.put("t-1", f1);
        forecasts.put("not-in-the-file", dry());

        site.on(new ForecastsRefreshed(forecasts, Instant.parse("2026-06-02T08:07:00Z")));

        var all = threeCrags();
        var expected = ConditionsExport.build(new ConditionsExport.CragsFile(all.region(), all.source(), all.licence(),
                all.elevationSource(), List.of(all.crags().get(0), all.crags().get(2))), List.of(f1, f3), DAYS,
                Instant.parse("2026-06-02T08:07:00Z"), AvalancheRegions.none(), Map.of());
        JsonMapper json = JsonMapper.builder().build();
        assertThat(site.latest().conditions()).isEqualTo(json.writeValueAsString(expected));
        assertThat(site.latest().api()).containsOnlyKeys("crags.json", "crags/t-1.json", "crags/t-3.json");
        assertThat(site.latest().pages()).containsOnlyKeys("c/t-1.html", "c/t-3.html");
        assertThat(site.latest().pages().get("c/t-1.html")).contains("https://x.test/cragcast/");
    }

    @Test
    void theRealCragsAndRegionsFromTheJarAndNoBulletinOutOfSeason() throws Exception {
        try (var http = new FakeHttp(uri -> Answer.NOT_FOUND)) {
            SiteData site = new SiteData(http.url(), "https://x.test/cragcast/", JUNE_2);
            String first = JsonMapper.builder().build().readTree(
                    new String(new org.springframework.core.io.ClassPathResource("data/crags.json").getInputStream().readAllBytes()))
                    .get("crags").get(0).get("id").asString();

            site.publish(Map.of(first, dry()), Instant.EPOCH);

            assertThat(site.latest().api()).containsKeys("crags.json", "crags/" + first + ".json");
            assertThat(site.latest().conditions()).doesNotContain("\"avalanche\"");
            // The regions shipped in the jar: one bulletin asked for each of the three days.
            assertThat(http.asked()).extracting(URI::getPath).hasSize(3)
                    .allMatch(p -> p.endsWith("-IT-25.json"));
        }
    }

    @Test
    void servedAtThePagesPathsAnd503BeforeTheFirstRefresh() throws Exception {
        SiteData site = site();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SiteController(site)).build();

        mvc.perform(get("/data/conditions.json")).andExpect(status().isServiceUnavailable());

        site.publish(Map.of("t-1", dry()), Instant.parse("2026-06-02T08:07:00Z"));
        mvc.perform(get("/data/conditions.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(jsonPath("$.generatedAt").value("2026-06-02T08:07:00Z"))
                .andExpect(jsonPath("$.crags[0].id").value("t-1"))
                .andExpect(jsonPath("$.crags[0].days['2026-06-03'].hours.length()").value(16));
        mvc.perform(get("/api/v1/crags.json")).andExpect(status().isOk()).andExpect(jsonPath("$.meta.version").value(1));
        mvc.perform(get("/api/v1/crags/t-1.json")).andExpect(status().isOk()).andExpect(jsonPath("$.crag.name").value("Sunny Test Wall"));
        mvc.perform(get("/api/v1/crags/t-3.json")).andExpect(status().isNotFound());
        mvc.perform(get("/c/t-1.html")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sunny Test Wall")));
        mvc.perform(get("/c/nowhere.html")).andExpect(status().isNotFound());
    }
}
