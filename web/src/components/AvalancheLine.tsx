// CG-11: the avalanche bulletin next to the weather, when the crag lies in a covered zone and a
// bulletin is out for the day. The facts only — levels, bands, problem types — and the link
// to the official bulletin, which is the one to read before going.
import type { Avalanche } from "../types";
import { dangerLabel, problemParts, ratingParts, zoneName } from "../lib/text";

export function AvalancheLine({ a, full }: { a: Avalanche; full: boolean }) {
  return (
    <p className="avalanche">
      <strong>{`Valanghe: ${dangerLabel(a.level)}`}</strong>
      {` — zona ${zoneName(a)}`}
      {full && `: ${ratingParts(a).join("; ")}`}
      {full && a.problems.length > 0 && `. Problemi: ${problemParts(a).join(", ")}`}
      {". "}
      <a href={a.bulletin} rel="noopener">{`Bollettino ${a.provider ?? "ARPA Lombardia"}`}</a>
    </p>
  );
}
