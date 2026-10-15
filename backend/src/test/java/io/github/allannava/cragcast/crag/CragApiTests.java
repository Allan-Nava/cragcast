package io.github.allannava.cragcast.crag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// No hourly refresh in tests: it would call the real Open-Meteo.
@SpringBootTest(properties = "cragcast.refresh.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class CragApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CragRepository crags;

    @BeforeEach
    void seed() {
        crags.deleteAll();
        // Test data only: a made-up crag, not a real place.
        crags.save(new Crag("test-wall", "Test Wall", "Test", 45.0, 9.0, Aspect.S,
                "https://example.org/test-wall"));
    }

    @Test
    void listsCrags() throws Exception {
        mvc.perform(get("/api/crags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("test-wall"))
                .andExpect(jsonPath("$[0].aspect").value("S"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void unknownCragIs404() throws Exception {
        mvc.perform(get("/api/crags/nowhere")).andExpect(status().isNotFound());
    }
}
