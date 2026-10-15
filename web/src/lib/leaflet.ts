// Leaflet as an ES module, then exposed as the global L that leaflet.markercluster (a UMD build)
// extends when it is imported right after this module.
import L from "leaflet";

(globalThis as unknown as { L: typeof L }).L = L;

export default L;
