package io.github.allannava.cragcast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIf(DockerAvailable.CHECK)
class CragcastBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
