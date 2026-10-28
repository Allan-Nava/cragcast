package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.avalanche.Avalanche;
import io.github.allannava.cragcast.avalanche.AvalancheBulletins;
import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.ferrata.FerrataForecast;
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
    private final OsmClimbing climbing;
    /** CG-115: the via ferratas, scored from the refresh's forecasts at their tops. */
    private final FerrateExport.FerrateFile ferrate;
    private final HttpClient http;
    private final String avalancheUrl;
    private final String siteUrl;
    private final Clock clock;
    private volatile Snapshot latest;

    @Autowired
    SiteData(@Value("${cragcast.avalanche.base-url}") String avalancheUrl,
             @Value("${cragcast.site.url}") String siteUrl, Clock clock, FerrateData ferrate) throws IOException {
        this(readCrags(), readRegions(), readClimbing(), ferrate.file(), avalancheUrl, siteUrl, clock);
    }

    SiteData(String avalancheUrl, String siteUrl, Clock clock) throws IOException {
        this(readCrags(), readRegions(), readClimbing(), FerrateData.none(), avalancheUrl, siteUrl, clock);
    }

    SiteData(ConditionsExport.CragsFile crags, AvalancheRegions regions, String avalancheUrl, String siteUrl,
             Clock clock) {
        this(crags, regions, OsmClimbing.none(), avalancheUrl, siteUrl, clock);
    }

    SiteData(ConditionsExport.CragsFile crags, AvalancheRegions regions, OsmClimbing climbing, String avalancheUrl,
             String siteUrl, Clock clock) {
        this(crags, regions, climbing, FerrateData.none(), avalancheUrl, siteUrl, clock);
    }

    SiteData(ConditionsExport.CragsFile crags, AvalancheRegions regions, OsmClimbing climbing,
             FerrateExport.FerrateFile ferrate, String avalancheUrl, String siteUrl, Clock clock) {
        this.ferrate = ferrate;
        this.crags = crags;
        this.regions = regions;
        this.climbing = climbing;
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

    /** CG-117: the sectors and routes ship in the jar too; without the file, none. */
    private static OsmClimbing readClimbing() throws IOException {
        var file = new ClassPathResource("data/osm-climbing.json");
        if (!file.exists()) {
            return OsmClimbing.none();
        }
        try (InputStream in = file.getInputStream()) {
            return OsmClimbing.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonMapper.builder().build());
        }
    }

    @EventListener
    void on(ForecastsRefreshed e) {
        publish(e.forecasts(), e.ferrate(), e.fetchedAt());
    }

    public void publish(Map<String, HourlyForecast> forecasts, Instant now) {
        publish(forecasts, Map.of(), now);
    }

    /**
     * A new snapshot from the refresh's forecasts, by crag slug (the OSM id of data/crags.json).
     * Crags without a forecast are left out, in the file's order, as the export lists them.
     */
    public void publish(Map<String, HourlyForecast> forecasts, Map<String, FerrataForecast> tops, Instant now) {
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
        ConditionsExport.Output out = ConditionsExport.build(file, theirs, days, now, regions, bulletins, climbing,
                ferrate(tops, days, bulletins));
        Map<String, String> api = new LinkedHashMap<>();
        ConditionsExport.api(out).forEach((path, doc) -> api.put(path, json.writeValueAsString(doc)));
        latest = new Snapshot(now, json.writeValueAsString(out), api, SharePages.pages(out, siteUrl));
    }

    /**
     * CG-115: the via ferratas with a forecast, in the file's order, scored as on GitHub Pages
     * (FerrateExport). A failure leaves them out, never the crags.
     */
    private List<FerrateExport.FerrataOut> ferrate(Map<String, FerrataForecast> tops, List<LocalDate> days,
                                                   Map<LocalDate, Map<String, Avalanche>> bulletins) {
        List<FerrateExport.Ferrata> scored = new ArrayList<>();
        List<FerrataForecast> theirs = new ArrayList<>();
        for (FerrateExport.Ferrata f : ferrate.ferrate()) {
            FerrataForecast forecast = tops.get(f.id());
            if (forecast != null) {
                scored.add(f);
                theirs.add(forecast);
            }
        }
        try {
            return FerrateExport.build(new FerrateExport.FerrateFile(ferrate.region(), ferrate.source(), ferrate.licence(),
                    ferrate.elevationSource(), scored), theirs, days, regions, bulletins);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** The last snapshot, or null before the first refresh has finished. */
    Snapshot latest() {
        return latest;
    }
}
