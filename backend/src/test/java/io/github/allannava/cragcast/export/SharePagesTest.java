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

    /** CG-119: a made-up via ferrata, "no" tomorrow for a thunderstorm. */
    private static FerrateExport.FerrataOut ferrata(String id, Integer scaleMin, Integer scaleMax) {
        Map<String, ConditionsExport.DayOut> days = new LinkedHashMap<>();
        Weather w = new Weather(0.0, 0.0, 11.6, 19.8);
        days.put("2026-10-15", new ConditionsExport.DayOut(Verdict.GO, List.of(), w, List.of(), null));
        days.put("2026-10-16", new ConditionsExport.DayOut(Verdict.NO_GO, List.of("thunderstorm forecast"), w, List.of(), null));
        return new FerrateExport.FerrataOut(id, "Ferrata Test", 45.9, 9.4, "https://example.org/w", List.of(),
                new FerrateExport.Point(45.9, 9.4, 900), new FerrateExport.Point(45.91, 9.41, 1300), 400, scaleMin, scaleMax,
                List.of(), days);
    }

    @Test
    void aViaFerrataHasItsPageOpeningOnTheViaFerratasAndSaysToCheckOnTheSpot() {
        var c = crag("Test Wall", Verdict.GO, List.of());
        var out = new ConditionsExport.Output("2026-10-15T10:00:00Z", "Europe/Rome", List.of("2026-10-15", "2026-10-16"),
                "", "", "", "", List.of(c), List.of(ferrata("osm-way-10", 2, 3), ferrata("osm-node-1", null, null)));
        Map<String, String> pages = SharePages.pages(out, "https://example.org/cc/");

        assertThat(pages).containsOnlyKeys("c/osm-node-1.html", "c/osm-way-10.html");
        assertThat(pages.get("c/osm-node-1.html")).contains("Test Wall");             // a crag keeps its path
        String html = pages.get("c/osm-way-10.html");
        assertThat(html).contains("<meta property=\"og:title\" content=\"Via ferrata Ferrata Test — venerdì 16 ottobre: no\">");
        assertThat(html).contains("content=\"Venerdì 16 ottobre: no (temporale previsto). In cima: max 12 °C, pioggia 0,0 mm, "
                + "vento 20 km/h. Difficoltà 2–3 (scala 0–6), cima circa 1300 m. Condizioni da verificare sempre sul posto.\"");
        assertThat(html).contains("<link rel=\"canonical\" href=\"https://example.org/cc/?k=ferrate&amp;crag=osm-way-10\">");
        assertThat(html).contains("url=../?k=ferrate&amp;crag=osm-way-10");
        assertThat(html).contains("location.replace(\"../?k=ferrate&crag=osm-way-10\" + location.hash)");
    }

    @Test
    void aViaFerrataWithoutAMappedDifficultyOrTopSaysSo() {
        var f = ferrata("osm-way-10", null, null);
        String text = SharePages.description(new FerrateExport.FerrataOut(f.id(), f.name(), f.latitude(), f.longitude(),
                f.sourceUrl(), f.pieces(), f.start(), new FerrateExport.Point(45.91, 9.41, null), f.lengthM(), null, null,
                List.of(), f.days()), f.days().get("2026-10-15"), "Giovedì 15 ottobre");
        assertThat(text).isEqualTo("Giovedì 15 ottobre: si va. In cima: max 12 °C, pioggia 0,0 mm, vento 20 km/h. "
                + "Difficoltà non mappata. Condizioni da verificare sempre sul posto.");
        assertThat(SharePages.description(ferrata("x", 3, 3), f.days().get("2026-10-15"), "G")).contains("Difficoltà 3 (scala 0–6)");
    }
}
