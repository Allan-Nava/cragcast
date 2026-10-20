package io.github.allannava.cragcast.osm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One object of the OpenStreetMap climbing query, copied from data/osm-climbing.json (CG-103). */
@Entity
@Table(name = "osm_object")
public class OsmObject {

    @Id
    private String id;

    @Column(name = "osm_type", nullable = false)
    private String osmType;

    @Column(name = "osm_id", nullable = false)
    private long osmId;

    @Column(name = "category", nullable = false)
    private String category;

    private String name;
    private String climbing;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String aspect;
    private String grade;

    @Column(name = "grade_min")
    private String gradeMin;

    @Column(name = "grade_max")
    private String gradeMax;

    @Column(name = "details", nullable = false)
    private String detailsJson;

    protected OsmObject() {
        // for JPA
    }

    OsmObject(OsmObjectsFile.Entry e, String detailsJson) {
        this.id = e.id();
        this.osmType = e.osmType();
        this.osmId = e.osmId();
        this.category = e.category();
        this.name = e.name();
        this.climbing = e.climbing();
        this.latitude = e.latitude();
        this.longitude = e.longitude();
        this.aspect = e.aspect();
        this.grade = text(e, "grade");
        this.gradeMin = text(e, "gradeMin");
        this.gradeMax = text(e, "gradeMax");
        this.detailsJson = detailsJson;
    }

    private static String text(OsmObjectsFile.Entry e, String key) {
        Object v = e.details() == null ? null : e.details().get(key);
        return v == null ? null : v.toString();
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getClimbing() { return climbing; }
    public String getGrade() { return grade; }
    public String getGradeMax() { return gradeMax; }
    public String getAspect() { return aspect; }
    public String getDetailsJson() { return detailsJson; }
}
