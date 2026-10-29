import { useState } from "react";
import type { Verdict } from "../types";
import { LABEL, LETTER, VERDICTS, dayLabel } from "../lib/text";
import { ALL_REGIONS, REGIONS, type Order, type ViewState } from "../lib/state";
import { GRADE_CHOICES, gradeLabel, gradeParam, readGrade, type GradeFilter } from "../lib/grades";

/** CG-61: the orders offered; "distance" only while the position is known. */
const ORDER_LABEL: Record<Order | "distance", string> = {
  verdict: "Per verdetto", name: "Per nome", low: "Dalle più basse", high: "Dalle più alte", distance: "Per distanza",
};

interface Props {
  days: string[];
  provinces: { code: string; name: string; count: number }[];
  /** CG-43: the regions in the data; any beyond REGIONS are added to the choice. */
  regions?: { name: string; count: number }[];
  onRegion?(region: string): void;
  state: ViewState;
  near: "off" | "asking" | "on";
  /** CG-53: how many filters are on, for the "Filtri" button that holds them on a phone. */
  active?: number;
  onDay(day: string): void;
  onQuery(q: string): void;
  onToggle(v: Verdict): void;
  onProvince(code: string): void;
  /** CG-61: the grade filter and the order; byDistance when "Vicino a me" orders the list. */
  onGrade?(grade: GradeFilter | null): void;
  onOrder?(order: Order | "distance"): void;
  byDistance?: boolean;
  onNear(): void;
  /** CG-46: how many favourites, and the filter on them. */
  favourites?: number;
  onlyFavourites?: boolean;
  onOnlyFavourites?(): void;
}

export function Controls({ days, provinces, state, near, active = 0, onDay, onQuery, onToggle, onProvince, onNear,
                          favourites = 0, onlyFavourites = false, onOnlyFavourites,
                          onGrade, onOrder, byDistance = false, regions = [], onRegion }: Props) {
  const ferrate = state.kind === "ferrate";
  // CG-53: on a phone the filters fold behind one button; from 600 px they stay in the row and
  // the button is hidden (style.css).
  const [open, setOpen] = useState(false);
  return (
    <>
      {/* CG-127: the three days are there from the first paint, waiting for the data's dates, so
          the row does not appear with the data and push the page down (CLS). */}
      <nav className="days" aria-label="Giorno">
        {(days.length ? days : ["", "", ""]).map((day, i) => (
          <button key={day || i} type="button" aria-pressed={!!day && day === state.day} disabled={!day}
                  onClick={() => onDay(day)}
                  title={day ? new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" }) : undefined}>
            {dayLabel(i, day)}
          </button>
        ))}
      </nav>
      <div className="controls">
        <label className="search">
          <span className="sr-only">{ferrate ? "Cerca una via ferrata" : "Cerca una falesia"}</span>
          <input id="q" type="search" placeholder={ferrate ? "Cerca una via ferrata…" : "Cerca una falesia…"} autoComplete="off" spellCheck={false}
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
        {/* CG-114: a via ferrata has no province or grades from OSM: those filters are for crags. */}
        {/* CG-121: the via ferratas have regions too. CG-43: the same options from the first paint, without counts: a select is as wide as its
            longest option, and one that widened when the data arrived wrapped the row and pushed
            the page down (Lighthouse CLS 0.8). The summary says how many. */}
        <label className="area">
          <span className="sr-only">Regione</span>
          <select id="region" value={state.region} onChange={e => onRegion?.(e.target.value)}>
            {[...new Set([...REGIONS, ...regions.map(r => r.name)])].map(name =>
              <option key={name} value={name}>{name}</option>)}
            <option value={ALL_REGIONS}>Tutte le regioni</option>
          </select>
        </label>
        {!ferrate && <>
        <label className="area">
          <span className="sr-only">Provincia</span>
          <select id="province" value={state.province} onChange={e => onProvince(e.target.value)}>
            <option value="">Tutte le province</option>
            {provinces.map(p => <option key={p.code} value={p.code}>{`${p.name} (${p.count})`}</option>)}
          </select>
        </label>
        <label className="area">
          <span className="sr-only">Gradi</span>
          <select id="grade" value={state.grade ? gradeParam(state.grade) : ""}
                  title="Le falesie di cui OpenStreetMap non dà i gradi restano fuori: il conteggio dice quante."
                  onChange={e => onGrade?.(readGrade(e.target.value))}>
            <option value="">Tutti i gradi</option>
            {GRADE_CHOICES.map(g => <option key={gradeParam(g)} value={gradeParam(g)}>{gradeLabel(g)}</option>)}
          </select>
        </label>
        </>}
        <label className="area">
          <span className="sr-only">Ordine</span>
          <select id="order" value={byDistance ? "distance" : state.order}
                  onChange={e => onOrder?.(e.target.value as Order | "distance")}>
            {(["verdict", "name", "low", "high"] as const).map(o => <option key={o} value={o}>{ORDER_LABEL[o]}</option>)}
            {near === "on" && <option value="distance">{ORDER_LABEL.distance}</option>}
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
