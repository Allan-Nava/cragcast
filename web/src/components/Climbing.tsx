// CG-117: the crag's sectors (CG-104) and single routes (CG-105) from OpenStreetMap, in the
// panel, each folded away under its count so the panel stays short. Routes come easiest first,
// as the export sorts them; each name links to its object on OSM.
import type { Crag, Route, Sector } from "../types";
import { ASPECT_IT, osmUrl } from "../lib/text";

const range = (s: Pick<Sector, "gradeMin" | "gradeMax">): string | null =>
  s.gradeMin && s.gradeMax ? (s.gradeMin === s.gradeMax ? s.gradeMin : `${s.gradeMin}–${s.gradeMax}`)
    : s.gradeMin ? `da ${s.gradeMin}` : s.gradeMax ? `fino a ${s.gradeMax}` : null;

const many = (n: number, one: string, more: string) => `${n} ${n === 1 ? one : more}`;

function SectorList({ sectors }: { sectors: Sector[] }) {
  return (
    <details className="climbing">
      <summary>{many(sectors.length, "settore", "settori")} su OpenStreetMap</summary>
      <ul>
        {sectors.map(s => (
          <li key={s.id}>
            <a href={osmUrl(s.id)} rel="noopener">{s.name ?? "Settore senza nome"}</a>
            {[s.aspect ? `esposto a ${ASPECT_IT[s.aspect]}` : null, range(s)].filter(Boolean).map(t => ` · ${t}`).join("")}
          </li>
        ))}
      </ul>
    </details>
  );
}

function RouteTable({ routes, sectors }: { routes: Route[]; sectors: Sector[] }) {
  const sectorName = new Map(sectors.map(s => [s.id, s.name]));
  const bySector = routes.some(r => r.sector && sectorName.get(r.sector));
  return (
    <details className="climbing">
      <summary>{many(routes.length, "via", "vie")} su OpenStreetMap</summary>
      <table className="routes">
        <thead>
          <tr>
            <th scope="col">Via</th>
            <th scope="col">Grado</th>
            <th scope="col">Lunghezza</th>
            <th scope="col">Spit</th>
            {bySector && <th scope="col">Settore</th>}
          </tr>
        </thead>
        <tbody>
          {routes.map(r => (
            <tr key={r.id}>
              <th scope="row"><a href={osmUrl(r.id)} rel="noopener">{r.name ?? "Senza nome"}</a>
                {r.pitches != null && r.pitches > 1 && <span className="muted">{` · ${r.pitches} tiri`}</span>}</th>
              <td>{r.grade ?? "—"}</td>
              <td>{r.length != null ? `${r.length.toLocaleString("it-IT")} m` : "—"}</td>
              <td>{r.bolts ?? "—"}</td>
              {bySector && <td>{(r.sector && sectorName.get(r.sector)) ?? "—"}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}

export function Climbing({ crag }: { crag: Crag }) {
  const sectors = crag.sectors ?? [];
  const routes = crag.routes ?? [];
  if (!sectors.length && !routes.length) return null;
  return (
    <>
      {sectors.length > 0 && <SectorList sectors={sectors} />}
      {routes.length > 0 && <RouteTable routes={routes} sectors={sectors} />}
    </>
  );
}
