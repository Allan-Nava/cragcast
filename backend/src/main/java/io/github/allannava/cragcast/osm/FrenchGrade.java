package io.github.allannava.cragcast.osm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** French climbing grades in order (CG-105), for the route table's grade_rank. */
final class FrenchGrade {

    // The form tools/osm_crags.py validates: 4, 5c, 6b+, 8a.
    private static final Pattern GRADE = Pattern.compile("([1-9])([abc]?)(\\+?)");

    private FrenchGrade() {
    }

    /**
     * 6a = 36, 6a+ = 37, 6b = 38, 6b+ = 39, 6c = 40, 6c+ = 41, 7a = 42: six steps a number, so
     * the rank sorts as climbers read the grade. A bare number counts as its "a" (6 = 6a). Null
     * for no grade or one not in that form.
     */
    static Integer rank(String grade) {
        if (grade == null) {
            return null;
        }
        Matcher m = GRADE.matcher(grade.trim().toLowerCase());
        if (!m.matches()) {
            return null;
        }
        int letter = m.group(2).isEmpty() ? 0 : m.group(2).charAt(0) - 'a';
        return Integer.parseInt(m.group(1)) * 6 + letter * 2 + (m.group(3).isEmpty() ? 0 : 1);
    }
}
