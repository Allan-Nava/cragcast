import { useEffect } from "react";
import type { Crag } from "../types";
import { LABEL, LETTER, allDetails, describe, place } from "../lib/text";

interface Props {
  crags: Crag[];
  day: string;
  distance: ((c: Crag) => number) | null;
  onOpen(crag: Crag): void;
  onDetail(crag: Crag): void;
  /** CG-38: the crag under the pointer or focus, so the map can light its mark. */
  onHot?(id: string | null): void;
  chosen?: string | null;
}

export function CragList({ crags, day, distance, onOpen, onDetail, onHot, chosen }: Props) {
  // A crag chosen on the map comes into view in the list — where the list scrolls by itself
  // (side by side from 900 px); on a phone that would yank the page away from the map.
  useEffect(() => {
    if (!chosen || typeof matchMedia !== "function" || !matchMedia("(min-width: 900px)").matches) return;
    document.getElementById("crag-" + chosen)?.scrollIntoView({ block: "nearest" });
  }, [chosen]);
  return (
    <ul id="list">
      {crags.map(crag => {
        const info = describe(crag, day);
        const details = allDetails(crag);
        return (
          <li key={crag.id} id={"crag-" + crag.id} tabIndex={0} className={crag.id === chosen ? "chosen" : undefined}
              aria-current={crag.id === chosen ? "true" : undefined}
              onClick={() => onOpen(crag)} onKeyDown={e => { if (e.key === "Enter") onOpen(crag); }}
              onMouseEnter={() => onHot?.(crag.id)} onMouseLeave={() => onHot?.(null)}
              onFocus={e => { if (e.target === e.currentTarget) onHot?.(crag.id); }} onBlur={() => onHot?.(null)}>
            <h2>
              {/* the verdict is in the text next to the mark */}
              <span className={"mark small " + info.verdict.toLowerCase()} aria-hidden="true"><b>{LETTER[info.verdict]}</b></span>
              {`${crag.name} — ${LABEL[info.verdict]}`}
              {distance && <span className="km">{`${Math.round(distance(crag))} km`}</span>}
            </h2>
            <p>{[place(crag), info.aspect, ...info.reasons].filter(Boolean).join(" · ")}</p>
            {details.length > 0 && <p className="details">{details.join(" · ")}</p>}
            <button type="button" className="more" onClick={e => { e.stopPropagation(); onDetail(crag); }}>Dettagli</button>
          </li>
        );
      })}
      {crags.length === 0 && <li className="empty">Nessuna falesia corrisponde alla ricerca.</li>}
    </ul>
  );
}
