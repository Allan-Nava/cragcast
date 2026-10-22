package io.github.allannava.cragcast.osm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;

/** One single route from OpenStreetMap, linked to its crag and sector when one is near (CG-105). */
@Entity
@Table(name = "route")
public class Route {

    @Id
    private String id;

    @Column(name = "crag_id")
    private Long cragId;

    @Column(name = "sector_id")
    private String sectorId;

    @Column(name = "linked_by")
    private String linkedBy;

    @Column(nullable = false)
    private String climbing;

    private String name;
    private String grade;

    @Column(name = "grade_rank")
    private Integer gradeRank;

    @Column(name = "length_m")
    private Double lengthM;

    private Integer bolts;
    private Integer pitches;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "details", nullable = false)
    private String detailsJson;

    protected Route() {
        // for JPA
    }

    Route(Long cragId, RouteImporter.RouteEntry e, String detailsJson) {
        Map<String, Object> d = e.details() == null ? Map.of() : e.details();
        this.id = e.id();
        this.cragId = cragId;
        this.sectorId = cragId == null ? null : e.sector();
        this.linkedBy = cragId == null ? null : e.linkedBy();
        this.climbing = e.climbing();
        this.name = e.name();
        this.grade = d.get("grade") == null ? null : d.get("grade").toString();
        this.gradeRank = FrenchGrade.rank(grade);
        this.lengthM = d.get("length") instanceof Number n ? n.doubleValue() : null;
        this.bolts = d.get("bolts") instanceof Number n ? n.intValue() : null;
        this.pitches = d.get("pitches") instanceof Number n ? n.intValue() : null;
        this.latitude = e.latitude();
        this.longitude = e.longitude();
        this.detailsJson = detailsJson;
    }

    public String getId() { return id; }
    public Long getCragId() { return cragId; }
    public String getSectorId() { return sectorId; }
    public String getLinkedBy() { return linkedBy; }
    public String getClimbing() { return climbing; }
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public Integer getGradeRank() { return gradeRank; }
    public Double getLengthM() { return lengthM; }
    public Integer getBolts() { return bolts; }
    public Integer getPitches() { return pitches; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDetailsJson() { return detailsJson; }
}
