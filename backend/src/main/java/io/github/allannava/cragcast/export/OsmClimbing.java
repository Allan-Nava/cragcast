package io.github.allannava.cragcast.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.osm.FrenchGrade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-117: what data/osm-climbing.json says of each crag's sectors (CG-104) and single routes
 * (CG-105), for the page and the API: the export has no database, so it reads the file as
 * the backend's importers do. Without the file, no crag has sectors or routes.
 */
final class OsmClimbing {

    private static final OsmClimbing NONE = new OsmClimbing(Map.of(), Map.of());

    private final Map<String, Entry> objects;
    private final Map<String, List<Entry>> routesByCrag;

    private OsmClimbing(Map<String, Entry> objects, Map<String, List<Entry>> routesByCrag) {
        this.objects = objects;
        this.routesByCrag = routesByCrag;
    }

    static OsmClimbing none() {
        return NONE;
    }

    static OsmClimbing parse(String text, JsonMapper json) {
        List<Entry> all = Objects.requireNonNullElse(json.readValue(text, File.class).objects(), List.of());
        Map<String, Entry> objects = new LinkedHashMap<>();
        Map<String, List<Entry>> routes = new LinkedHashMap<>();
        for (Entry e : all) {
            objects.put(e.id(), e);
            if (e.crag() != null && "route".equals(e.category())) {
                routes.computeIfAbsent(e.crag(), k -> new ArrayList<>()).add(e);
            }
        }
        return new OsmClimbing(objects, routes);
    }

    /** The crag's sectors, in the order crags.json lists them; an id not in the file is skipped. */
    List<ConditionsExport.SectorOut> sectors(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(objects::get).filter(Objects::nonNull)
                .map(e -> new ConditionsExport.SectorOut(e.id(), e.name(), aspect(e.aspect()),
                        text(e, "gradeMin"), text(e, "gradeMax")))
                .toList();
    }

    /** The crag's single routes, easiest first, the ungraded last, then by name. */
    List<ConditionsExport.RouteOut> routes(String cragId) {
        return routesByCrag.getOrDefault(cragId, List.of()).stream()
                .map(e -> new ConditionsExport.RouteOut(e.id(), e.name(), text(e, "grade"), number(e, "length"),
                        integer(e, "bolts"), integer(e, "pitches"), e.sector()))
                .sorted(Comparator.comparing((ConditionsExport.RouteOut r) -> FrenchGrade.rank(r.grade()),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ConditionsExport.RouteOut::name, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ConditionsExport.RouteOut::id))
                .toList();
    }

    private static Aspect aspect(String a) {
        try {
            return a == null ? null : Aspect.valueOf(a);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(Entry e, String key) {
        Object v = e.details() == null ? null : e.details().get(key);
        return v == null ? null : v.toString();
    }

    private static Double number(Entry e, String key) {
        return e.details() != null && e.details().get(key) instanceof Number n ? n.doubleValue() : null;
    }

    private static Integer integer(Entry e, String key) {
        return e.details() != null && e.details().get(key) instanceof Number n ? n.intValue() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record File(List<Entry> objects) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, String category, String name, String aspect, Map<String, Object> details,
                 String crag, String sector) {
    }
}
