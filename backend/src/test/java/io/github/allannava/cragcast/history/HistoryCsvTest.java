package io.github.allannava.cragcast.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Made-up crags and numbers, in the history branch's format (history/README.md). */
class HistoryCsvTest {

    static final String HEADER = "date,crag_id,crag_name,province,verdict,rain_last48h_mm,rain_day_mm,"
            + "max_temp_c,max_wind_kmh,reasons,generated_at\n";

    @Test
    void readsTheColumnsByName() {
        var p = HistoryCsv.parse(HEADER
                + "2026-10-15,test-wall,Test Wall,SO,GO,0.0,0.0,17.6,11.4,dry,2026-10-15T10:38:40.039332013Z\n");

        assertThat(p.skipped()).isZero();
        assertThat(p.rows()).containsExactly(new HistoryRow(LocalDate.of(2026, 10, 15), "test-wall", "Test Wall",
                "SO", "GO", 0.0, 0.0, 17.6, 11.4, "dry", Instant.parse("2026-10-15T10:38:40.039332013Z")));
    }

    @Test
    void anotherOrderAndAColumnAddedAtTheEndChangeNothing() {
        var p = HistoryCsv.parse("crag_id,date,crag_name,province,verdict,rain_last48h_mm,rain_day_mm,"
                + "max_temp_c,max_wind_kmh,reasons,generated_at,new_column\r\n"
                + "test-wall,2026-10-15,Test Wall,SO,NO_GO,4.2,1.0,9.0,30.0,wet,2026-10-15T10:00:00Z,x\r\n");

        assertThat(p.rows()).singleElement().satisfies(r -> {
            assertThat(r.cragSlug()).isEqualTo("test-wall");
            assertThat(r.verdict()).isEqualTo("NO_GO");
            assertThat(r.rainLast48hMm()).isEqualTo(4.2);
        });
    }

    @Test
    void quotedFieldsKeepTheirCommasAndQuotes() {
        var p = HistoryCsv.parse(HEADER
                + "2026-10-15,test-wall,\"Wall, the \"\"Test\"\"\",,MAYBE,1,0,12,20,\"rain, then sun; windy\","
                + "2026-10-15T10:00:00Z");

        assertThat(p.rows()).singleElement().satisfies(r -> {
            assertThat(r.cragName()).isEqualTo("Wall, the \"Test\"");
            assertThat(r.province()).isNull();
            assertThat(r.reasons()).isEqualTo("rain, then sun; windy");
        });
    }

    @Test
    void aMalformedRowIsSkippedNeverGuessed() {
        var p = HistoryCsv.parse(HEADER
                + "2026-10-15,test-wall,Test Wall,SO,GO,0,0,17,11,dry,2026-10-15T10:00:00Z\n"
                + "not-a-date,test-wall,Test Wall,SO,GO,0,0,17,11,dry,2026-10-15T10:00:00Z\n"
                + "2026-10-15,,No Id,SO,GO,0,0,17,11,dry,2026-10-15T10:00:00Z\n"
                + "2026-10-15,short-row,Short\n"
                + "\n");

        assertThat(p.rows()).hasSize(1);
        assertThat(p.skipped()).isEqualTo(3);
    }

    @Test
    void aFileWithoutAColumnReadsNothing() {
        var p = HistoryCsv.parse("date,crag_id\n2026-10-15,test-wall\n");

        assertThat(p.rows()).isEmpty();
        assertThat(p.skipped()).isEqualTo(1);
    }

    @Test
    void anEmptyFileHasNoRows() {
        assertThat(HistoryCsv.parse("").rows()).isEmpty();
    }
}
