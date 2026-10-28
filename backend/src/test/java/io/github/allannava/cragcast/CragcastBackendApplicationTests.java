package io.github.allannava.cragcast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
// No hourly refresh in tests: it would call the real Open-Meteo.
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false", "cragcast.avalanche.archive.enabled=false"})
@EnabledIf(DockerAvailable.CHECK)
class CragcastBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
