package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.avalanche.Avalanche;
import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.ferrata.FerrataClient;
import io.github.allannava.cragcast.ferrata.FerrataForecast;
import io.github.allannava.cragcast.ferrata.FerrataReducer;
import io.github.allannava.cragcast.ferrata.FerrataScorer;
import io.github.allannava.cragcast.ferrata.FerrataWeather;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CG-114: the via ferratas of data/ferrate.json (ADR 0023) scored for the page, next to the crags
 * in data/conditions.json under {@code ferrate} — with {@link FerrataScorer} on the weather at
 * each top (ADR 0024), never the crags' scorer. The static API v1 does not list them yet (CG-115).
 */
public final class FerrateExport {

    private FerrateExport() {
    }

    record FerrateFile(String region, String source, String licence, String elevationSource, List<Ferrata> ferrate) {
    }

    record Point(double latitude, double longitude, Integer elevation) {
    }

    /** One via ferrata as tools/osm_ferrate.py writes it. */
    record Ferrata(String id, String name, String sourceUrl, List<String> pieces, Point start, Point top,
                   Integer lengthM, Integer scaleMin, Integer scaleMax, List<String> scaleDropped) {
    }

    /**
     * One via ferrata on the page: placed at its start, where one goes, and scored at its top.
     * {@code days} has the crags' shape, so the page's list, map and panel read both; its weather
     * and hours are the top's.
     */
    record FerrataOut(String id, String name, double latitude, double longitude, String sourceUrl, List<String> pieces,
                      Point start, Point top, Integer lengthM, Integer scaleMin, Integer scaleMax,
                      List<String> scaleDropped, Map<String, ConditionsExport.DayOut> days) {
    }

    /** The tops, in the file's order, for one Open-Meteo request per {@code batch} of them. */
    static List<FerrataForecast> fetch(FerrataClient client, FerrateFile file, int batch) {
        List<FerrataForecast> out = new ArrayList<>();
        List<Ferrata> all = file.ferrate();
        for (int i = 0; i < all.size(); i += batch) {
            out.addAll(client.hourly(all.subList(i, Math.min(i + batch, all.size())).stream().map(FerrateExport::top).toList()));
        }
        return out;
    }

    /** Where a via ferrata's forecast is asked: its top, at the DEM's elevation. */
    static FerrataClient.Top top(Ferrata f) {
        // NaN asks Open-Meteo for its grid's own height: no correction without a DEM value.
        return new FerrataClient.Top(f.top().latitude(), f.top().longitude(),
                f.top().elevation() == null ? Double.NaN : f.top().elevation());
    }

    /** The pure part: via ferratas and their forecasts in, the page's entries out. */
    static List<FerrataOut> build(FerrateFile file, List<FerrataForecast> forecasts, List<LocalDate> days,
                                  AvalancheRegions regions, Map<LocalDate, Map<String, Avalanche>> bulletins) {
        if (forecasts.size() != file.ferrate().size()) {
            throw new IllegalArgumentException(forecasts.size() + " forecasts for " + file.ferrate().size() + " via ferratas");
        }
        FerrataScorer scorer = new FerrataScorer();
        List<FerrataOut> out = new ArrayList<>();
        for (int i = 0; i < forecasts.size(); i++) {
            Ferrata f = file.ferrate().get(i);
            FerrataForecast forecast = forecasts.get(i);
            // CG-11: the bulletin of the zone where it starts; it informs and never scores.
            Optional<String> region = regions.regionOf(f.start().latitude(), f.start().longitude());
            Map<String, ConditionsExport.DayOut> byDay = new LinkedHashMap<>();
            for (LocalDate day : days) {
                FerrataWeather w = FerrataReducer.reduce(forecast, day);
                FerrataScorer.Result r = scorer.score(w);
                Avalanche avalanche = region.map(id -> bulletins.getOrDefault(day, Map.of()).get(id)).orElse(null);
                byDay.put(day.toString(), new ConditionsExport.DayOut(r.verdict(), r.reasons(),
                        new Weather(w.rainLast48hMm(), w.rainDuringDayMm(), w.maxTemperatureC(), w.maxWindKmh()),
                        hours(forecast, day), avalanche));
            }
            out.add(new FerrataOut(f.id(), f.name(), f.start().latitude(), f.start().longitude(), f.sourceUrl(),
                    f.pieces() == null ? List.of() : f.pieces(), f.start(), f.top(), f.lengthM(), f.scaleMin(),
                    f.scaleMax(), f.scaleDropped() == null ? List.of() : f.scaleDropped(), byDay));
        }
        return out;
    }

    /** The day's hours at the top, from 06:00 to 21:00, as the crags' chart reads them. */
    static List<ConditionsExport.HourOut> hours(FerrataForecast f, LocalDate day) {
        List<ConditionsExport.HourOut> out = new ArrayList<>();
        for (int k = 0; k < f.time().size(); k++) {
            var t = f.time().get(k);
            if (t.toLocalDate().equals(day) && t.getHour() >= ConditionsExport.FIRST_HOUR
                    && t.getHour() <= ConditionsExport.LAST_HOUR) {
                out.add(new ConditionsExport.HourOut(t.getHour(), f.precipitation().get(k), f.temperature().get(k),
                        f.windSpeed().get(k)));
            }
        }
        return out;
    }
}
