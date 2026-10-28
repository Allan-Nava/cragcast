package io.github.allannava.cragcast.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Synthetic crags and forecasts, made up for the test; no database. */
class ForecastArchiveTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Instant JUNE_2_MORNING = LocalDateTime.of(2026, 6, 2, 10, 0).atZone(ROME).toInstant();

    /** Five days of hours from 2026-05-31, dry; rain in the afternoon of 06-03. */
    private static HourlyForecast forecast() {
        List<LocalDateTime> t = new ArrayList<>();
        List<Double> rain = new ArrayList<>(Collections.nCopies(120, 0.0));
        for (int i = 0; i < 120; i++) {
            t.add(LocalDateTime.of(2026, 5, 31, 0, 0).plusHours(i));
        }
        rain.set(72 + 15, 4.0);   // 06-03 15:00
        return new HourlyForecast(t, rain, new ArrayList<>(Collections.nCopies(120, 17.0)),
                new ArrayList<>(Collections.nCopies(120, 9.0)));
    }

    private static Crag crag(long id, Aspect aspect) {
        Crag c = mock(Crag.class);
        when(c.getId()).thenReturn(id);
        when(c.getSlug()).thenReturn("test-crag-" + id);
        when(c.getAspect()).thenReturn(aspect);
        return c;
    }

    private static ForecastArchive archive() {
        return new ForecastArchive(mock(CragRepository.class), null, null, Clock.system(ROME));
    }

    @Test
    void oneRowPerCragAndDayKeyedByTheRefreshTime() {
        List<Crag> two = List.of(crag(1, Aspect.S), crag(2, null));

        var rows = archive().rows(two, Map.of("test-crag-1", forecast(), "test-crag-2", forecast()), JUNE_2_MORNING);

        assertThat(rows).extracting(r -> r.cragId() + " " + r.day() + " " + r.verdict()).containsExactly(
                "1 2026-06-02 GO", "1 2026-06-03 NO_GO", "1 2026-06-04 GO",
                "2 2026-06-02 GO", "2 2026-06-03 NO_GO", "2 2026-06-04 GO");
        assertThat(rows).allSatisfy(r -> assertThat(r.fetchedAt()).isEqualTo(JUNE_2_MORNING));
        assertThat(rows.get(1).reasons()).isEqualTo("[\"rain forecast during the day\"]");
        assertThat(rows.get(3).reasons()).isEqualTo("[\"wall aspect not mapped: judged as a shaded wall\"]");
        assertThat(rows.get(1).weather().rainDuringDayMm()).isEqualTo(4.0);
    }

    @Test
    void aCragWithoutAForecastIsLeftOut() {
        var rows = archive().rows(List.of(crag(1, Aspect.S), crag(2, Aspect.S)),
                Map.of("test-crag-2", forecast()), JUNE_2_MORNING);

        assertThat(rows).extracting(ForecastArchive.Row::cragId).containsOnly(2L);
    }

    @Test
    void theHoursAreTheWholeDayAsParallelLists() {
        var hours = ForecastArchive.hours(forecast(), LocalDate.of(2026, 6, 3));

        assertThat(hours).containsOnlyKeys("hour", "rainMm", "temperatureC", "windKmh");
        assertThat(hours.get("hour")).hasSize(24).startsWith(0, 1).endsWith(23);
        assertThat(hours.get("rainMm").get(15)).isEqualTo(4.0);
        assertThat(hours.get("temperatureC")).containsOnly(17.0);
    }

    @Test
    void theViaFerratasTopForecastsArchivedPerDayScoredAtTheTop() {
        var rows = archive().ferrataRows(Map.of("osm-way-10", io.github.allannava.cragcast.export.SiteDataTestAccess.top(true)),
                JUNE_2_MORNING);
        assertThat(rows).extracting(r -> r.ferrataId() + " " + r.day() + " " + r.verdict())
                .containsExactly("osm-way-10 2026-06-02 GO", "osm-way-10 2026-06-03 NO_GO", "osm-way-10 2026-06-04 GO");
        assertThat(rows.get(1).reasons()).isEqualTo("[\"thunderstorm forecast\"]");
        assertThat(rows.get(1).weather()).contains("\"thunderstormHours\":1");
        assertThat(rows.get(1).hours()).contains("\"weatherCode\":[").contains("\"isDay\":[");
        assertThat(archive().ferrataRows(Map.of(), JUNE_2_MORNING)).isEmpty();
    }
}
