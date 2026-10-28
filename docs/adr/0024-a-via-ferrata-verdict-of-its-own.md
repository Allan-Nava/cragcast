# 0024 — A via ferrata gets a verdict of its own, stricter than a crag's

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-113

## Context

The crags' verdict (`ConditionsScorer`) is about wet rock on a wall, read at the crag. A via
ferrata's risks are different: lightning on a steel cable, snow or verglas on the steps, wind on
an exposed ridge, and a day long enough to finish in the light. They are worst at the top, often
a thousand metres above the start. A wrong "si va" there costs more than a wasted trip to a wet
crag. `data/ferrate.json` (ADR 0023) gives each via ferrata a top with its DEM elevation.

## Decision

`FerrataScorer`, in the `ferrata` package, scores the weather at the top. `FerrataClient` asks
Open-Meteo for it, many tops in one request, passing each top's elevation so the temperature is
corrected to the route's height. It also asks for gusts, the WMO weather code, CAPE, snowfall and
`is_day`. `FerrataReducer` reads the day from 07:00 to 19:00, wider than a crag's window, plus
the 48 hours before and the day's hours of light.

- **Never "si va"** with a thunderstorm in any hour of the window, nor with CAPE from
  1000 J/kg. From 300 J/kg it is "forse".
- **"No"** for rain in the day above 1 mm (a crag allows 2 mm), fresh snow at the top (1 cm
  over the 48 hours and the day), rain followed by frost (ice), or gusts above 60 km/h.
- **"Forse"** for sustained wind above 35 km/h, recent rain above 5 mm, a top colder than
  3 °C or hotter than 30 °C, or under 10 hours of light.
- Every threshold is a named constant with its reason, changed only with a test for the day it
  fixes, like the crags'. The avalanche bulletin informs and never scores (ADR 0007).

## Consequences

- Cautious by design: many autumn and winter days come out "forse" or "no" on light and cold
  alone. Observed days (CG-27) are what will loosen it, never a guess.
- One more Open-Meteo request an hour, for all via ferratas together, once CG-114 and CG-115
  call it — never one per route.
- The verdict reads the top only. A storm over the start but not the top is still caught by
  CAPE, which covers the whole area; a cold start under a mild top is not a risk this models.
- The S/F/N marks keep meaning the verdict. The page (CG-114) says which kind of place is
  which in words and by its own mark.
