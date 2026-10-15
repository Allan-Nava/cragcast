package io.github.allannava.cragcast.conditions;

import io.github.allannava.cragcast.crag.CragRepository;
import io.github.allannava.cragcast.weather.CragWeather;
import io.github.allannava.cragcast.weather.CragWeatherRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@code GET /api/crags/{slug}/conditions?date=YYYY-MM-DD}: the verdict for one crag and one
 * day, from the weather the hourly refresh stored. Without a date, tomorrow.
 */
@RestController
class ConditionsController {

    private final CragRepository crags;
    private final CragWeatherRepository weather;
    private final ConditionsScorer scorer = new ConditionsScorer();
    private final Clock clock;

    ConditionsController(CragRepository crags, CragWeatherRepository weather, Clock clock) {
        this.crags = crags;
        this.weather = weather;
        this.clock = clock;
    }

    @GetMapping("/api/crags/{slug}/conditions")
    ConditionsView conditions(@PathVariable String slug,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var crag = crags.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no crag " + slug));
        LocalDate day = date != null ? date : LocalDate.now(clock).plusDays(1);
        CragWeather row = weather.findById(new CragWeather.Key(crag.getId(), day))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "no forecast for " + slug + " on " + day + " (today to the day after tomorrow are kept)"));
        var result = scorer.score(crag.getAspect(), row.weather());
        return new ConditionsView(crag.getSlug(), day, result.verdict(), result.reasons(), row.weather(), row.getFetchedAt());
    }

    record ConditionsView(String crag, LocalDate date, Verdict verdict, List<String> reasons, Weather weather,
                          Instant fetchedAt) {
    }
}
