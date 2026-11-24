# A Raspberry Pi from scratch

How to bring up a new Raspberry Pi that runs cragcast: the backend, its PostgreSQL, the page on
port 8081 and the nightly backup, from `deploy/pi/compose.yml` as a Portainer Git stack
([ADR 0015](adr/0015-raspberry-pi-pulls-its-stack.md)). The Pi pulls everything; nothing from
GitHub reaches into it, and no port is opened on the router.

One step at a time: each one says how to check it before going on.

## 0. What you need

- **A Raspberry Pi 4 or 5 with 4 GB or more.** The backend is capped at 1 GB and the JVM takes
  75% of it; PostgreSQL, the page and the OS share the rest.
- **A 64-bit OS.** The images are built for `linux/arm64` only (`image.yml`, `release.yml`); a
  32-bit OS cannot run them.
- **Storage:** a 32 GB or larger microSD card works; a USB SSD to boot from lasts longer under
  PostgreSQL's writes.
- **A second USB disk for the backups**, wiped by step 6.
- **Ethernet** if you can: the stack does not need Wi-Fi, and a cable avoids a Pi that drops off
  the network.

## 1. Write the OS

With [Raspberry Pi Imager](https://www.raspberrypi.com/software/) on your computer:

1. Device: your Pi. OS: **Raspberry Pi OS Lite (64-bit)** — no desktop, nothing to spend memory
   on. Storage: the card or the SSD.
2. In the customisation (OS customisation / "Edit settings"):
   - hostname `cragcast` (the Pi then answers as `cragcast.local` on most networks);
   - a user and a strong password;
   - Wi-Fi only if there is no cable;
   - locale: time zone `Europe/Rome`;
   - **Services: enable SSH, public-key authentication only**, with your public key
     (`~/.ssh/id_ed25519.pub`).
3. Write, put the card in the Pi, connect the cable and power it on. The first boot takes a
   minute or two.

**Check:** `ssh <user>@cragcast.local` from your computer opens a shell on the Pi. If `.local`
does not resolve, find the Pi's address in the router's list of devices.

Give the Pi a fixed address in the router (a DHCP reservation for its MAC address), so its URL
does not change.

## 2. Update it

```bash
sudo apt update && sudo apt full-upgrade -y
sudo reboot
```

**Check:** after the reboot, `uname -m` says `aarch64`, the 64-bit OS.

Security updates by themselves from then on:

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades    # answer Yes
```

## 3. Docker

Docker's install script, which detects Raspberry Pi OS and adds Docker's own package repository:

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
less get-docker.sh                                 # read what it will run
sudo sh get-docker.sh
sudo usermod -aG docker "$USER"                    # docker without sudo, from the next login
exit                                               # log out, then ssh in again
```

**Check:** `docker run --rm hello-world` prints "Hello from Docker!", and `docker compose version`
answers.

## 4. Portainer

Portainer is the web page that runs the stack and keeps it in step with the repository.

```bash
docker volume create portainer_data
docker run -d --name portainer --restart=always -p 9443:9443 \
  -v /var/run/docker.sock:/var/run/docker.sock -v portainer_data:/data \
  portainer/portainer-ce:lts
```

**Check:** open `https://cragcast.local:9443` in a browser. The certificate is self-signed, so
the browser warns once; go on. Create the admin user **within five minutes** of the start — after
that Portainer locks itself for safety, and `docker restart portainer` reopens the window. Then
choose the local environment.

## 5. The backup disk

Follow [the backup's set-up](pi-backup.md#set-up-the-disk-once) up to the marker file, one step
at a time as it says: plug the disk in, check `lsblk -f` lists it as `sda1` and not the card,
format, mount by label with `nofail`, then write `.cragcast-backup-target` only once
`mountpoint` confirms the disk is mounted.

Without the disk the stack still runs; the backup service only refuses to write and shows as
unhealthy.

## 6. The stack

Generate the database password once and keep it in your password manager:

```bash
openssl rand -base64 24
```

In Portainer: **Stacks → Add stack**, name `cragcast`, build method **Repository**:

| Field | Value |
|---|---|
| Repository URL | `https://github.com/Allan-Nava/cragcast` |
| Repository reference | `refs/heads/main` |
| Compose path | `deploy/pi/compose.yml` |
| Authentication | off (the repository is public) |
| GitOps updates | on, mechanism **Polling** (5 minutes is enough) |
| Re-pull image | on |

Environment variables (**Advanced mode** lets you paste them):

```
DATABASE_PASSWORD=<the password generated above>
CRAGCAST_VERSION=<the latest release, e.g. v2026.10.28>
SITE_URL=http://cragcast.local:8081/
```

- `CRAGCAST_VERSION` pins the Pi to one release ([ADR 0020](adr/0020-calendar-releases-the-pi-follows.md)):
  the latest is on the [releases page](https://github.com/Allan-Nava/cragcast/releases). Left
  out, the Pi follows `main` and restarts at every merge.
- `SITE_URL` is the page's address as you open it; the share links use it.
- The other variables have defaults in `deploy/pi/compose.yml`: `BACKEND_PORT` (8080),
  `WEB_PORT` (8081), `DB_PORT` (5432, on 127.0.0.1 only), `BACKUP_DIR`, `BACKUP_HOUR`,
  `BACKUP_DAYS`.

**Deploy the stack.** The first pull downloads the images and the backend's first start imports
the crags and the history and fetches the weather: give it a few minutes.

## 7. Check that it works

On the Pi:

```bash
docker ps --format '{{.Names}}\t{{.Status}}'      # cragcast-db-1, -backend-1, -web-1, -backup-1: Up
curl -s localhost:8080/actuator/health            # {"status":"UP"...}
curl -s localhost:8080/api/crags | head -c 200    # the crags, as JSON
docker logs --tail 20 cragcast-backend-1          # Flyway migrations, then the crag import
```

From a computer on the same network: `http://cragcast.local:8081/` shows the page, with today's
verdicts. The next morning, `ls -lh /mnt/cragcast-backups` shows the first dump.

## 8. Reach it from outside the house (optional)

With [Tailscale](https://tailscale.com/) on the Pi's host — not in the stack — the Pi joins your
tailnet and nothing else does ([ADR 0017](adr/0017-the-pi-over-tailscale.md)):

```bash
curl -fsSL https://tailscale.com/install.sh -o tailscale-install.sh
less tailscale-install.sh
sudo sh tailscale-install.sh
sudo tailscale up                                  # prints a link: open it and log in
tailscale ip -4                                    # the Pi's tailnet address
```

No `--advertise-routes`, no `--advertise-exit-node`: those would bring the home network into the
tailnet. From any device of yours on the tailnet: `http://<tailnet address>:8081/`, the API on
`:8080`, and the database through an SSH tunnel, since it listens on the Pi's 127.0.0.1 only:

```bash
ssh -L 5433:127.0.0.1:5432 <user>@<tailnet address>
psql -h 127.0.0.1 -p 5433 -U cragcast cragcast    # in another terminal
```

## Day to day

- **A new release:** change `CRAGCAST_VERSION` in the stack's environment and **Update the
  stack**. Going back is setting the previous one.
- **Logs:** Portainer → Containers → a container → Logs, or `docker logs`.
- **Restore a backup:** [pi-backup.md](pi-backup.md#restore).
- **A rebuilt Pi** needs steps 1 to 8 again; the data comes back from the last dump.
