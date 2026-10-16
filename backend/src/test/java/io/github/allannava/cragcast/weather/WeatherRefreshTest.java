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
        when(c.getSlug()).thenReturn("test-crag-" + id);
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

        int rows = new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING, e -> { }).refresh();

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

        new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING, e -> { }).refresh();

        assertThat(old.weather().maxWindKmh()).isEqualTo(9.0);
        assertThat(old.getFetchedAt()).isEqualTo(Instant.now(JUNE_2_MORNING));
        verify(weather).save(old);
    }

    @Test
    void afterARefreshTheForecastsGoOutBySlug() {
        CragRepository crags = mock(CragRepository.class);
        CragWeatherRepository weather = mock(CragWeatherRepository.class);
        OpenMeteoClient openMeteo = mock(OpenMeteoClient.class);
        List<Crag> two = List.of(crag(1), crag(2));
        when(crags.findAll()).thenReturn(two);
        HourlyForecast first = dry(), second = dry();
        when(openMeteo.hourly(anyList())).thenReturn(List.of(first, second));
        when(weather.findById(any())).thenReturn(Optional.empty());
        List<Object> events = new ArrayList<>();

        new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING, events::add).atStartup();

        assertThat(events).singleElement().isInstanceOfSatisfying(ForecastsRefreshed.class, e -> {
            assertThat(e.forecasts()).containsExactly(
                    java.util.Map.entry("test-crag-1", first), java.util.Map.entry("test-crag-2", second));
            assertThat(e.fetchedAt()).isEqualTo(Instant.now(JUNE_2_MORNING));
        });
    }

    @Test
    void aFailedRefreshSendsNothingAndAFailedListenerKeepsTheRows() {
        CragRepository crags = mock(CragRepository.class);
        CragWeatherRepository weather = mock(CragWeatherRepository.class);
        OpenMeteoClient openMeteo = mock(OpenMeteoClient.class);
        List<Crag> one = List.of(crag(1));
        when(crags.findAll()).thenReturn(one);
        when(openMeteo.hourly(anyList())).thenThrow(new IllegalStateException("Open-Meteo is down"));
        List<Object> events = new ArrayList<>();

        new WeatherRefresh(crags, weather, openMeteo, JUNE_2_MORNING, events::add).hourly();
        assertThat(events).isEmpty();

        OpenMeteoClient working = mock(OpenMeteoClient.class);
        when(working.hourly(anyList())).thenReturn(List.of(dry()));
        when(weather.findById(any())).thenReturn(Optional.empty());
        new WeatherRefresh(crags, weather, working, JUNE_2_MORNING, e -> { throw new IllegalStateException("no"); }).hourly();
        verify(weather, times(3)).save(any());
    }
}
