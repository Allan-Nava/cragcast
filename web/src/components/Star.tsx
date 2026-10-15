// CG-46: the favourite toggle — a star outline or filled, and a label that says what a press does.
export function Star({ name, on, onToggle }: { name: string; on: boolean; onToggle(): void }) {
  return (
    <button type="button" className="star" aria-pressed={on}
            aria-label={on ? `Togli ${name} dalle preferite` : `Aggiungi ${name} alle preferite`}
            title={on ? "Togli dalle preferite" : "Aggiungi alle preferite"}
            onClick={e => { e.stopPropagation(); onToggle(); }}>
      <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
        <path d="M12 2.8l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.2l-5.7 3.1 1.2-6.4-4.7-4.4 6.4-.8z"
              fill={on ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      </svg>
    </button>
  );
}
