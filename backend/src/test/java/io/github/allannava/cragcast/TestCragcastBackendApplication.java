package io.github.allannava.cragcast;

import org.springframework.boot.SpringApplication;

public class TestCragcastBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CragcastBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
