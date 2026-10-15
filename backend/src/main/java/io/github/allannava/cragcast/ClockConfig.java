package io.github.allannava.cragcast;

import io.github.allannava.cragcast.weather.OpenMeteoClient;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** "Today" is the climbers' today, in Italy; tests replace the clock to pin it. */
@Configuration
@EnableScheduling
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of(OpenMeteoClient.TIMEZONE));
    }
}
