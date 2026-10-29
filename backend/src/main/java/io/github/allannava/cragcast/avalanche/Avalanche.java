package io.github.allannava.cragcast.avalanche;

import java.util.List;

/**
 * CG-11: the avalanche danger for one micro-region on one day, from the regional bulletin.
 * Only the facts — the 1–5 level per elevation band and the avalanche problem types — never
 * the bulletin's text; {@code bulletin} links to the official one.
 *
 * @param region   the EAWS micro-region id, e.g. IT-25-LC-01
 * @param name     the region's name in the bulletin, e.g. "PREALPI CENTRALI"
 * @param level    the highest level of the day, 1 (low) to 5 (very high)
 * @param ratings  the levels by elevation band and part of the day, as published
 * @param problems the avalanche problem types (CAAML v6: new_snow, wind_slab, …), each once
 * @param bulletin the official bulletin to read before going
 * @param provider CG-120: who issues it — ARPA Lombardia, AINEVA Piemonte, the Euregio
 */
public record Avalanche(String region, String name, int level, List<Rating> ratings, List<String> problems,
                        String bulletin, String provider) {

    /** The provider from the region's id (AvalancheBulletins.providerOf). */
    public Avalanche(String region, String name, int level, List<Rating> ratings, List<String> problems, String bulletin) {
        this(region, name, level, ratings, problems, bulletin, AvalancheBulletins.providerOf(region).name());
    }

    /**
     * @param above  lower bound of the band: metres ("2000") or "treeline"; null when unbounded
     * @param below  upper bound of the band, same form; null when unbounded
     * @param period "earlier" or "later" in the day; null for the whole day
     */
    public record Rating(int level, String above, String below, String period) {
    }
}
