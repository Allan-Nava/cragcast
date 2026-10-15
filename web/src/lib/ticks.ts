/** Clean axis ticks: 0/5/10… (or by 10 over a wide range) for temperature, 0 and a round top for rain. */
export function ticks(min: number, max: number, isRain: boolean): { lo: number; hi: number; ticks: number[] } {
  if (isRain) {
    const top = Math.max(1, Math.ceil(max));
    return { lo: 0, hi: top, ticks: [0, top] };
  }
  const step = max - min > 12 ? 10 : 5;
  const lo = Math.floor(min / step) * step;
  let hi = Math.ceil(max / step) * step;
  if (hi === lo) hi = lo + step;
  const t: number[] = [];
  for (let v = lo; v <= hi; v += step) t.push(v);
  return { lo, hi, ticks: t };
}
