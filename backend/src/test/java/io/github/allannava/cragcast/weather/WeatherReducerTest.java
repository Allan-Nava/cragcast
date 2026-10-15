package io.github.allannava.cragcast.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Synthetic forecasts: made-up numbers placed on chosen hours, to pin the boundaries. */
class WeatherReducerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDate DAY = LocalDate.of(2026, 6, 3);

    /** Four days of hours from START: dry, 15 °C, 10 km/h, except where a test sets otherwise. */
    private static List<Double>[] blank() {
        @SuppressWarnings("unchecked")
        List<Double>[] cols = new List[] {new ArrayList<Double>(), new ArrayList<Double>(), new ArrayList<Double>()};
        for (int i = 0; i < 96; i++) {
            cols[0].add(0.0);
            cols[1].add(15.0);
            cols[2].add(10.0);
        }
        return cols;
    }

    private static List<LocalDateTime> times() {
        List<LocalDateTime> t = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            t.add(START.plusHours(i));
        }
        return t;
    }

    private static int at(int day, int hour) {
        return (day - 1) * 24 + hour;
    }

    @Test
    void sumsTheRainOfTheLast48HoursUpToMidnight() {
        var c = blank();
        c[0].set(at(1, 0), 5.0);   // ends exactly 48 h before midnight: outside the window
        c[0].set(at(1, 1), 1.0);   // first hour inside
        c[0].set(at(2, 10), 3.0);
        c[0].set(at(3, 0), 2.0);   // the hour ending at midnight: inside
        var w = WeatherReducer.reduce(new HourlyForecast(times(), c[0], c[1], c[2]), DAY);
        assertThat(w.rainLast48hMm()).isCloseTo(6.0, within(1e-9));
    }

    @Test
    void climbingWindowIsTheHoursEndingFrom09To18() {
        var c = blank();
        c[0].set(at(3, 8), 4.0);   // ends at 08:00: before the window
        c[0].set(at(3, 9), 1.0);
        c[0].set(at(3, 18), 0.5);
        c[0].set(at(3, 19), 7.0);  // after the window
        c[1].set(at(3, 14), 27.0);
        c[1].set(at(3, 20), 35.0); // evening heat does not count
        c[2].set(at(3, 12), 42.0);
        var w = WeatherReducer.reduce(new HourlyForecast(times(), c[0], c[1], c[2]), DAY);
        assertThat(w.rainDuringDayMm()).isCloseTo(1.5, within(1e-9));
        assertThat(w.maxTemperatureC()).isEqualTo(27.0);
        assertThat(w.maxWindKmh()).isEqualTo(42.0);
    }

    @Test
    void refusesADayTheForecastDoesNotCover() {
        var c = blank();
        var f = new HourlyForecast(times(), c[0], c[1], c[2]);
        assertThatThrownBy(() -> WeatherReducer.reduce(f, LocalDate.of(2026, 6, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not cover");
    }
}
