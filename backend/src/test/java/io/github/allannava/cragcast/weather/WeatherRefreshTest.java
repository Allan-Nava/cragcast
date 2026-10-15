package io.github.allannava.cragcast.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Synthetic crags and forecasts, made up for the test. */
class WeatherRefreshTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Clock JUNE_2_MORNING = Clock.fixed(
            LocalDateTime.of(2026, 6, 2, 10, 0).atZone(ROME).toInstant(), ROME);

    /** Five days of hours from 2026-05-31: enough for the 48 h before 06-02 and all of 06-04. */
    private static HourlyForecast dry() {
        List<LocalDateTime> t = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            t.add(LocalDateTime.of(2026, 5, 31, 0, 0).plusHours(i));
        }
        return new HourlyForecast(t, new ArrayList<>(Collections.nCopies(120, 0.0)),
                new ArrayList<>(Collections.nCopies(120, 17.0)), new ArrayList<>(Collections.nCopies(120, 9.0)));
    }

    private static Crag crag(long id) {
        Crag c = mock(Crag.class);
        when(c.getId()).thenReturn(id);
        when(c.getLatitude()).thenReturn(45.0 + id);
        when(c.getLongitude()).thenReturn(9.0);
        when(c.getAspect()).thenReturn(Aspect.S);
        return c;
    }

    @Test
    void storesTodayTomorrowAndTheDayAfterForEveryCrag() {
        CragRepository crags = mock(CragRepository.class);
        CragWeatherRepository weather = mock(CragWeatherRepository.class);
        OpenMeteoClient openMeteo = mock(OpenMeteoClient.class);
        List<Crag> two = List.of(crag(1), crag(2));   // built first: Mockito refuses nested stubbing
        when(crags.findAll()).thenReturn(two);
        when(openMeteo.hourly(anyList())).thenReturn(List.of(dry(), dry()));
        when(weather.findById(any())).thenReturn(Optional.empty());

        int rows = new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING).refresh();

        assertThat(rows).isEqualTo(6);
        ArgumentCaptor<CragWeather> saved = ArgumentCaptor.forClass(CragWeather.class);
        verify(weather, times(6)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(CragWeather::getDay)
                .containsOnly(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 4));
        assertThat(saved.getAllValues().getFirst().weather().maxTemperatureC()).isEqualTo(17.0);
        assertThat(saved.getAllValues().getFirst().getFetchedAt()).isEqualTo(Instant.now(JUNE_2_MORNING));
        verify(openMeteo, times(1)).hourly(anyList());
    }

    @Test
    void updatesARowAlreadyThere() {
        CragRepository crags = mock(CragRepository.class);
        CragWeatherRepository weather = mock(CragWeatherRepository.class);
        OpenMeteoClient openMeteo = mock(OpenMeteoClient.class);
        List<Crag> one = List.of(crag(1));
        when(crags.findAll()).thenReturn(one);
        when(openMeteo.hourly(anyList())).thenReturn(List.of(dry()));
        var old = new CragWeather(1, LocalDate.of(2026, 6, 3),
                new io.github.allannava.cragcast.conditions.Weather(9, 9, 1, 99), Instant.EPOCH);
        when(weather.findById(any())).thenReturn(Optional.empty());
        when(weather.findById(new CragWeather.Key(1, LocalDate.of(2026, 6, 3)))).thenReturn(Optional.of(old));

        new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING).refresh();

        assertThat(old.weather().maxWindKmh()).isEqualTo(9.0);
        assertThat(old.getFetchedAt()).isEqualTo(Instant.now(JUNE_2_MORNING));
        verify(weather).save(old);
    }
}
