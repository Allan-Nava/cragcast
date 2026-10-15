import { useState } from "react";
import type { Verdict } from "../types";
import { LABEL, LETTER, VERDICTS, dayLabel } from "../lib/text";
import type { ViewState } from "../lib/state";

interface Props {
  days: string[];
  provinces: { code: string; name: string; count: number }[];
  state: ViewState;
  near: "off" | "asking" | "on";
  /** CG-53: how many filters are on, for the "Filtri" button that holds them on a phone. */
  active?: number;
  onDay(day: string): void;
  onQuery(q: string): void;
  onToggle(v: Verdict): void;
  onProvince(code: string): void;
  onNear(): void;
  /** CG-46: how many favourites, and the filter on them. */
  favourites?: number;
  onlyFavourites?: boolean;
  onOnlyFavourites?(): void;
}

export function Controls({ days, provinces, state, near, active = 0, onDay, onQuery, onToggle, onProvince, onNear,
                          favourites = 0, onlyFavourites = false, onOnlyFavourites }: Props) {
  // CG-53: on a phone the filters fold behind one button; from 600 px they stay in the row and
  // the button is hidden (style.css).
  const [open, setOpen] = useState(false);
  return (
    <>
      <nav className="days" aria-label="Giorno">
        {days.map((day, i) => (
          <button key={day} type="button" aria-pressed={day === state.day} onClick={() => onDay(day)}
                  title={new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" })}>
            {dayLabel(i, day)}
          </button>
        ))}
      </nav>
      <div className="controls">
        <label className="search">
          <span className="sr-only">Cerca una falesia</span>
          <input id="q" type="search" placeholder="Cerca una falesia…" autoComplete="off" spellCheck={false}
                 value={state.q} onChange={e => onQuery(e.target.value)} />
        </label>
        <button type="button" className="filters-toggle" aria-expanded={open} aria-controls="filters"
                onClick={() => setOpen(o => !o)}>
          {active ? `Filtri (${active})` : "Filtri"}
        </button>
        <div id="filters" className="filters" data-open={open}>
        <div className="legend" role="group" aria-label="Mostra per verdetto">
          {VERDICTS.map(v => (
            <button key={v} type="button" aria-pressed={state.verdicts.has(v)} onClick={() => onToggle(v)}>
              <span className={"mark small " + v.toLowerCase()} aria-hidden="true"><b>{LETTER[v]}</b></span>{LABEL[v]}
            </button>
          ))}
        </div>
        <label className="area">
          <span className="sr-only">Provincia</span>
          <select id="province" value={state.province} onChange={e => onProvince(e.target.value)}>
            <option value="">Tutte le province</option>
            {provinces.map(p => <option key={p.code} value={p.code}>{`${p.name} (${p.count})`}</option>)}
          </select>
        </label>
        <button type="button" id="near" aria-pressed={near === "on"} onClick={onNear}
                title="Ordina per distanza da te. La posizione resta nel browser: non viene inviata né salvata.">
          {near === "asking" ? "Cerco la posizione…" : "Vicino a me"}
        </button>
        <button type="button" id="favs" aria-pressed={onlyFavourites} onClick={onOnlyFavourites}
                title="Le preferite restano in questo browser: non vengono inviate.">
          {`Preferite (${favourites})`}
        </button>
        </div>
      </div>
    </>
  );
}
