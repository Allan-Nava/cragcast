package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrenchGradeTest {

    @Test
    void ranksSortAsClimbersReadTheGrade() {
        List<String> shuffled = List.of("7a", "6a+", "5c", "6c+", "4", "6b", "8a", "6a", "6c");
        assertThat(shuffled.stream().sorted(Comparator.comparing(FrenchGrade::rank)).toList())
                .containsExactly("4", "5c", "6a", "6a+", "6b", "6c", "6c+", "7a", "8a");
        assertThat(FrenchGrade.rank("6a")).isEqualTo(36);
        assertThat(FrenchGrade.rank("6c+")).isEqualTo(41);
        assertThat(FrenchGrade.rank("7a")).isEqualTo(42);
    }

    @Test
    void aBareNumberIsItsAAndAnythingElseIsNoRank() {
        assertThat(FrenchGrade.rank("6")).isEqualTo(FrenchGrade.rank("6a"));
        assertThat(FrenchGrade.rank(" 6B+ ")).isEqualTo(FrenchGrade.rank("6b+"));
        assertThat(FrenchGrade.rank(null)).isNull();
        assertThat(FrenchGrade.rank("V+")).isNull();
        assertThat(FrenchGrade.rank("6d")).isNull();
    }
}
