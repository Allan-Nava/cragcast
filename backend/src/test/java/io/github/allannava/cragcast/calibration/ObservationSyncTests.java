package io.github.allannava.cragcast.calibration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/** Against PostgreSQL and the V10 migration, the CSV from a fake main; made-up crags and observers. */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false",
        "cragcast.avalanche.archive.enabled=false", "cragcast.observations.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class ObservationSyncTests {

    private static final String TWO = ObservationSync.HEADER + "\n"
            + "# a comment, as in the real file\n"
            + "2026-10-20,osm-node-1,dry,Test Climber,warm and dry\n"
            + "2026-10-21,Test Wall,wet,Other Climber\n";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @BeforeEach
    void empty() {
        jdbc.update("DELETE FROM observation");
    }

    private ObservationSync sync(FakeHttp http) {
        return new ObservationSync(RestClient.builder().baseUrl(http.url()), jdbc, tx);
    }

    private java.util.List<String> rows() {
        return jdbc.queryForList("SELECT day || ' ' || crag || ' ' || rock || ' ' || observer || ' [' || note || ']'"
                + " FROM observation ORDER BY day", String.class);
    }

    @Test
    void theCsvBecomesTheTable() throws Exception {
        try (var http = new FakeHttp(uri -> Answer.ok(TWO))) {
            sync(http).atStartup();

            assertThat(http.asked()).extracting(URI::getPath).containsExactly("/data/observations.csv");
        }
        assertThat(rows()).containsExactly("2026-10-20 osm-node-1 DRY Test Climber [warm and dry]",
                "2026-10-21 Test Wall WET Other Climber []");
    }

    @Test
    void aRowRemovedFromTheCsvLeavesTheTable() throws Exception {
        try (var http = new FakeHttp(uri -> Answer.ok(TWO))) {
            sync(http).atStartup();
        }
        try (var http = new FakeHttp(uri -> Answer.ok(ObservationSync.HEADER + "\n"))) {
            sync(http).nightly();
        }
        assertThat(rows()).isEmpty();
    }

    @Test
    void aFailedReadAMissingHeaderOrABadRowKeepsTheRows() throws Exception {
        try (var http = new FakeHttp(uri -> Answer.ok(TWO))) {
            sync(http).atStartup();
        }
        for (Answer bad : new Answer[] {Answer.NOT_FOUND, Answer.ok(""), Answer.ok("<html>busy</html>"),
                Answer.ok(ObservationSync.HEADER + "\n2026-10-22,Test Wall,muddy,Test Climber\n")}) {
            try (var http = new FakeHttp(uri -> bad)) {
                sync(http).nightly();
            }
        }
        assertThat(rows()).hasSize(2);
    }
}
