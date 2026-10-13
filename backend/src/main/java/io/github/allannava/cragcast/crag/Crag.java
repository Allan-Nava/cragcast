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

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Aspect aspect;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

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

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Aspect getAspect() { return aspect; }
    public String getSourceUrl() { return sourceUrl; }
}
