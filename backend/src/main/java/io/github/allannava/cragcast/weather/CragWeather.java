package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.conditions.Weather;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/** The reduced weather of one crag on one day, as the hourly refresh last saw it. */
@Entity
@Table(name = "crag_weather")
public class CragWeather {

    @EmbeddedId
    private Key key;

    @Column(name = "rain_last48h_mm", nullable = false)
    private double rainLast48hMm;

    @Column(name = "rain_during_day_mm", nullable = false)
    private double rainDuringDayMm;

    @Column(name = "max_temperature_c", nullable = false)
    private double maxTemperatureC;

    @Column(name = "max_wind_kmh", nullable = false)
    private double maxWindKmh;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected CragWeather() {
        // for JPA
    }

    public CragWeather(long cragId, LocalDate day, Weather w, Instant fetchedAt) {
        this.key = new Key(cragId, day);
        set(w, fetchedAt);
    }

    public void set(Weather w, Instant fetchedAt) {
        this.rainLast48hMm = w.rainLast48hMm();
        this.rainDuringDayMm = w.rainDuringDayMm();
        this.maxTemperatureC = w.maxTemperatureC();
        this.maxWindKmh = w.maxWindKmh();
        this.fetchedAt = fetchedAt;
    }

    public Weather weather() {
        return new Weather(rainLast48hMm, rainDuringDayMm, maxTemperatureC, maxWindKmh);
    }

    public Instant getFetchedAt() { return fetchedAt; }
    public LocalDate getDay() { return key.day; }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "crag_id")
        private long cragId;

        @Column(name = "day")
        private LocalDate day;

        protected Key() {
        }

        public Key(long cragId, LocalDate day) {
            this.cragId = cragId;
            this.day = day;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.cragId == cragId && k.day.equals(day);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(cragId) * 31 + day.hashCode();
        }
    }
}
