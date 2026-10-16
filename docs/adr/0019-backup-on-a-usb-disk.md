# 0019 — A nightly backup on a USB disk on the Pi

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-109

## Context

[0016](0016-history-copied-into-postgres.md) held that the Pi's database needs no backup: the
crags come from the image, the weather from Open-Meteo, the history from the `history` branch,
so a restart rebuilds it. The v1.2.0 milestone changes that: CG-106 archives each hourly
forecast, rows that exist nowhere else, on an SD card — the part of a Pi most likely to fail.
Three places for a copy were weighed: a cloud store (credentials on the Pi), the owner's Mac
over Tailscale (only while it is on), a USB disk on the Pi (simplest, no credentials).

## Decision

A `backup` service in the Pi's stack, the same `postgres:17-alpine` image: each night after
03:00 Italian time a `pg_dump -Fc` to a USB disk mounted at `BACKUP_DIR`, checked with
`pg_restore --list` before it replaces anything, 14 days kept. It writes only where the file
`.cragcast-backup-target` is, so an unmounted disk never sends the dumps to the SD card. A
healthcheck marks it unhealthy in Portainer when the newest dump is over two days old. The
restore is written down in `docs/pi-backup.md` and was tried before this was merged.

## Consequences

- The data survives the SD card. It does not survive the Pi itself, or anything that happens
  to the house it is in: the USB disk sits next to it. A copy elsewhere (cloud, the Mac) is a
  later decision, if the archive comes to matter that much.
- [0016](0016-history-copied-into-postgres.md)'s "needs no backup" no longer holds once CG-106
  lands; this record supersedes that consequence, not its decision.
- Setting up the disk is done by hand on the Pi, once (`docs/pi-backup.md`).
