import { useEffect } from "react";
import { Star } from "./Star";
import type { Crag } from "../types";
import { LABEL, LETTER, allDetails, aspectShort, describe, place } from "../lib/text";

interface Props {
  crags: Crag[];
  day: string;
  /** CG-39: every day of the data, for the three verdicts in each row. */
  days?: string[];
  onDay?(day: string): void;
  distance: ((c: Crag) => number) | null;
  onOpen(crag: Crag): void;
  onDetail(crag: Crag): void;
  /** CG-38: the crag under the pointer or focus, so the map can light its mark. */
  onHot?(id: string | null): void;
  chosen?: string | null;
  /** CG-46 */
  favourites?: Set<string>;
  onFavourite?(id: string): void;
  /** What to say when nothing is shown, instead of the search message. */
  empty?: string;
}

export function CragList({ crags, day, days = [], onDay, distance, onOpen, onDetail, onHot, chosen, favourites, onFavourite,
                          empty }: Props) {
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
            {/* CG-58: compact — the star sits beside the heading, not in it, so the heading reads as
                the crag; the days and "Dettagli" share the last row. */}
            <div className="head">
              <h2>
                {/* the verdict is in the text next to the mark */}
                <span className={"mark small " + info.verdict.toLowerCase()} aria-hidden="true"><b>{LETTER[info.verdict]}</b></span>
                {/* CG-114: a via ferrata's own mark beside the verdict's, and the word in the meta line */}
                {crag.ferrata && <i className="peak" aria-hidden="true" />}
                {`${crag.name} — ${LABEL[info.verdict]}`}
                {distance && <span className="km">{`${Math.round(distance(crag))} km`}</span>}
              </h2>
              {onFavourite && <Star name={crag.name} on={!!favourites?.has(crag.id)} onToggle={() => onFavourite(crag.id)} />}
            </div>
            <p className="meta">
              {crag.ferrata && <span className="tag kind-tag">via ferrata</span>}
              {[place(crag), aspectShort(crag)].filter(Boolean).join(" · ")}
              {!crag.aspect && !crag.ferrata && <>{place(crag) ? " · " : ""}<span className="tag" title="Esposizione non mappata su OpenStreetMap: valutata come parete in ombra">
                esposizione ?<span className="sr-only"> (non mappata: valutata come parete in ombra)</span></span></>}
              {info.reasons.length > 0 && ` · ${info.reasons.join(" · ")}`}
              {details.length > 0 && <> · <span className="details">{details.join(" · ")}</span></>}
            </p>
            <div className="row">
              {days.length > 1 && <DayStrip crag={crag} days={days} day={day} onDay={onDay} />}
              {/* In a narrow list the word yields to "›" (style.css); the name stays "Dettagli". */}
              <button type="button" className="more" onClick={e => { e.stopPropagation(); onDetail(crag); }}>
                <span className="more-text">Dettagli</span><span className="more-icon" aria-hidden="true">›</span>
              </button>
            </div>
          </li>
        );
      })}
      {crags.length === 0 && <li className="empty">{empty ?? "Nessuna falesia corrisponde alla ricerca."}</li>}
    </ul>
  );
}

const short = (day: string) => new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "short" });
const long = (day: string) => new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric" });

/**
 * CG-39: the three days of one crag side by side — verdict mark, day, max temperature, and the
 * rain of the day when there is any (a drop and the mm; the data has no cloud cover, so no sun
 * is drawn). Each is a button that switches the page to that day.
 */
function DayStrip({ crag, days, day, onDay }: { crag: Crag; days: string[]; day: string; onDay?(d: string): void }) {
  return (
    <div className="strip" role="group" aria-label={`${crag.name}: i giorni`}>
      {days.map(d => {
        const v = crag.days[d];
        if (!v) return null;
        const rain = v.weather.rainDuringDayMm;
        const wet = rain >= 0.5;
        const label = `${long(d)}: ${LABEL[v.verdict].toLowerCase()}, massima ${Math.round(v.weather.maxTemperatureC)} °C` +
          (wet ? `, pioggia ${rain.toFixed(1).replace(".", ",")} mm` : "");
        // The name starts with what is seen ("ven 19°"), so voice control can say it (WCAG 2.5.3);
        // the full reading follows for screen readers.
        return (
          <button key={d} type="button" className="day" aria-pressed={d === day} title={label}
                  onClick={e => { e.stopPropagation(); onDay?.(d); }}>
            <span className={"mark small " + v.verdict.toLowerCase()} aria-hidden="true"><b>{LETTER[v.verdict]}</b></span>
            <span>{short(d)}</span>
            <span className="t">{`${Math.round(v.weather.maxTemperatureC)}°`}</span>
            {wet && (
              <span className="rain">
                <svg viewBox="0 0 10 14" width="8" height="11" aria-hidden="true"><path d="M5 0C5 0 0 6.5 0 9.2A5 5 0 0 0 10 9.2C10 6.5 5 0 5 0Z" fill="#3987e5" /></svg>
                {Math.round(rain)}
              </span>
            )}
            <span className="sr-only">{` — ${label}`}</span>
          </button>
        );
      })}
    </div>
  );
}
