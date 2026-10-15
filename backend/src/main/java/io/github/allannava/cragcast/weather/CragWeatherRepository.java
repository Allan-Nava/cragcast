package io.github.allannava.cragcast.weather;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CragWeatherRepository extends JpaRepository<CragWeather, CragWeather.Key> {
}
