package io.github.allannava.cragcast.crag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Crag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    /** Null for crags from OpenStreetMap, which gives no area (CG-25). */
    private String region;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** Null when nobody has mapped it: the scorer then judges the wall as shaded. */
    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private Aspect aspect;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    /** Metres, from the Copernicus DEM: the ground of the area. Null until imported. */
    private Integer elevation;

    /**
     * The OSM climbing details as JSON text ({@code {"gradeMin":"5a",...}}), stored and returned
     * whole. Text rather than a mapped JSON type: Hibernate's JSON mapping and the Jackson 3 of
     * Spring Boot 4 are not a pairing to rely on, and nothing queries inside it.
     */
    @Column(name = "details", nullable = false)
    private String detailsJson = "{}";

    protected Crag() {
        // for JPA
    }

    public Crag(String slug, String name, String region, double latitude, double longitude,
                Aspect aspect, String sourceUrl) {
        this.slug = slug;
        this.name = name;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.aspect = aspect;
        this.sourceUrl = sourceUrl;
    }

    /** Takes the latest values from the source, keeping the id and slug. */
    public void update(String name, String region, double latitude, double longitude, Aspect aspect,
                       String sourceUrl) {
        this.name = name;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.aspect = aspect;
        this.sourceUrl = sourceUrl;
    }

    public void setExtras(Integer elevation, String detailsJson) {
        this.elevation = elevation;
        this.detailsJson = detailsJson == null || detailsJson.isBlank() ? "{}" : detailsJson;
    }

    public Integer getElevation() { return elevation; }
    public String getDetailsJson() { return detailsJson; }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Aspect getAspect() { return aspect; }
    public String getSourceUrl() { return sourceUrl; }
}
