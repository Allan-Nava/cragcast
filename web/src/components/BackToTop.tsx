// CG-57: "Torna su" once the crags have been scrolled — the page on a phone, the list's own
// column from 900 px, where the list scrolls by itself. Smooth unless motion is reduced.
import { useEffect, useState } from "react";

/** How far, in px, before the button shows. */
export const AFTER_PX = 600;

const calm = () => typeof matchMedia === "function" && matchMedia("(prefers-reduced-motion: reduce)").matches;

export function BackToTop() {
  const [shown, setShown] = useState(false);
  useEffect(() => {
    const list = () => document.getElementById("list");
    const check = () => setShown(window.scrollY > AFTER_PX || (list()?.scrollTop ?? 0) > AFTER_PX);
    addEventListener("scroll", check, { passive: true });
    // The list's scroll does not bubble: listen in the capture phase to hear it.
    document.addEventListener("scroll", check, { passive: true, capture: true });
    return () => {
      removeEventListener("scroll", check);
      document.removeEventListener("scroll", check, { capture: true });
    };
  }, []);
  if (!shown) return null;
  const up = () => {
    const behavior: ScrollBehavior = calm() ? "auto" : "smooth";
    const list = document.getElementById("list");
    if (list && list.scrollTop > 0) list.scrollTo({ top: 0, behavior });
    if (window.scrollY > 0) window.scrollTo({ top: 0, behavior });
  };
  return <button type="button" className="to-top" onClick={up}>Torna su</button>;
}
