package io.github.allannava.cragcast.conditions;

/**
 * The weather the score needs for one crag and one day, already reduced from the hourly
 * forecast (CG-3 fills it from Open-Meteo).
 *
 * @param rainLast48hMm       rain fallen in the 48 hours before the day, in millimetres
 * @param rainDuringDayMm     rain forecast during climbing hours of the day, in millimetres
 * @param maxTemperatureC     the day's maximum temperature, in degrees Celsius
 * @param maxWindKmh          the day's strongest sustained wind, in km/h
 */
public record Weather(double rainLast48hMm, double rainDuringDayMm, double maxTemperatureC,
                      double maxWindKmh) {
}
