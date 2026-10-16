package io.github.allannava.cragcast.history;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/** A crag's verdict on one past day, copied from the history branch (CG-101). */
@Entity
@Table(name = "crag_history")
public class CragHistory {

    @EmbeddedId
    private Key key;

    @Column(name = "crag_name", nullable = false)
    private String cragName;

    @Column(name = "province")
    private String province;

    @Column(name = "verdict", nullable = false)
    private String verdict;

    @Column(name = "rain_last48h_mm", nullable = false)
    private double rainLast48hMm;

    @Column(name = "rain_day_mm", nullable = false)
    private double rainDayMm;

    @Column(name = "max_temp_c", nullable = false)
    private double maxTempC;

    @Column(name = "max_wind_kmh", nullable = false)
    private double maxWindKmh;

    @Column(name = "reasons", nullable = false)
    private String reasons;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected CragHistory() {
        // for JPA
    }

    CragHistory(HistoryRow r) {
        this.key = new Key(r.day(), r.cragSlug());
        this.cragName = r.cragName();
        this.province = r.province();
        this.verdict = r.verdict();
        this.rainLast48hMm = r.rainLast48hMm();
        this.rainDayMm = r.rainDayMm();
        this.maxTempC = r.maxTempC();
        this.maxWindKmh = r.maxWindKmh();
        this.reasons = r.reasons();
        this.generatedAt = r.generatedAt();
    }

    public LocalDate getDay() { return key.day; }
    public String getCragSlug() { return key.cragSlug; }
    public String getCragName() { return cragName; }
    public String getVerdict() { return verdict; }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "day")
        private LocalDate day;

        @Column(name = "crag_slug")
        private String cragSlug;

        protected Key() {
        }

        public Key(LocalDate day, String cragSlug) {
            this.day = day;
            this.cragSlug = cragSlug;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.day.equals(day) && k.cragSlug.equals(cragSlug);
        }

        @Override
        public int hashCode() {
            return day.hashCode() * 31 + cragSlug.hashCode();
        }
    }
}
