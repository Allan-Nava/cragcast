package io.github.allannava.cragcast.ferrata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A synthetic forecast from 2026-06-01, five days of hours, made up for the test. */
class FerrataReducerTest {

    private static final LocalDate JUNE_3 = LocalDate.of(2026, 6, 3);
    private static final int N = 120;

    /** Index of an hour on 2026-06-03. */
    private static int at(int hour) {
        return 48 + hour;
    }

    private static List<Double> flat(double v) {
        return new ArrayList<>(Collections.nCopies(N, v));
    }

    private static FerrataForecast forecast(List<Double> rain, List<Double> temp, List<Double> gust, List<Integer> code,
                                            List<Double> cape, List<Double> snow) {
        List<LocalDateTime> t = new ArrayList<>();
        List<Integer> day = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            t.add(LocalDateTime.of(2026, 6, 1, 0, 0).plusHours(i));
            day.add(t.get(i).getHour() >= 6 && t.get(i).getHour() < 21 ? 1 : 0);
        }
        return new FerrataForecast(t, rain, temp, flat(10), gust, code, cape, snow, day);
    }

    @Test
    void theWindowRunsFromSevenToNineteenAndThePastIsTheFortyEightHoursBefore() {
        List<Double> rain = flat(0), temp = flat(12), gust = flat(20), cape = flat(0), snow = flat(0);
        List<Integer> code = new ArrayList<>(Collections.nCopies(N, 0));
        rain.set(at(0), 3.0);            // the hour ending at midnight: the day before
        snow.set(at(0) - 10, 2.0);       // 14:00 the day before
        rain.set(at(7), 9.0);            // ending 07:00: before the window
        rain.set(at(8), 0.5);            // ending 08:00: the first hour of it
        code.set(at(19), 95);            // ending 19:00: the last hour of it
        code.set(at(20), 99);            // ending 20:00: after it
        cape.set(at(15), null);          // a model without CAPE for that hour
        cape.set(at(14), 850.0);
        temp.set(at(9), -1.0);
        temp.set(at(13), 24.0);
        gust.set(at(12), 70.0);

        FerrataWeather w = FerrataReducer.reduce(forecast(rain, temp, gust, code, cape, snow), JUNE_3);

        assertThat(w.rainLast48hMm()).isEqualTo(3.0);
        assertThat(w.snowLast48hCm()).isEqualTo(2.0);
        assertThat(w.rainDuringDayMm()).isEqualTo(0.5);
        assertThat(w.thunderstormHours()).isEqualTo(1);
        assertThat(w.maxCapeJkg()).isEqualTo(850.0);
        assertThat(w.maxGustKmh()).isEqualTo(70.0);
        assertThat(w.maxWindKmh()).isEqualTo(10.0);
        assertThat(w.minTemperatureC()).isEqualTo(-1.0);
        assertThat(w.maxTemperatureC()).isEqualTo(24.0);
        assertThat(w.daylightHours()).isEqualTo(15);
    }

    @Test
    void aForecastThatDoesNotCoverTheDayIsRefused() {
        List<Integer> code = new ArrayList<>(Collections.nCopies(N, 0));
        var f = forecast(flat(0), flat(12), flat(20), code, flat(0), flat(0));
        assertThatThrownBy(() -> FerrataReducer.reduce(f, LocalDate.of(2026, 6, 6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FerrataReducer.reduce(f, LocalDate.of(2026, 6, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listsOfDifferentLengthsAreRefused() {
        assertThatThrownBy(() -> new FerrataForecast(List.of(LocalDateTime.of(2026, 6, 1, 0, 0)), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
