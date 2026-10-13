package io.github.allannava.cragcast;

import org.testcontainers.DockerClientFactory;

/**
 * Tests that need a real PostgreSQL run where Docker exists (CI always has it) and are
 * skipped, not failed, on a machine without it: {@code @EnabledIf(DockerAvailable.CHECK)}.
 */
public final class DockerAvailable {

    public static final String CHECK = "io.github.allannava.cragcast.DockerAvailable#isAvailable";

    private DockerAvailable() {
    }

    public static boolean isAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
