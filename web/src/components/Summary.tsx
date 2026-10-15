// CG-39: the day's verdicts counted over the crags that match the search, province and area —
// every verdict, so the toggles never hide what they filter out. Marks and words, never
// colour alone.
import type { Crag, Verdict } from "../types";
import { LABEL, LETTER, VERDICTS, dayLabel } from "../lib/text";

/** The day's verdicts, then (CG-53) the count of crags shown, on one line. */
export function Summary({ crags, day, index, count }: { crags: Crag[]; day: string; index: number; count?: string }) {
  const n: Record<Verdict, number> = { GO: 0, MAYBE: 0, NO_GO: 0 };
  crags.forEach(c => n[c.days[day].verdict]++);
  const date = new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" });
  return (
    <p className="summary">
      <strong>{dayLabel(index, day)}<span className="date">{`, ${date}`}</span>:</strong>
      {VERDICTS.map(v => (
        <span key={v} className="count">
          <span className={"mark small " + v.toLowerCase()} aria-hidden="true"><b>{LETTER[v]}</b></span>
          {`${n[v]} ${LABEL[v].toLowerCase()}`}
        </span>
      ))}
      {count != null && <span id="count" aria-live="polite">{count}</span>}
    </p>
  );
}
