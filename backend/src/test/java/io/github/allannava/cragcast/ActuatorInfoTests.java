package io.github.allannava.cragcast;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** /actuator/info says which release runs: the image's CRAGCAST_VERSION, set by release.yml. */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false", "cragcast.avalanche.archive.enabled=false",
        "cragcast.observations.enabled=false",
        "CRAGCAST_VERSION=v2026.01.01"})
@AutoConfigureMockMvc
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class ActuatorInfoTests {

    @Autowired
    MockMvc mvc;

    @Test
    void namesTheRelease() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.version").value("v2026.01.01"));
    }
}
