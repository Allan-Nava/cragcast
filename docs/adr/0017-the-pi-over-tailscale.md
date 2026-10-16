# 0017 — The Pi over Tailscale, and only the Pi

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-102

## Context

The Pi of [0015](0015-raspberry-pi-pulls-its-stack.md) sits on a friend's home network and was
reachable only from there: checking the stack, reading the database or fixing a deploy meant
being in that house. Opening a port on the friend's router would put the service on the
internet, which [0015](0015-raspberry-pi-pulls-its-stack.md) ruled out, and would touch a
network that is not cragcast's.

## Decision

Tailscale runs on the Pi's host (not in the stack), joined to the owner's tailnet. The Pi
advertises no subnet routes and is no exit node, so only the Pi itself joins the tailnet, not
the network around it. The backend answers on the Pi's tailnet address as on the LAN.
PostgreSQL is published on the Pi's 127.0.0.1 only and reached by an SSH tunnel; it is never on
the LAN or the tailnet by itself.

## Consequences

- The owner reaches the Pi, its API and psql from anywhere, with no port opened on the router
  and nothing public; Pages stays the only public surface.
- The host's Tailscale is set up by hand on the Pi, outside the repository and Portainer: a
  rebuilt Pi needs it again (the steps are in CG-102).
- Advertising a route or an exit node from the Pi would bring the friend's network into the
  tailnet; that needs the friend's consent and a new record.
