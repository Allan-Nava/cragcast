// CG-24: the crag's panel — verdict, reasons, details, the day's hours as two charts and a
// table, directions and OSM links.
import { useEffect, useRef, useState } from "react";
import type { Crag } from "../types";
import { LABEL, LETTER, allDetails, describe, osmEditUrl, place } from "../lib/text";
import { HourChart, HourTable, hourReadout } from "./HourChart";
import { AvalancheLine } from "./AvalancheLine";

interface Props {
  crag: Crag | null;
  day: string;
  onClose(): void;
}

export function DetailDialog({ crag, day, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const [cursor, setCursor] = useState<number | null>(null);

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    setCursor(null);
    if (crag && !d.open && typeof d.showModal === "function") d.showModal();
    if (!crag && d.open) d.close();
  }, [crag]);

  const info = crag ? describe(crag, day) : null;
  const hours = crag ? crag.days[day].hours ?? [] : [];
  const edit = crag ? osmEditUrl(crag.sourceUrl) : null;

  return (
    <dialog id="detail" ref={ref} aria-labelledby="d-title" onClose={onClose}>
      <form method="dialog"><button className="close" aria-label="Chiudi">×</button></form>
      {crag && info && (
        <div>
          <h2 id="d-title">
            <span className={"mark " + info.verdict.toLowerCase()} aria-hidden="true"><b>{LETTER[info.verdict]}</b></span>
            {` ${crag.name} — ${LABEL[info.verdict]}`}
          </h2>
          <p className="muted">{[place(crag), info.aspect, ...info.reasons].filter(Boolean).join(" · ")}</p>
          {allDetails(crag).length > 0 && <p>{allDetails(crag).join(" · ")}</p>}
          {crag.days[day].avalanche && <AvalancheLine a={crag.days[day].avalanche} full />}
          {hours.length > 0 && (
            <>
              <p className="readout" aria-live="polite">
                {cursor == null ? "Passa sul grafico o usa le frecce per leggere un'ora." : hourReadout(hours[cursor])}
              </p>
              <HourChart hours={hours} kind="temperature" cursor={cursor} onCursor={setCursor} />
              <HourChart hours={hours} kind="rain" cursor={cursor} onCursor={setCursor} />
              <HourTable hours={hours} />
            </>
          )}
          <p className="links">
            <a href={`https://www.google.com/maps/dir/?api=1&destination=${crag.latitude},${crag.longitude}`} rel="noopener">Indicazioni (Google Maps)</a>{" "}
            <a href={`https://www.openstreetmap.org/directions?route=%3B${crag.latitude}%2C${crag.longitude}`} rel="noopener">Indicazioni (OSM)</a>{" "}
            <a href={crag.sourceUrl} rel="noopener">Su OpenStreetMap</a>{" "}
            {edit && <a href={edit} rel="noopener">Modifica su OSM</a>}
          </p>
        </div>
      )}
    </dialog>
  );
}
