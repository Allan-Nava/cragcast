# 0027 — Three avalanche services, the same rule: facts only, each credited

- **Status:** accepted
- **Date:** 2026-10-29
- **Backlog:** CG-120

## Context

Since CG-43 cragcast covers Piedmont and Trentino-Alto Adige, whose crags had no avalanche line.
Their bulletins are on the same EAWS server and in the same CAAML v6 format as Lombardy's: one
file a day for Piedmont (IT-21, issued by AINEVA Piemonte) and one per province for the Euregio
(IT-32-TN, IT-32-BZ, read on avalanche.report). The backlog asked for each licence to be checked
before anything was shown. None of them states one for the bulletin: the Trentino province calls
the Euregio's data free and open to everyone; ARPA Piemonte's geographic services are CC BY 4.0,
but not the bulletin; ARPA Lombardia states none (ADR 0007).

## Decision

The rule of ADR 0007 holds for all three: only the facts — the level by band and part of the
day, the problem types — never the text, always with a link to the official bulletin, and the
bulletin informs, never scores. `AvalancheBulletins.PROVIDERS` lists the four files with who issues
each and where its bulletin is read; every micro-region takes its provider from its id (the
longest matching prefix), the page names it on the link ("Bollettino AINEVA Piemonte"), and the
credits name all three. `tools/avalanche_regions.py` takes the micro-regions of the four files
from the EAWS repository (CC0). A provider whose file is missing or fails is left out; the others
are shown.

## Consequences

- 608 of 780 crags lie in a bulletin's zone (on 2026-10-29: Lombardy 121 of 222, Piedmont 246 of
  317, Trentino-Alto Adige 241 of 241).
- Four requests a day where there was one; the bulletin archive on the Pi keeps all of them.
- The Ticino's SLF does state CC BY 4.0; when CG-122 brings it, it can be credited as such.
