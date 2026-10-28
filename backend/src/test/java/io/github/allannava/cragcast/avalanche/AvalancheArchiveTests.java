package io.github.allannava.cragcast.avalanche;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Against PostgreSQL and the V9 migration, the bulletin from a fake server (a recorded issue). */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false",
        "cragcast.avalanche.archive.enabled=false",
        "cragcast.observations.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class AvalancheArchiveTests {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Clock MARCH_1 = Clock.fixed(LocalDateTime.of(2026, 3, 1, 9, 30).atZone(ROME).toInstant(), ROME);
    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @BeforeEach
    void empty() {
        jdbc.update("DELETE FROM avalanche_bulletin");
    }

    private static String fixture() throws IOException {
        try (InputStream in = AvalancheArchiveTests.class.getResourceAsStream("/avalanche/IT-25-2026-03-01.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private AvalancheArchive archive(FakeHttp http) {
        return new AvalancheArchive(http.url(), jdbc, tx, MARCH_1);
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM avalanche_bulletin", Integer.class);
    }

    @Test
    void todaysBulletinKeptByRegionFactsOnly() throws Exception {
        String body = fixture();
        int regions = AvalancheBulletins.parse(body, tools.jackson.databind.json.JsonMapper.builder().build()).size();
        try (var http = new FakeHttp(uri -> Answer.ok(body))) {
            archive(http).atStartup();

            assertThat(http.asked()).extracting(URI::getPath).containsExactly("/2026-03-01/2026-03-01-IT-25.json");
        }
        assertThat(rows()).isEqualTo(regions).isPositive();
        var any = jdbc.queryForMap("SELECT * FROM avalanche_bulletin ORDER BY region LIMIT 1");
        assertThat(any.get("day").toString()).isEqualTo("2026-03-01");
        assertThat((String) any.get("ratings")).startsWith("[{\"level\":");
        assertThat((String) any.get("problems")).startsWith("[");
        assertThat(any.get("bulletin")).isEqualTo(AvalancheBulletins.BULLETIN_PAGE);
    }

    @Test
    void noBulletinKeepsWhatIsThere() throws Exception {
        String body = fixture();
        try (var http = new FakeHttp(uri -> Answer.ok(body))) {
            archive(http).daily();
        }
        int before = rows();
        try (var http = new FakeHttp(uri -> Answer.NOT_FOUND)) {
            archive(http).daily();
        }
        assertThat(rows()).isEqualTo(before).isPositive();
    }

    @Test
    void aLaterIssueReplacesTheDayWhole() throws Exception {
        String body = fixture();
        try (var http = new FakeHttp(uri -> Answer.ok(body))) {
            var archive = archive(http);
            archive.daily();
            var later = new Avalanche("IT-25-TEST-01", "Test", 2, List.of(new Avalanche.Rating(2, null, null, null)),
                    List.of("wind_slab"), AvalancheBulletins.BULLETIN_PAGE);
            archive.keep(DAY, Map.of(later.region(), later));
        }
        assertThat(jdbc.queryForList("SELECT region || ' ' || level FROM avalanche_bulletin", String.class))
                .containsExactly("IT-25-TEST-01 2");
    }
}
