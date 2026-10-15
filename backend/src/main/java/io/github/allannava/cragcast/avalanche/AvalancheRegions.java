package io.github.allannava.cragcast.avalanche;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-11: Lombardy's avalanche micro-regions (data/avalanche-regions.geojson, from EAWS, CC0) and
 * the one a crag stands in. Low crags lie outside every micro-region: the bulletin does not
 * cover them, and they get no avalanche line.
 */
public final class AvalancheRegions {

    /** One micro-region: polygons, each an outer ring followed by its holes, as [lon, lat]. */
    private record Region(String id, List<List<double[][]>> polygons) {
    }

    private final List<Region> regions;

    private AvalancheRegions(List<Region> regions) {
        this.regions = regions;
    }

    public static AvalancheRegions none() {
        return new AvalancheRegions(List.of());
    }

    /** Reads a GeoJSON FeatureCollection of MultiPolygons with an {@code id} property. */
    public static AvalancheRegions parse(String geojson, JsonMapper json) {
        List<Region> regions = new ArrayList<>();
        for (JsonNode f : json.readTree(geojson).path("features")) {
            List<List<double[][]>> polygons = new ArrayList<>();
            for (JsonNode polygon : f.path("geometry").path("coordinates")) {
                List<double[][]> rings = new ArrayList<>();
                for (JsonNode ring : polygon) {
                    double[][] points = new double[ring.size()][];
                    for (int i = 0; i < ring.size(); i++) {
                        points[i] = new double[] {ring.get(i).get(0).asDouble(), ring.get(i).get(1).asDouble()};
                    }
                    rings.add(points);
                }
                polygons.add(rings);
            }
            regions.add(new Region(f.path("properties").path("id").asString(), polygons));
        }
        return new AvalancheRegions(regions);
    }

    public int size() {
        return regions.size();
    }

    /** The micro-region the point lies in, if any. */
    public Optional<String> regionOf(double latitude, double longitude) {
        for (Region r : regions) {
            for (List<double[][]> polygon : r.polygons()) {
                if (inside(longitude, latitude, polygon.getFirst())
                        && polygon.stream().skip(1).noneMatch(hole -> inside(longitude, latitude, hole))) {
                    return Optional.of(r.id());
                }
            }
        }
        return Optional.empty();
    }

    /** Ray casting: a ray from the point crosses the ring's edges an odd number of times when inside. */
    static boolean inside(double x, double y, double[][] ring) {
        boolean in = false;
        for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            double xi = ring[i][0], yi = ring[i][1], xj = ring[j][0], yj = ring[j][1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }
}
