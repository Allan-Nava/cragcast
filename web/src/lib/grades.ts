// CG-61: the grade filter. French grades as OSM's climbing:grade:french:* holds them and
// tools/osm_crags.py validates them (4, 5c, 6b+), ordered as its grade_order: number, letter, plus.
import type { Details } from "../types";

export type GradeFilter = { dir: "le" | "ge"; grade: string };

/** The choices offered: up to an easier grade, or from a harder one. */
export const GRADE_CHOICES: GradeFilter[] = [
  { dir: "le", grade: "5c" }, { dir: "le", grade: "6a" }, { dir: "le", grade: "6b" }, { dir: "le", grade: "6c" },
  { dir: "ge", grade: "7a" }, { dir: "ge", grade: "7b" }, { dir: "ge", grade: "7c" }, { dir: "ge", grade: "8a" },
];

const GRADE = /^([1-9])([abc]?)(\+?)$/;

/** A number that sorts as the grade does: 5c < 6a < 6a+ < 6b; null for what is not a grade. */
export function gradeRank(g: string): number | null {
  const m = GRADE.exec(g.trim().toLowerCase());
  if (!m) return null;
  return Number(m[1]) * 100 + (m[2] ? m[2].charCodeAt(0) - 96 : 0) * 10 + (m[3] ? 1 : 0);
}

/** ?g=le6a / ?g=ge7a; null when absent or malformed. */
export function readGrade(g: string | null): GradeFilter | null {
  const m = /^(le|ge)(.+)$/.exec(g ?? "");
  return m && gradeRank(m[2]) != null ? { dir: m[1] as "le" | "ge", grade: m[2] } : null;
}

export const gradeParam = (f: GradeFilter): string => f.dir + f.grade;

/** Whether OSM gives the crag any grade at all. */
export const hasGrades = (d: Details | undefined): boolean =>
  gradeRank(d?.gradeMin ?? "") != null || gradeRank(d?.gradeMax ?? "") != null;

/**
 * Whether the crag surely has routes in the range: "fino al 6a" needs its easiest grade at most
 * 6a, "dal 7a" its hardest at least 7a. With one end only, the other end is used when it
 * already settles it (a crag up to 5c has routes up to 6a); otherwise it is left out, never
 * guessed in.
 */
export function inGrade(d: Details | undefined, f: GradeFilter): boolean {
  const want = gradeRank(f.grade)!;
  const min = gradeRank(d?.gradeMin ?? ""), max = gradeRank(d?.gradeMax ?? "");
  if (f.dir === "le") return (min ?? max) != null && (min ?? max)! <= want;
  return (max ?? min) != null && (max ?? min)! >= want;
}

/** "fino al 6a", "dal 7a", "dall'8a" — Italian elides before "otto". */
export function gradeLabel(f: GradeFilter): string {
  const vowel = f.grade.startsWith("8");
  return f.dir === "le" ? (vowel ? `fino all'${f.grade}` : `fino al ${f.grade}`) : (vowel ? `dall'${f.grade}` : `dal ${f.grade}`);
}

/** CG-118: a route's level, the grade's number: "le4" up to 4, then 5, 6 and 7, "ge8" from 8, or
 * "none" for a route OSM gives no grade. */
export type Level = "le4" | "5" | "6" | "7" | "ge8" | "none";

/** The levels in order, each with what a climber reads it as; the words are indicative. */
export const LEVELS: { level: Level; label: string; word: string | null }[] = [
  { level: "le4", label: "4 e meno", word: "facile" },
  { level: "5", label: "5", word: "medio" },
  { level: "6", label: "6", word: "impegnativo" },
  { level: "7", label: "7", word: "difficile" },
  { level: "ge8", label: "8 e oltre", word: "estremo" },
  { level: "none", label: "Senza grado", word: null },
];

export function routeLevel(grade: string | null): Level {
  const r = grade == null ? null : gradeRank(grade);
  if (r == null) return "none";
  const n = Math.floor(r / 100);
  return n <= 4 ? "le4" : n >= 8 ? "ge8" : (String(n) as Level);
}

/** "dal 3b al 7a", "solo 6a", or null without a grade; "all'8a" and "dall'8a" elide. */
export function gradeSpan(grades: (string | null)[]): string | null {
  const ranked = grades.filter((g): g is string => g != null && gradeRank(g) != null)
    .sort((a, b) => gradeRank(a)! - gradeRank(b)!);
  if (!ranked.length) return null;
  const [lo, hi] = [ranked[0], ranked[ranked.length - 1]];
  if (lo === hi) return `solo ${lo}`;
  const from = lo.startsWith("8") ? `dall'${lo}` : `dal ${lo}`;
  const to = hi.startsWith("8") ? `all'${hi}` : `al ${hi}`;
  return `${from} ${to}`;
}
