package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.conditions.Verdict;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CG-45: one small static page per crag, {@code c/<id>.html}, rebuilt with the data every hour.
 * Link previews (WhatsApp, Telegram…) read Open Graph tags without running JavaScript, so these
 * pages carry the crag's name, tomorrow's verdict and weather in plain HTML; a browser is sent
 * straight on to the crag's panel ({@code ../?crag=<id>}). The day is written as a date, never
 * "domani": a preview is read later than it is made.
 */
final class SharePages {

    static final String IMAGE = "og.png";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN);
    private static final Map<Verdict, String> LABEL = Map.of(Verdict.GO, "si va", Verdict.MAYBE, "forse", Verdict.NO_GO, "no");
    /** The same words as the page (web/src/lib/text.ts, REASON_IT). */
    private static final Map<String, String> REASON_IT = Map.of(
            "rain forecast during the day", "pioggia prevista durante il giorno",
            "strong wind", "vento forte",
            "rock may still be wet from recent rain", "roccia forse ancora bagnata dalla pioggia recente",
            "cold", "freddo",
            "hot on a sunny wall", "caldo su parete al sole");

    private SharePages() {
    }

    /** Relative path → HTML, for every crag; {@code siteUrl} ends with a slash. */
    static Map<String, String> pages(ConditionsExport.Output out, String siteUrl) {
        Map<String, String> files = new LinkedHashMap<>();
        String day = out.days().size() > 1 ? out.days().get(1) : out.days().getFirst();   // tomorrow
        for (ConditionsExport.CragOut c : out.crags()) {
            files.put("c/" + c.id() + ".html", page(c, day, siteUrl));
        }
        return files;
    }

    static String page(ConditionsExport.CragOut c, String day, String siteUrl) {
        ConditionsExport.DayOut d = c.days().get(day);
        String date = DAY.format(LocalDate.parse(day));
        date = Character.toUpperCase(date.charAt(0)) + date.substring(1);
        String title = c.name() + " — " + date.substring(0, 1).toLowerCase(Locale.ITALIAN) + date.substring(1)
                + ": " + LABEL.get(d.verdict());
        String description = description(c, d, date);
        String app = siteUrl + "?crag=" + c.id();
        String self = siteUrl + "c/" + c.id() + ".html";
        return """
                <!doctype html>
                <html lang="it">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%1$s</title>
                <meta name="description" content="%2$s">
                <meta name="robots" content="noindex">
                <link rel="canonical" href="%3$s">
                <meta property="og:type" content="website">
                <meta property="og:site_name" content="cragcast">
                <meta property="og:locale" content="it_IT">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:url" content="%4$s">
                <meta property="og:image" content="%5$s">
                <meta property="og:image:width" content="1280">
                <meta property="og:image:height" content="640">
                <meta name="twitter:card" content="summary_large_image">
                <meta http-equiv="refresh" content="0; url=../?crag=%6$s">
                </head>
                <body style="background:#050505;color:#f2f4f5;font:16px/1.45 system-ui,sans-serif;padding:16px">
                <p><a href="../?crag=%6$s" style="color:#10cf53">%1$s</a></p>
                <p>%2$s</p>
                <script>location.replace("../?crag=%6$s" + location.hash)</script>
                </body>
                </html>
                """.formatted(html(title), html(description), html(app), html(self), html(siteUrl + IMAGE), html(c.id()));
    }

    /** "Venerdì 16 ottobre: si va. Max 19 °C, pioggia 0,0 mm, vento 12 km/h. Galbiate (LC), quota circa 482 m." */
    static String description(ConditionsExport.CragOut c, ConditionsExport.DayOut d, String date) {
        StringBuilder s = new StringBuilder(date).append(": ").append(LABEL.get(d.verdict()));
        List<String> reasons = d.reasons().stream().filter(r -> !r.startsWith("wall aspect not mapped"))
                .map(r -> REASON_IT.getOrDefault(r, r)).collect(Collectors.toList());
        if (!reasons.isEmpty()) {
            s.append(" (").append(String.join("; ", reasons)).append(')');
        }
        s.append(String.format(Locale.ITALIAN, ". Max %d °C, pioggia %.1f mm, vento %d km/h.",
                Math.round(d.weather().maxTemperatureC()), d.weather().rainDuringDayMm(), Math.round(d.weather().maxWindKmh())));
        if (c.municipality() != null) {
            s.append(' ').append(c.municipality()).append(c.provinceCode() != null ? " (" + c.provinceCode() + ")" : "");
            s.append(c.elevation() != null ? ", quota circa " + c.elevation() + " m." : ".");
        }
        return s.toString();
    }

    /** Text and attribute escaping: names come from OSM. */
    static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
