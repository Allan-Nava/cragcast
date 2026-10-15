interface Props {
  generatedAt: string | null;
  error: string | null;
  now?: number;
}

/** "Aggiornato …", with a warning when the data is older than 3 h (CG-35). */
export function updatedText(generatedAt: string, now: number): { text: string; stale: boolean } {
  const t = new Date(generatedAt);
  const hoursOld = (now - t.getTime()) / 3.6e6;
  // The data is rebuilt every hour; older than 3 h means offline (the service worker served
  // the cached copy) or a failed run. Say so rather than show stale verdicts as current.
  const stale = hoursOld > 3;
  return {
    text: "Aggiornato " + t.toLocaleString("it-IT", { dateStyle: "medium", timeStyle: "short" }) +
      (stale ? ` — dati di ${Math.round(hoursOld)} ore fa: sei offline o l'aggiornamento non è riuscito.` : ""),
    stale,
  };
}

export function Footer({ generatedAt, error, now = Date.now() }: Props) {
  const u = generatedAt ? updatedText(generatedAt, now) : null;
  return (
    <footer>
      <p id="updated" className={u?.stale ? "stale" : undefined}>
        {error ? `Dati non disponibili (${error}).` : u?.text}
      </p>
      <p>Falesie: © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>, ODbL.
         Meteo: <a href="https://open-meteo.com/">Open-Meteo.com</a>, CC BY 4.0.
         Quota: Copernicus DEM GLO-90 (<a href="https://doi.org/10.5270/ESA-c5d3d65">ESA</a>), tramite Open-Meteo.</p>
      <p><a href="api/v1/crags.json">API (JSON)</a> ·{" "}
         <a href="https://github.com/Allan-Nava/cragcast">Codice su GitHub</a> · MIT · un progetto di{" "}
         <a href="https://allan-nava.github.io">Allan Nava</a></p>
    </footer>
  );
}
