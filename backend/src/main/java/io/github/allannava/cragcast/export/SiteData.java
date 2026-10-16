package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.avalanche.Avalanche;
import io.github.allannava.cragcast.avalanche.AvalancheBulletins;
import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.weather.ForecastsRefreshed;
import io.github.allannava.cragcast.weather.HourlyForecast;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-110: the static page's data, made by the Spring service from the forecasts its hourly
 * refresh has just fetched — no second call to Open-Meteo. The same {@link ConditionsExport#build}
 * as GitHub Pages, so the verdict is one code path: data/conditions.json, the static API v1 and
 * the share pages, held in memory and served by {@link SiteController} to the Pi's page.
 */
@Component
public class SiteData {

    /** What one refresh produced: every file the page and its API read, as text. */
    record Snapshot(Instant generatedAt, String conditions, Map<String, String> api, Map<String, String> pages) {
    }

    private final JsonMapper json = JsonMapper.builder().build();
    private final ConditionsExport.CragsFile crags;
    private final AvalancheRegions regions;
    private final HttpClient http;
    private final String avalancheUrl;
    private final String siteUrl;
    private final Clock clock;
    private volatile Snapshot latest;

    @Autowired
    SiteData(@Value("${cragcast.avalanche.base-url}") String avalancheUrl,
             @Value("${cragcast.site.url}") String siteUrl, Clock clock) throws IOException {
        this(readCrags(), readRegions(), avalancheUrl, siteUrl, clock);
    }

    SiteData(ConditionsExport.CragsFile crags, AvalancheRegions regions, String avalancheUrl, String siteUrl,
             Clock clock) {
        this.crags = crags;
        this.regions = regions;
        this.avalancheUrl = avalancheUrl;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl : siteUrl + "/";
        this.clock = clock;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    private static ConditionsExport.CragsFile readCrags() throws IOException {
        try (InputStream in = new ClassPathResource("data/crags.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(in, ConditionsExport.CragsFile.class);
        }
    }

    /** CG-11: the micro-regions ship in the jar next to the crags; without them, no bulletin. */
    private static AvalancheRegions readRegions() throws IOException {
        var file = new ClassPathResource("data/avalanche-regions.geojson");
        if (!file.exists()) {
            return AvalancheRegions.none();
        }
        try (InputStream in = file.getInputStream()) {
            return AvalancheRegions.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonMapper.builder().build());
        }
    }

    @EventListener
    void on(ForecastsRefreshed e) {
        publish(e.forecasts(), e.fetchedAt());
    }

    /**
     * A new snapshot from the refresh's forecasts, by crag slug (the OSM id of data/crags.json).
     * Crags without a forecast are left out, in the file's order, as the export lists them.
     */
    public void publish(Map<String, HourlyForecast> forecasts, Instant now) {
        List<ConditionsExport.Crag> scored = new ArrayList<>();
        List<HourlyForecast> theirs = new ArrayList<>();
        for (ConditionsExport.Crag c : crags.crags()) {
            HourlyForecast f = forecasts.get(c.id());
            if (f != null) {
                scored.add(c);
                theirs.add(f);
            }
        }
        LocalDate today = LocalDate.now(clock.withZone(ConditionsExport.ZONE));
        List<LocalDate> days = List.of(today, today.plusDays(1), today.plusDays(2));
        Map<LocalDate, Map<String, Avalanche>> bulletins = new LinkedHashMap<>();
        if (regions.size() > 0) {
            for (LocalDate day : days) {
                bulletins.put(day, AvalancheBulletins.fetch(http, avalancheUrl, day, json));
            }
        }
        var file = new ConditionsExport.CragsFile(crags.region(), crags.source(), crags.licence(),
                crags.elevationSource(), scored);
        ConditionsExport.Output out = ConditionsExport.build(file, theirs, days, now, regions, bulletins);
        Map<String, String> api = new LinkedHashMap<>();
        ConditionsExport.api(out).forEach((path, doc) -> api.put(path, json.writeValueAsString(doc)));
        latest = new Snapshot(now, json.writeValueAsString(out), api, SharePages.pages(out, siteUrl));
    }

    /** The last snapshot, or null before the first refresh has finished. */
    Snapshot latest() {
        return latest;
    }
}
