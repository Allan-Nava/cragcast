# The Pi's backup (CG-109)

The `backup` service of `deploy/pi/compose.yml` dumps the database each night after 03:00
Italian time onto a USB disk mounted on the Pi, checks the dump with `pg_restore --list` and
keeps 14 days ([ADR 0019](adr/0019-backup-on-a-usb-disk.md)). It writes only to a disk that
carries the file `.cragcast-backup-target`, so a disk that failed to mount never fills the SD
card instead.

Since CG-106 the dump is the only copy of `crag_forecast`, every hourly forecast the Pi has
made ([ADR 0021](adr/0021-hourly-forecasts-archived.md)), and since CG-107 of `avalanche_bulletin`
([ADR 0022](adr/0022-avalanche-bulletins-kept.md)); the other tables can be rebuilt from
the repository and the `history` branch.

## Set up the disk, once

On the Pi, with the USB disk plugged in (a whole disk for the backups; formatting erases it).
One step at a time, reading each output before the next — not pasted as one block: without a
disk, `/mnt/cragcast-backups` is a plain folder on the SD card, and a marker written there would
send the dumps to the card.

```bash
lsblk -f                                   # the disk must be listed, e.g. sda1 — not mmcblk0, the SD card
```

Only if it is:

```bash
sudo mkfs.ext4 -L cragcast-backups /dev/sda1
sudo mkdir -p /mnt/cragcast-backups
echo 'LABEL=cragcast-backups /mnt/cragcast-backups ext4 defaults,nofail,x-systemd.device-timeout=10s 0 2' \
  | sudo tee -a /etc/fstab                 # nofail: the Pi still boots without the disk
sudo systemctl daemon-reload && sudo mount -a
# The marker only on the mounted disk, never on the folder underneath it:
mountpoint /mnt/cragcast-backups && sudo touch /mnt/cragcast-backups/.cragcast-backup-target
```

Then **Pull and redeploy** the stack in Portainer. Another mount point: set `BACKUP_DIR` in the
stack's environment. `BACKUP_HOUR` (3) and `BACKUP_DAYS` (14) change the time and the retention.

## Check it

```bash
docker logs --tail 5 cragcast-backup-1     # "backup /backups/cragcast-<date>.dump, 28.0K"
ls -lh /mnt/cragcast-backups
```

Portainer shows `cragcast-backup-1` as unhealthy when the newest dump is over two days old (a
day's grace after the first start). "no .cragcast-backup-target" in its log means the disk is
not mounted.

## Restore

Into the running stack, replacing what is in the database — the backend stopped, so nothing
writes during the restore:

```bash
docker stop cragcast-backend-1
docker exec cragcast-backup-1 pg_restore --clean --if-exists --no-owner -d cragcast /backups/cragcast-<date>.dump
docker start cragcast-backend-1
```

Tried on 2026-10-16 on the stack built from this repository, with history and weather rows
deleted first: with the backend still off, the restore brought back every row (295 history,
660 weather, 220 crags, the 4 Flyway migrations); a restore into an empty PostgreSQL gave the
same counts.

To look at a dump without touching the database, restore it into a throwaway container:

```bash
docker run -d --name restore-check -e POSTGRES_PASSWORD=check -e POSTGRES_USER=cragcast \
  -e POSTGRES_DB=cragcast -v /mnt/cragcast-backups:/backups:ro postgres:17-alpine
docker exec restore-check pg_restore -U cragcast -d cragcast --no-owner /backups/cragcast-<date>.dump
docker exec -it restore-check psql -U cragcast
docker rm -f restore-check
```
