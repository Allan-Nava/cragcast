package io.github.allannava.cragcast.calibration;

import java.time.LocalDate;

/**
 * What the rock was actually like at a crag on a day, seen by someone who was there.
 *
 * @param crag     the crag's id in data/crags.json (osm-node-…), or its exact name
 * @param observer who saw it, so a doubtful row can be asked about
 */
public record Observation(LocalDate date, String crag, Rock rock, String observer, String note) {

    public enum Rock { DRY, DAMP, WET }
}
