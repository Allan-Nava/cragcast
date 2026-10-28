package io.github.allannava.cragcast.ferrata;

/**
 * CG-113: what the via ferrata verdict reads for one day, at the route's top.
 *
 * @param rainLast48hMm      rain in the 48 hours before the day
 * @param snowLast48hCm      snowfall in the 48 hours before the day
 * @param rainDuringDayMm    rain in the day's window ({@link FerrataReducer})
 * @param snowDuringDayCm    snowfall in the day's window
 * @param thunderstormHours  hours of the window with a thunderstorm weather code (95, 96, 99)
 * @param maxCapeJkg         the window's highest CAPE, J/kg; 0 when the model gives none
 * @param maxWindKmh         the window's strongest sustained wind
 * @param maxGustKmh         the window's strongest gust
 * @param minTemperatureC    the window's lowest temperature
 * @param maxTemperatureC    the window's highest temperature
 * @param daylightHours      hours of light in the whole day
 */
public record FerrataWeather(double rainLast48hMm, double snowLast48hCm, double rainDuringDayMm,
                             double snowDuringDayCm, int thunderstormHours, double maxCapeJkg, double maxWindKmh,
                             double maxGustKmh, double minTemperatureC, double maxTemperatureC, int daylightHours) {
}
