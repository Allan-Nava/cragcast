#!/usr/bin/env bash
# terraform apply, again and again, while Oracle answers "Out of host capacity" for A1.Flex —
# the usual state of the Always Free pool in a busy region. Any other error stops the loop.
#
#   ./retry-apply.sh            # every 120 s, at most 360 attempts (12 hours)
#   INTERVAL=300 ./retry-apply.sh
set -uo pipefail
cd "$(dirname "$0")" || exit 1
INTERVAL="${INTERVAL:-120}"
MAX="${MAX:-360}"
log=$(mktemp)
trap 'rm -f "$log"' EXIT

for ((i = 1; i <= MAX; i++)); do
  echo "[$(date '+%F %T')] attempt $i/$MAX"
  if terraform apply -auto-approve -input=false -no-color >"$log" 2>&1; then
    tail -n 5 "$log"
    echo "[$(date '+%F %T')] VM created."
    exit 0
  fi
  if grep -qiE "out of (host )?capacity" "$log"; then
    echo "  no A1 capacity in the region; next try in ${INTERVAL}s"
    sleep "$INTERVAL"
    continue
  fi
  echo "[$(date '+%F %T')] terraform failed for another reason:" >&2
  tail -n 30 "$log" >&2
  exit 1
done
echo "gave up after $MAX attempts" >&2
exit 1
