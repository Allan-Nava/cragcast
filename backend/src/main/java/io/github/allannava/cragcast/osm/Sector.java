package io.github.allannava.cragcast.osm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One sector of a crag (CG-104): an OSM object that tools/osm_crags.py folded into the crag,
 * with its own position, aspect and grades. The crag's row keeps the folded values.
 */
@Entity
@Table(name = "sector")
public class Sector {

    @Id
    private String id;

    @Column(name = "crag_id", nullable = false)
    private long cragId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String category;

    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String aspect;

    @Column(name = "grade_min")
    private String gradeMin;

    @Column(name = "grade_max")
    private String gradeMax;

    @Column(name = "details", nullable = false)
    private String detailsJson;

    protected Sector() {
        // for JPA
    }

    Sector(long cragId, int position, OsmObjectsFile.Entry e, String detailsJson) {
        this.id = e.id();
        this.cragId = cragId;
        this.position = position;
        this.category = e.category();
        this.name = e.name();
        this.latitude = e.latitude();
        this.longitude = e.longitude();
        this.aspect = e.aspect();
        this.gradeMin = OsmObject.text(e, "gradeMin");
        this.gradeMax = OsmObject.text(e, "gradeMax");
        this.detailsJson = detailsJson;
    }

    public String getId() { return id; }
    public long getCragId() { return cragId; }
    public int getPosition() { return position; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAspect() { return aspect; }
    public String getGradeMin() { return gradeMin; }
    public String getGradeMax() { return gradeMax; }
    public String getDetailsJson() { return detailsJson; }
}
