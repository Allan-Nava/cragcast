import type { Crag } from "../types";
import { LABEL, LETTER, allDetails, describe, place } from "../lib/text";

interface Props {
  crags: Crag[];
  day: string;
  distance: ((c: Crag) => number) | null;
  onOpen(crag: Crag): void;
  onDetail(crag: Crag): void;
}

export function CragList({ crags, day, distance, onOpen, onDetail }: Props) {
  return (
    <ul id="list">
      {crags.map(crag => {
        const info = describe(crag, day);
        const details = allDetails(crag);
        return (
          <li key={crag.id} tabIndex={0} onClick={() => onOpen(crag)} onKeyDown={e => { if (e.key === "Enter") onOpen(crag); }}>
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
