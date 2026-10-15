// CG-35: the service worker that makes cragcast installable and readable offline.
// - The page's own files and the data: network first, so an update is never held back; the
//   cached copy only when the network fails. The page then says how old the data is.
// - Leaflet is bundled by Vite since CG-10, so there is no third-party script to cache.
// - Map tiles are never cached here: OpenStreetMap's tile policy asks apps not to store them.
// Bump CACHE when SHELL or the strategy changes, or installed copies keep the old files.
const CACHE = "cragcast-v2";
const SHELL = ["./", "index.html", "manifest.webmanifest", "logo.svg", "data/conditions.json",
               "icons/icon-any-192.png"];

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
  if (e.request.method !== "GET" || url.origin !== self.location.origin) return;   // tiles included
  // Network first, the cache as the fallback. The query string (?q=…) is the page's own
  // state, so match the cached file without it. Vite's hashed assets are cached as they load.
  e.respondWith(fetch(e.request).then(res => {
    if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(url.pathname.endsWith("/") ? "./" : e.request, copy)); }
    return res;
  }).catch(() => caches.match(e.request, { ignoreSearch: true })
    .then(hit => hit || caches.match("./"))));
});
