package io.github.allannava.cragcast;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.conditions.ConditionsScorer;
import io.github.allannava.cragcast.ferrata.FerrataReducer;
import io.github.allannava.cragcast.ferrata.FerrataScorer;
import io.github.allannava.cragcast.weather.WeatherReducer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * CG-130: the help on the page says what the verdict is measured against, from
 * web/src/lib/rules.ts. This test keeps that file honest: each of its numbers must equal the
 * scorer's or reducer's constant of the same name, and every numeric constant of the two scorers
 * must be listed there — a threshold changed in Java without the page's words fails here.
 */
class HelpRulesTest {

    private static final Path RULES = Path.of("../web/src/lib/rules.ts");

    /** {@code export const NAME = { key: number, … };} → key → number. */
    private static Map<String, Double> block(String source, String name) {
        Matcher m = Pattern.compile("export const " + name + " = \\{(.*?)\\};", Pattern.DOTALL).matcher(source);
        assertThat(m.find()).as("%s in %s", name, RULES).isTrue();
        Map<String, Double> out = new LinkedHashMap<>();
        Matcher kv = Pattern.compile("(\\w+):\\s*(-?\\d+(?:\\.\\d+)?)").matcher(m.group(1));
        while (kv.find()) {
            out.put(kv.group(1), Double.valueOf(kv.group(2)));
        }
        return out;
    }

    /** climbFromHour → CLIMB_FROM_HOUR. */
    private static String constant(String key) {
        return key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
    }

    private static Map<String, Double> constants(Class<?>... classes) throws IllegalAccessException {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Class<?> c : classes) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) && Modifier.isFinal(mod)
                        && (f.getType() == double.class || f.getType() == int.class)) {
                    f.setAccessible(true);
                    out.put(f.getName(), ((Number) f.get(null)).doubleValue());
                }
            }
        }
        return out;
    }

    private static void sameNumbers(Map<String, Double> page, Map<String, Double> java, Class<?> scorer)
            throws IllegalAccessException {
        page.forEach((key, value) -> assertThat(java).as("%s (%s)", key, constant(key))
                .containsEntry(constant(key), value));
        // Every threshold of the scorer is said on the page.
        for (String name : constants(scorer).keySet()) {
            assertThat(page.keySet().stream().map(HelpRulesTest::constant)).as("%s in %s", name, RULES).contains(name);
        }
    }

    @Test
    void theCragsHelpSaysTheScorersNumbers() throws IOException, IllegalAccessException {
        sameNumbers(block(Files.readString(RULES), "CRAG"), constants(ConditionsScorer.class, WeatherReducer.class),
                ConditionsScorer.class);
    }

    @Test
    void theViaFerratasHelpSaysTheirScorersNumbers() throws IOException, IllegalAccessException {
        sameNumbers(block(Files.readString(RULES), "FERRATA"), constants(FerrataScorer.class, FerrataReducer.class),
                FerrataScorer.class);
    }

    @Test
    void namesTurnIntoConstants() {
        assertThat(constant("climbFromHour")).isEqualTo("CLIMB_FROM_HOUR");
        assertThat(constant("capeNoGoJkg")).isEqualTo("CAPE_NO_GO_JKG");
        assertThat(constant("shortDayH")).isEqualTo("SHORT_DAY_H");
    }
}
