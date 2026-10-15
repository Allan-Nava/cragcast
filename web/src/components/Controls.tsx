import type { Verdict } from "../types";
import { LABEL, LETTER, VERDICTS, dayLabel } from "../lib/text";
import type { ViewState } from "../lib/state";

interface Props {
  days: string[];
  provinces: { code: string; name: string; count: number }[];
  state: ViewState;
  near: "off" | "asking" | "on";
  count: string;
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

export function Controls({ days, provinces, state, near, count, onDay, onQuery, onToggle, onProvince, onNear,
                          favourites = 0, onlyFavourites = false, onOnlyFavourites }: Props) {
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
        <p id="count" aria-live="polite">{count}</p>
      </div>
    </>
  );
}
