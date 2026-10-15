// CG-35: the service worker that makes cragcast installable and readable offline.
// - The page's own files and the data: network first, so an update is never held back; the
//   cached copy only when the network fails. The page then says how old the data is.
// - Leaflet from cdnjs: cache first — the URLs carry the version and an SRI hash.
// - Map tiles are never cached here: OpenStreetMap's tile policy asks apps not to store them.
const CACHE = "cragcast-v1";
const SHELL = ["./", "index.html", "style.css", "app.js", "detail.js", "logo.svg", "manifest.webmanifest",
               "data/conditions.json", "icons/icon-any-192.png"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", e => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.hostname.endsWith("tile.openstreetmap.org")) return;
  if (url.hostname === "cdnjs.cloudflare.com") {
    e.respondWith(caches.match(e.request).then(hit => hit || fetch(e.request).then(res => {
      const copy = res.clone();
      caches.open(CACHE).then(c => c.put(e.request, copy));
      return res;
    })));
    return;
  }
  if (url.origin !== self.location.origin) return;
  // Same origin: network first, the cache as the fallback. The query string (?q=…) is the page's
  // own state, so match the cached file without it.
  e.respondWith(fetch(e.request).then(res => {
    if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(url.pathname.endsWith("/") ? "./" : e.request, copy)); }
    return res;
  }).catch(() => caches.match(e.request, { ignoreSearch: true })
    .then(hit => hit || caches.match("./"))));
});
