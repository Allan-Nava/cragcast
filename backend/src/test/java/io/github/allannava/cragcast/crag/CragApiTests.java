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

// No hourly refresh or history sync in tests: they would call Open-Meteo and GitHub.
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false", "cragcast.avalanche.archive.enabled=false"})
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
        Crag c = new Crag("test-wall", "Test Wall", "Test", 45.0, 9.0, Aspect.S,
                "https://example.org/test-wall");
        c.setExtras(820, "{\"gradeMin\":\"5a\",\"gradeMax\":\"7b\",\"styles\":[\"sport\"]}");
        crags.save(c);
    }

    @Test
    void listsCrags() throws Exception {
        mvc.perform(get("/api/crags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("test-wall"))
                .andExpect(jsonPath("$[0].aspect").value("S"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].elevation").value(820))
                .andExpect(jsonPath("$[0].details.gradeMax").value("7b"))
                .andExpect(jsonPath("$[0].details.styles[0]").value("sport"));
    }

    @Test
    void unknownCragIs404() throws Exception {
        mvc.perform(get("/api/crags/nowhere")).andExpect(status().isNotFound());
    }
}
