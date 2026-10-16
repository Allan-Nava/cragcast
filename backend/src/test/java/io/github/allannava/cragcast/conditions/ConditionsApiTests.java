package io.github.allannava.cragcast.conditions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import io.github.allannava.cragcast.weather.CragWeather;
import io.github.allannava.cragcast.weather.CragWeatherRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Against a real PostgreSQL (Testcontainers); made-up crag and weather. */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false"})
@AutoConfigureMockMvc
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class ConditionsApiTests {

    @Autowired MockMvc mvc;
    @Autowired CragRepository crags;
    @Autowired CragWeatherRepository weather;

    @BeforeEach
    void seed() {
        weather.deleteAll();
        crags.deleteAll();
        Crag c = crags.save(new Crag("test-wall", "Test Wall", "Test", 45.0, 9.0, Aspect.N, "https://example.org/w"));
        weather.save(new CragWeather(c.getId(), LocalDate.of(2026, 6, 3), new Weather(0, 0, 18, 10), Instant.EPOCH));
        weather.save(new CragWeather(c.getId(), LocalDate.of(2026, 6, 4), new Weather(8, 0, 18, 10), Instant.EPOCH));
    }

    @Test
    void dryDayIsGo() throws Exception {
        mvc.perform(get("/api/crags/test-wall/conditions").param("date", "2026-06-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("GO"))
                .andExpect(jsonPath("$.weather.maxTemperatureC").value(18.0));
    }

    @Test
    void recentRainOnAShadedWallIsMaybeWithTheReason() throws Exception {
        mvc.perform(get("/api/crags/test-wall/conditions").param("date", "2026-06-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MAYBE"))
                .andExpect(jsonPath("$.reasons[0]").value("rock may still be wet from recent rain"));
    }

    @Test
    void aDayWithoutForecastIs404() throws Exception {
        mvc.perform(get("/api/crags/test-wall/conditions").param("date", "2026-06-09"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnknownCragIs404() throws Exception {
        mvc.perform(get("/api/crags/nowhere/conditions")).andExpect(status().isNotFound());
    }
}
