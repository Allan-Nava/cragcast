import { useEffect, useState } from "react";

interface Props {
  generatedAt: string | null;
  error: string | null;
  now?: number;
  /** The release this page was built for (vYYYY.MM.DD), set only in a release's image. */
  version?: string;
}

/** The release in the page's build, from the image's build argument; none on GitHub Pages. */
const BUILT_VERSION: string | undefined = import.meta.env.VITE_CRAGCAST_VERSION || undefined;

/** CG-41: "adesso", "12 min fa", "2 ore fa". */
export function ago(ms: number): string {
  const min = Math.max(0, Math.floor(ms / 60000));
  if (min < 1) return "adesso";
  if (min < 60) return `${min} min fa`;
  const h = Math.floor(min / 60);
  return h === 1 ? "1 ora fa" : `${h} ore fa`;
}

/** Two missed 3-hourly runs and a margin (CG-43; it was 3 h while the data was hourly). */
export const STALE_HOURS = 7;

/** "Aggiornato 12 min fa (…)", with a warning when the data is older than STALE_HOURS (CG-35). */
export function updatedText(generatedAt: string, now: number): { text: string; stale: boolean } {
  const t = new Date(generatedAt);
  const hoursOld = (now - t.getTime()) / 3.6e6;
  // The data is rebuilt every 3 hours (CG-43); older than STALE_HOURS means offline (the service
  // worker served the cached copy) or two failed runs. Say so rather than show stale verdicts as current.
  const stale = hoursOld > STALE_HOURS;
  return {
    text: `Aggiornato ${ago(now - t.getTime())} (${t.toLocaleString("it-IT", { dateStyle: "medium", timeStyle: "short" })})` +
      (stale ? " — sei offline o l'aggiornamento non è riuscito." : ""),
    stale,
  };
}

export function Footer({ generatedAt, error, now, version = BUILT_VERSION }: Props) {
  // CG-41: the age keeps itself current — every 30 s is enough for minutes.
  const [tick, setTick] = useState(() => Date.now());
  useEffect(() => {
    if (now != null) return;
    const t = setInterval(() => setTick(Date.now()), 30000);
    return () => clearInterval(t);
  }, [now]);
  const u = generatedAt ? updatedText(generatedAt, now ?? tick) : null;
  return (
    <footer>
      <p id="updated" className={u?.stale ? "stale" : undefined}>
        {error ? `Dati non disponibili (${error}).` : u?.text}
      </p>
      <p>Falesie: © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>, ODbL.
         Meteo: <a href="https://open-meteo.com/">Open-Meteo.com</a>, CC BY 4.0.
         Quota: Copernicus DEM GLO-90 (<a href="https://doi.org/10.5270/ESA-c5d3d65">ESA</a>), tramite Open-Meteo.
         Valanghe, in stagione: bollettini di <a href="https://www.arpalombardia.it/temi-ambientali/neve-e-ghiacciai/bollettino-neve-e-valanghe/">ARPA
         Lombardia</a>, <a href="https://bollettini.aineva.it/bulletin/latest">AINEVA Piemonte</a> e dell'<a
         href="https://avalanche.report/bulletin/latest">Euregio</a> per Trentino e Alto Adige, tramite EAWS.</p>
      <p><a href="api/v1/crags.json">API (JSON)</a> ·{" "}
         <a href="https://github.com/Allan-Nava/cragcast">Codice su GitHub</a> · MIT · un progetto di{" "}
         <a href="https://allan-nava.github.io">Allan Nava</a>
         {version && <> · <a href={`https://github.com/Allan-Nava/cragcast/releases/tag/${version}`}>{`versione ${version}`}</a></>}</p>
    </footer>
  );
}
