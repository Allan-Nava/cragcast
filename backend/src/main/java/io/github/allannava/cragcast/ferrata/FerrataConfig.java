package io.github.allannava.cragcast.ferrata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** CG-115: the via ferratas' Open-Meteo client, on the same base URL as the crags'. */
@Configuration
class FerrataConfig {

    @Bean
    FerrataClient ferrataClient(@Value("${cragcast.open-meteo.base-url}") String baseUrl) {
        return FerrataClient.create(baseUrl);
    }
}
