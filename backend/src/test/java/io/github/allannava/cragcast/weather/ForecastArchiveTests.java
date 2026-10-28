package io.github.allannava.cragcast.weather;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Against PostgreSQL and the V8 migration; a made-up crag. */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class ForecastArchiveTests {

    private static final Instant SIX = Instant.parse("2026-10-27T17:07:00Z");
    private static final Instant SEVEN = Instant.parse("2026-10-27T18:07:00Z");
    private static final LocalDate OCT_28 = LocalDate.of(2026, 10, 28);

    @Autowired
    ForecastArchive archive;

    @Autowired
    CragRepository crags;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEventPublisher events;

    long cragId;

    @BeforeEach
    void oneCrag() {
        jdbc.update("DELETE FROM crag_forecast");
        cragId = crags.findBySlug("osm-node-999999998")
                .orElseGet(() -> crags.save(new Crag("osm-node-999999998", "Test Wall", null, 45.0, 9.0, null,
                        "https://example.org/t")))
                .getId();
    }

    private ForecastArchive.Row row(Instant at, Verdict verdict) {
        return new ForecastArchive.Row(cragId, OCT_28, at, verdict, "[]", new Weather(0, 0, 15, 10), "{}");
    }

    @Test
    void eachRefreshAppendsAndAnEarlierHourStaysAsItWas() {
        archive.append(List.of(row(SIX, Verdict.GO)));
        archive.append(List.of(row(SEVEN, Verdict.MAYBE)));

        assertThat(jdbc.queryForList(
                "SELECT verdict FROM crag_forecast WHERE crag_id = ? AND day = ? ORDER BY fetched_at",
                String.class, cragId, OCT_28)).containsExactly("GO", "MAYBE");
    }

    @Test
    void theSameRefreshAppendedTwiceIsKeptOnce() {
        archive.append(List.of(row(SIX, Verdict.GO)));
        archive.append(List.of(row(SIX, Verdict.NO_GO)));

        assertThat(jdbc.queryForList("SELECT verdict FROM crag_forecast", String.class)).containsExactly("GO");
    }

    @Test
    void aFailedAppendWritesNothingAndThrowsNothingAtTheEvent() {
        var bad = new ForecastArchive.Row(-1, OCT_28, SIX, Verdict.GO, "[]", new Weather(0, 0, 15, 10), "{}");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> archive.append(List.of(row(SIX, Verdict.GO), bad))))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM crag_forecast", Integer.class)).isZero();

        // A forecast list too short for the day fails the scoring; the event still returns.
        var empty = new HourlyForecast(List.of(), List.of(), List.of(), List.of());
        events.publishEvent(new ForecastsRefreshed(Map.of("osm-node-999999998", empty), SIX));
    }
}
