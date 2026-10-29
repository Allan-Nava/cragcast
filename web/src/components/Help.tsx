// CG-26: how to add the missing data to OpenStreetMap — first-hand only.
// CG-130: and how the verdict is made, from the scorers' numbers (lib/rules.ts).
import { CRAG, FERRATA } from "../lib/rules";

const n = (x: number) => x.toLocaleString("it-IT");

/** CG-130: what "si va / forse / no" are measured against, in words, the numbers from the scorers. */
export function HowWeDecide() {
  return (
    <details>
      <summary>Come decidiamo il verdetto</summary>
      <p>Il giudizio è una stima dal meteo previsto (Open-Meteo), non un controllo sul posto.</p>
      <p><strong>Falesie</strong> — guardiamo le ore tra le {CRAG.climbFromHour} e le {CRAG.climbToHour}.</p>
      <ul>
        <li><strong>No</strong> se in quelle ore piovono più di {n(CRAG.rainDuringDayNoGoMm)}&nbsp;mm o il vento
            supera i {n(CRAG.windNoGoKmh)}&nbsp;km/h.</li>
        <li><strong>Forse</strong> se nelle 48 ore prima sono caduti più di {n(CRAG.recentRainSunnyMm)}&nbsp;mm su
            una parete al sole (S, SE, SO) — la metà, {n(CRAG.recentRainSunnyMm / 2)}&nbsp;mm, su una parete in
            ombra o di esposizione non mappata —, se la massima resta sotto i {n(CRAG.tooColdC)}&nbsp;°C, o se
            supera i {n(CRAG.tooHotC)}&nbsp;°C su una parete al sole.</li>
        <li><strong>Si va</strong> altrimenti.</li>
      </ul>
      <p><strong>Vie ferrate</strong> — il meteo in cima, tra le {FERRATA.fromHour} e le {FERRATA.toHour}: più
         severo, perché un errore costa di più.</p>
      <ul>
        <li><strong>Mai &laquo;si va&raquo;</strong> con un temporale previsto anche in una sola ora.</li>
        <li><strong>No</strong> con aria molto instabile (CAPE da {n(FERRATA.capeNoGoJkg)}&nbsp;J/kg), più di
            {" "}{n(FERRATA.rainDuringDayNoGoMm)}&nbsp;mm di pioggia, neve fresca da {n(FERRATA.freshSnowNoGoCm)}&nbsp;cm,
            pioggia (oltre {n(FERRATA.rainBeforeFrostMm)}&nbsp;mm) e poi gelo (da {n(FERRATA.frostC)}&nbsp;°C) che
            fanno ghiaccio, raffiche oltre i {n(FERRATA.gustNoGoKmh)}&nbsp;km/h.</li>
        <li><strong>Forse</strong> con un po' di instabilità (da {n(FERRATA.capeMaybeJkg)}&nbsp;J/kg), vento oltre
            i {n(FERRATA.windMaybeKmh)}&nbsp;km/h, più di {n(FERRATA.recentRainMaybeMm)}&nbsp;mm di pioggia nelle 48
            ore prima, una cima sotto i {n(FERRATA.coldMaybeC)}&nbsp;°C o sopra i {n(FERRATA.hotMaybeC)}&nbsp;°C, meno
            di {n(FERRATA.shortDayH)} ore di luce.</li>
      </ul>
      <p>Il bollettino valanghe informa e non cambia il verdetto. Le soglie sono una prima stima: le
         correggeremo con giornate osservate sul posto.</p>
    </details>
  );
}

export function Help() {
  return (
    <section className="help">
      <HowWeDecide />
      <details>
        <summary>Aiuta a mappare la tua falesia</summary>
        <p>cragcast prende le falesie da OpenStreetMap. Se conosci una falesia, puoi aggiungere tu
           i dati che mancano: aprila sulla mappa, clicca <em>modifica su OpenStreetMap</em> e
           aggiungi questi tag.</p>
        <ul>
          <li><code>climbing:orientation</code> — dove guarda la parete: N, NE, E, SE, S, SW, W o NW.
              È il dato che rende il verdetto più preciso: senza, la falesia porta l'etichetta
              <em>esposizione ?</em> ed è valutata come parete in ombra, la lettura prudente.</li>
          <li><code>climbing:grade:french:min</code> e <code>climbing:grade:french:max</code> — grado più facile e più difficile, es. 5a e 7b.</li>
          <li><code>climbing:length:max</code> — lunghezza delle vie più lunghe, in metri.</li>
          <li><code>climbing:rock</code> — es. limestone (calcare), granite (granito), gneiss.</li>
          <li>Per una via ferrata (CG-114): <code>via_ferrata_scale</code> — la difficoltà, un numero intero
              da 0 a 6, senza <em>+</em>. Senza, la via ferrata dice <em>difficoltà non mappata</em>.</li>
        </ul>
        <p><strong>Solo quello che conosci di persona:</strong> OpenStreetMap non accetta dati copiati
           da guide o siti protetti da copyright. Preferisci che lo faccia qualcun altro?{" "}
           <a href="https://github.com/Allan-Nava/cragcast/issues/new?template=crag-data.yml">Segnalalo su GitHub</a>.</p>
        <p>Le modifiche arrivano su cragcast con l'aggiornamento mensile dell'elenco delle falesie.</p>
      </details>
    </section>
  );
}
