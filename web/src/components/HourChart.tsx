// One measure per chart, never two y-axes (CG-24). Rain #3987e5 and temperature #d95926 are
// validated on the panel surface #111418 (dataviz validator: CVD ΔE 26.8, ≥3:1) and are not
// verdict colours. The readout and the table carry every value the chart shows.
import type { Hour } from "../types";
import { ticks } from "../lib/ticks";

export const RAIN = "#3987e5";
export const TEMP = "#d95926";
const W = 320, H = 116, L = 30, R = 8, T = 14, B = 18;
const fmt = (n: number, d = 0) => n.toLocaleString("it-IT", { minimumFractionDigits: d, maximumFractionDigits: d });

interface Props {
  hours: Hour[];
  kind: "temperature" | "rain";
  cursor: number | null;
  onCursor(i: number): void;
}

export function HourChart({ hours, kind, cursor, onCursor }: Props) {
  const isRain = kind === "rain";
  const title = isRain ? "Pioggia (mm per ora)" : "Temperatura (°C)";
  const vals = hours.map(h => (isRain ? h.rainMm : h.temperatureC));
  const { lo, hi, ticks: tk } = ticks(Math.min(...vals), Math.max(...vals), isRain);
  const band = (W - L - R) / hours.length;
  const x = (i: number) => L + band * (i + 0.5);
  const y = (v: number) => T + (H - T - B) * (1 - (v - lo) / (hi - lo));
  const index = (e: React.PointerEvent<SVGSVGElement>) => {
    const r = e.currentTarget.getBoundingClientRect();
    const vx = (e.clientX - r.left) / r.width * W;
    return Math.max(0, Math.min(hours.length - 1, Math.floor((vx - L) / band)));
  };
  const step = (e: React.KeyboardEvent) => {
    const cur = cursor ?? hours.findIndex(h => h.hour === 12);
    if (e.key === "ArrowRight") { onCursor(Math.min(hours.length - 1, cur + 1)); e.preventDefault(); }
    if (e.key === "ArrowLeft") { onCursor(Math.max(0, cur - 1)); e.preventDefault(); }
  };
  const iMax = vals.indexOf(Math.max(...vals));
  const bw = Math.min(24, band - 2);                // ≤24 px, 2 px surface gap

  return (
    <figure className="hchart">
      <figcaption>{title}</figcaption>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`${title}, ora per ora: vedi la tabella`} tabIndex={0}
           onPointerMove={e => onCursor(index(e))} onFocus={() => onCursor(cursor ?? Math.max(0, hours.findIndex(h => h.hour === 12)))}
           onKeyDown={step}>
        {tk.map(v => (
          <g key={v}>
            <line className="grid" x1={L} x2={W - R} y1={y(v)} y2={y(v)} />
            <text className="tick" x={L - 4} y={y(v) + 3} textAnchor="end">{fmt(v)}</text>
          </g>
        ))}
        {hours.map((h, i) => h.hour % 3 === 0 &&
          <text key={h.hour} className="tick" x={x(i)} y={H - 4} textAnchor="middle">{h.hour}</text>)}
        {isRain ? (
          <>
            {hours.map((h, i) => {
              if (h.rainMm <= 0) return null;
              const top = y(h.rainMm), base = y(0), r = Math.min(4, (base - top) / 2), x0 = x(i) - bw / 2;
              return <path key={h.hour} fill={RAIN}
                d={`M${x0},${base}V${top + r}Q${x0},${top} ${x0 + r},${top}H${x0 + bw - r}Q${x0 + bw},${top} ${x0 + bw},${top + r}V${base}Z`} />;
            })}
            {vals.every(v => v <= 0) &&
              <text className="note" x={(L + W - R) / 2} y={T + (H - T - B) / 2} textAnchor="middle">nessuna pioggia prevista</text>}
          </>
        ) : (
          <>
            <polyline points={hours.map((h, i) => `${x(i)},${y(h.temperatureC)}`).join(" ")} fill="none" stroke={TEMP}
                      strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
            <circle cx={x(iMax)} cy={y(vals[iMax])} r={4} fill={TEMP} stroke="#111418" strokeWidth={2} />
            {/* Near an edge the label leans inwards, so it is never cut (CG-63 found "max 16" clipped). */}
            <text className="label" x={x(iMax)} y={y(vals[iMax]) - 7}
                  textAnchor={x(iMax) > W - R - 28 ? "end" : x(iMax) < L + 28 ? "start" : "middle"}>{`max ${fmt(vals[iMax])}°`}</text>
          </>
        )}
        {cursor != null && <line className="cross" x1={x(cursor)} x2={x(cursor)} y1={T} y2={H - B} />}
      </svg>
    </figure>
  );
}

export function hourReadout(h: Hour): string {
  return `ore ${h.hour}: ${fmt(h.temperatureC)} °C · pioggia ${fmt(h.rainMm, 1)} mm · vento ${fmt(h.windKmh)} km/h`;
}

export function HourTable({ hours }: { hours: Hour[] }) {
  return (
    <details className="hours">
      <summary>Tabella ora per ora</summary>
      <table>
        <thead><tr><th scope="col">Ora</th><th scope="col">°C</th><th scope="col">Pioggia mm</th><th scope="col">Vento km/h</th></tr></thead>
        <tbody>
          {hours.map(h => (
            <tr key={h.hour}><td>{h.hour}</td><td>{fmt(h.temperatureC)}</td><td>{fmt(h.rainMm, 1)}</td><td>{fmt(h.windKmh)}</td></tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
