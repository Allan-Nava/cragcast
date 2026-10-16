package io.github.allannava.cragcast.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

/** The sync over real HTTP against a fake history branch; made-up crags. */
class HistorySyncTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Clock DEC_3 = Clock.fixed(
            LocalDateTime.of(2026, 12, 3, 23, 50).atZone(ROME).toInstant(), ROME);

    private static String month(String day) {
        return HistoryCsvTest.HEADER + day + ",test-wall,Test Wall,SO,GO,0,0,17,11,dry,2026-10-15T10:00:00Z\n";
    }

    private static Answer branch(URI uri) {
        return switch (uri.getPath()) {
            case "/2026/10.csv" -> Answer.ok(month("2026-10-15"));
            case "/2026/12.csv" -> Answer.ok(month("2026-12-03"));
            default -> Answer.NOT_FOUND; // November: no file, as before history.yml reaches a month
        };
    }

    private static HistorySync sync(FakeHttp http, HistoryWriter writer) {
        return new HistorySync(RestClient.builder().baseUrl(http.url()), YearMonth.of(2026, 10), writer, DEC_3);
    }

    @Test
    void atStartupEveryMonthSinceTheFirstAMissingOneSkipped() throws Exception {
        HistoryWriter writer = mock(HistoryWriter.class);
        when(writer.replaceDays(anyList())).thenAnswer(i -> ((List<?>) i.getArgument(0)).size());
        try (var http = new FakeHttp(HistorySyncTest::branch)) {
            sync(http, writer).atStartup();

            assertThat(http.asked()).extracting(URI::getPath)
                    .containsExactly("/2026/10.csv", "/2026/11.csv", "/2026/12.csv");
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<HistoryRow>> rows = ArgumentCaptor.forClass(List.class);
            verify(writer, times(2)).replaceDays(rows.capture());
            assertThat(rows.getAllValues()).extracting(r -> r.getFirst().day().toString())
                    .containsExactly("2026-10-15", "2026-12-03");
        }
    }

    @Test
    void eachNightTheLastTwoMonths() throws Exception {
        HistoryWriter writer = mock(HistoryWriter.class);
        try (var http = new FakeHttp(HistorySyncTest::branch)) {
            sync(http, writer).nightly();

            assertThat(http.asked()).extracting(URI::getPath).containsExactly("/2026/11.csv", "/2026/12.csv");
        }
    }

    @Test
    void neverBeforeTheFirstMonth() throws Exception {
        HistoryWriter writer = mock(HistoryWriter.class);
        try (var http = new FakeHttp(HistorySyncTest::branch)) {
            assertThat(sync(http, writer).sync(YearMonth.of(2025, 1))).isZero();

            assertThat(http.asked()).first().extracting(URI::getPath).isEqualTo("/2026/10.csv");
        }
    }

    @Test
    void aFailingGitHubKeepsTheRowsThere() throws Exception {
        HistoryWriter writer = mock(HistoryWriter.class);
        try (var http = new FakeHttp(uri -> new Answer(500, "busy"))) {
            sync(http, writer).atStartup(); // logged, not thrown

            verify(writer, never()).replaceDays(anyList());
        }
    }
}
