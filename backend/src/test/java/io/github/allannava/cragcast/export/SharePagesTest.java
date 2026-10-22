package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.conditions.Weather;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Made-up crags, not real places. */
class SharePagesTest {

    private static ConditionsExport.CragOut crag(String name, Verdict tomorrow, List<String> reasons) {
        Map<String, ConditionsExport.DayOut> days = new LinkedHashMap<>();
        Weather w = new Weather(0.0, 3.4, 18.6, 12.2);
        days.put("2026-10-15", new ConditionsExport.DayOut(Verdict.GO, List.of(), w, List.of(), null));
        days.put("2026-10-16", new ConditionsExport.DayOut(tomorrow, reasons, w, List.of(), null));
        return new ConditionsExport.CragOut("osm-node-1", name, 45.8, 9.4, null, "https://example.org/1", Map.of(), 482,
                List.of(), "Testville", "Testshire", "TT", days, List.of(), List.of());
    }

    private static ConditionsExport.Output out(ConditionsExport.CragOut c) {
        return new ConditionsExport.Output("2026-10-15T10:00:00Z", "Europe/Rome", List.of("2026-10-15", "2026-10-16"),
                "", "", "", "", List.of(c));
    }

    @Test
    void writesTomorrowsVerdictAndWeatherAsADate() {
        var c = crag("Test Wall", Verdict.NO_GO, List.of("rain forecast during the day", "wall aspect not mapped: x"));
        String html = SharePages.pages(out(c), "https://example.org/cc/").get("c/osm-node-1.html");

        assertThat(html).contains("<meta property=\"og:title\" content=\"Test Wall — venerdì 16 ottobre: no\">");
        assertThat(html).contains("content=\"Venerdì 16 ottobre: no (pioggia prevista durante il giorno). "
                + "Max 19 °C, pioggia 3,4 mm, vento 12 km/h. Testville (TT), quota circa 482 m.\"");
        assertThat(html).contains("<meta property=\"og:image\" content=\"https://example.org/cc/og.png\">");
        assertThat(html).contains("<meta property=\"og:url\" content=\"https://example.org/cc/c/osm-node-1.html\">");
        assertThat(html).contains("<link rel=\"canonical\" href=\"https://example.org/cc/?crag=osm-node-1\">");
        assertThat(html).contains("url=../?crag=osm-node-1");
        assertThat(html).doesNotContain("domani");
    }

    @Test
    void escapesNamesFromOsm() {
        var c = crag("Rock <b>\"&\"</b>", Verdict.GO, List.of());
        String html = SharePages.page(c, "2026-10-16", "https://example.org/");
        assertThat(html).contains("Rock &lt;b&gt;&quot;&amp;&quot;&lt;/b&gt; — venerdì 16 ottobre: si va");
        assertThat(html).doesNotContain("<b>");
    }
}
