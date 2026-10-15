// CG-26: how to add the missing data to OpenStreetMap — first-hand only.
export function Help() {
  return (
    <section className="help">
      <details>
        <summary>Aiuta a mappare la tua falesia</summary>
        <p>cragcast prende le falesie da OpenStreetMap. Se conosci una falesia, puoi aggiungere tu
           i dati che mancano: aprila sulla mappa, clicca <em>modifica su OpenStreetMap</em> e
           aggiungi questi tag.</p>
        <ul>
          <li><code>climbing:orientation</code> — dove guarda la parete: N, NE, E, SE, S, SW, W o NW.
              È il dato che rende il verdetto più preciso.</li>
          <li><code>climbing:grade:french:min</code> e <code>climbing:grade:french:max</code> — grado più facile e più difficile, es. 5a e 7b.</li>
          <li><code>climbing:length:max</code> — lunghezza delle vie più lunghe, in metri.</li>
          <li><code>climbing:rock</code> — es. limestone (calcare), granite (granito), gneiss.</li>
        </ul>
        <p><strong>Solo quello che conosci di persona:</strong> OpenStreetMap non accetta dati copiati
           da guide o siti protetti da copyright. Preferisci che lo faccia qualcun altro?{" "}
           <a href="https://github.com/Allan-Nava/cragcast/issues/new?template=crag-data.yml">Segnalalo su GitHub</a>.</p>
        <p>Le modifiche arrivano su cragcast con l'aggiornamento mensile dell'elenco delle falesie.</p>
      </details>
    </section>
  );
}
