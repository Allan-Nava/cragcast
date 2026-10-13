package io.github.allannava.cragcast;

/** Lets tests in sub-packages import the package-private Testcontainers configuration. */
public final class TestcontainersConfigurationAccess {

    private TestcontainersConfigurationAccess() {
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @org.springframework.context.annotation.Import(TestcontainersConfiguration.class)
    public static class Config {
    }
}
