// CG-24: the crag's panel — verdict, reasons, details, the day's hours as two charts and a
// table, directions and OSM links.
// CG-40: a side panel from 900 px, beside the map and the list, which stay usable (a non-modal
// dialog); a bottom sheet on phones (modal). The day switches inside it, by buttons or a
// sideways swipe; a downward swipe closes the sheet. Motion only without
// prefers-reduced-motion.
import { useCallback, useEffect, useRef, useState } from "react";
import type { Crag } from "../types";
import { LABEL, LETTER, allDetails, dayLabel, describe, osmEditUrl, place } from "../lib/text";
import { HourChart, HourTable, hourReadout } from "./HourChart";
import { AvalancheLine } from "./AvalancheLine";
import { cragLink } from "../lib/state";

/** CG-44: share the crag — the system share sheet where there is one, else copy the link. */
function Share({ crag }: { crag: Crag }) {
  const [copied, setCopied] = useState<"ok" | "fail" | null>(null);
  const link = cragLink(location.origin + location.pathname, crag.id);
  const canShare = typeof navigator.share === "function";
  const share = async () => {
    if (canShare) {
      try { await navigator.share({ title: `${crag.name} — cragcast`, url: link }); } catch { /* dismissed */ }
      return;
    }
    try { await navigator.clipboard.writeText(link); setCopied("ok"); }
    catch { setCopied("fail"); }
  };
  return (
    <p className="share">
      <button type="button" className="more" onClick={share}>{canShare ? "Condividi" : "Copia link"}</button>
      <span role="status">
        {copied === "ok" && " Link copiato."}
        {copied === "fail" && <> Copia il link: <a href={link}>{link}</a></>}
      </span>
    </p>
  );
}

interface Props {
  crag: Crag | null;
  day: string;
  /** CG-40: the days of the data, for the switcher; onDay changes the page's day. */
  days?: string[];
  onDay?(day: string): void;
  onClose(): void;
}

const media = (q: string) => typeof matchMedia === "function" && matchMedia(q).matches;
const wide = () => media("(min-width: 900px)");
const calm = () => media("(prefers-reduced-motion: reduce)");
/** dialog.close(), or its effect where the element lacks it (Safari before 15.4, jsdom). */
function shut(d: HTMLDialogElement) {
  if (typeof d.close === "function") { d.close(); return; }
  d.removeAttribute("open");
  d.dispatchEvent(new Event("close"));
}
/** How far a swipe must travel, in px, to count. */
export const SWIPE = 60;

export function DetailDialog({ crag, day, days = [], onDay, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const title = useRef<HTMLHeadingElement>(null);
  const back = useRef<Element | null>(null);
  const start = useRef<{ x: number; y: number } | null>(null);
  const [cursor, setCursor] = useState<number | null>(null);
  const [side, setSide] = useState(false);
  const [leaving, setLeaving] = useState(false);

  /** Close with the slide-out, unless motion is reduced. */
  const close = useCallback(() => {
    const d = ref.current;
    if (!d?.open) return;
    if (calm()) { shut(d); return; }
    setLeaving(true);
    setTimeout(() => { setLeaving(false); shut(d); }, 180);
  }, []);

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    setCursor(null);
    if (crag && !d.open) {
      back.current = document.activeElement;
      const asSide = wide();
      setSide(asSide);
      if (asSide && typeof d.show === "function") d.show();
      else if (!asSide && typeof d.showModal === "function") d.showModal();
      else d.setAttribute("open", "");
      title.current?.focus();
    }
    if (!crag && d.open) shut(d);
  }, [crag]);

  useEffect(() => {                                   // Esc for the non-modal side panel
    if (!crag || !side) return;
    const esc = (e: KeyboardEvent) => { if (e.key === "Escape") close(); };
    document.addEventListener("keydown", esc);
    return () => document.removeEventListener("keydown", esc);
  }, [crag, side, close]);

  const shift = (step: number) => {
    const i = days.indexOf(day) + step;
    if (i >= 0 && i < days.length) onDay?.(days[i]);
  };
  const down = (e: React.PointerEvent) => {
    // The charts read the pointer themselves; a swipe never starts on them.
    start.current = (e.target as Element).closest(".hchart") ? null : { x: e.clientX, y: e.clientY };
  };
  const up = (e: React.PointerEvent) => {
    const s = start.current;
    start.current = null;
    if (!s) return;
    const dx = e.clientX - s.x, dy = e.clientY - s.y;
    if (Math.abs(dx) > SWIPE && Math.abs(dx) > 2 * Math.abs(dy)) shift(dx < 0 ? 1 : -1);
    else if (!side && dy > SWIPE * 1.5 && Math.abs(dy) > 2 * Math.abs(dx) && (ref.current?.scrollTop ?? 0) <= 0) close();
  };

  const info = crag ? describe(crag, day) : null;
  const hours = crag ? crag.days[day].hours ?? [] : [];
  const edit = crag ? osmEditUrl(crag.sourceUrl) : null;

  return (
    <dialog id="detail" ref={ref} aria-labelledby="d-title" className={(side ? "side" : "sheet") + (leaving ? " leaving" : "")}
            onClose={() => { onClose(); (back.current as HTMLElement | null)?.focus?.(); }}
            onCancel={e => { e.preventDefault(); close(); }}
            onPointerDown={down} onPointerUp={up} onPointerCancel={() => { start.current = null; }}>
      {!side && <div className="handle" aria-hidden="true" />}
      <button type="button" className="close" aria-label="Chiudi" onClick={close}>×</button>
      {crag && info && (
        <div key={day}>
          <h2 id="d-title" ref={title} tabIndex={-1}>
            <span className={"mark " + info.verdict.toLowerCase()} aria-hidden="true"><b>{LETTER[info.verdict]}</b></span>
            {` ${crag.name} — ${LABEL[info.verdict]}`}
          </h2>
          {days.length > 1 && (
            <nav className="d-days" aria-label="Giorno">
              {days.map((d, i) => {
                const v = crag.days[d]?.verdict;
                return (
                  <button key={d} type="button" aria-pressed={d === day} onClick={() => onDay?.(d)}>
                    {v && <span className={"mark small " + v.toLowerCase()} aria-hidden="true"><b>{LETTER[v]}</b></span>}
                    {dayLabel(i, d)}
                    {v && <span className="sr-only">{`: ${LABEL[v].toLowerCase()}`}</span>}
                  </button>
                );
              })}
            </nav>
          )}
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
          <Share key={crag.id} crag={crag} />
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
