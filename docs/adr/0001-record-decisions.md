# 0001 — Record decisions as Architectural Decision Records

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-65

## Context

cragcast changed shape several times in a few weeks: a Spring service meant for an Oracle VM
became a static site on GitHub Pages; the page was rewritten in React; the crag list tripled.
The reasons lived in CLAUDE.md rules, PR descriptions and commit messages — enough to follow
what the code does, not why it was chosen over the alternatives, nor when a reason stops
holding.

## Decision

Each decision that changes how cragcast is built, what data it trusts or what it promises its
readers is written down as an Architectural Decision Record (ADR) in `docs/adr/`, numbered, in the
form of `template.md`: context, decision, consequences, status. The record goes in the same PR
as the change. A decision reversed later keeps its record, marked superseded by the new one.

## Consequences

- The "why" survives the people and the sessions that made it; CLAUDE.md keeps the rules, the
  ADRs keep the reasons behind them.
- The first records (0002–0014) were written after the fact, on 2026-10-16, from the backlog,
  the PRs and the discussion that led to each one; their dates are the day the decision first
  landed in git.
- A small cost per decision: one short file.
