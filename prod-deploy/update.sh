#!/usr/bin/env bash
# Pull a tested release and recreate containers without building code or deleting data.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

tag="${1:-}"
if [[ -z "$tag" && -f release-tag.txt ]]; then
  tag="$(tr -d '\r\n' < release-tag.txt)"
fi
if [[ ! "$tag" =~ ^[0-9a-f]{40}$ && "$tag" != latest ]]; then
  printf 'Usage: bash update.sh <40-character Git commit SHA|latest>\n' >&2
  printf 'Or place a successful workflow commit SHA in release-tag.txt.\n' >&2
  exit 2
fi
[[ -f .env && -f docker-compose.prod.yml ]] || { printf 'Missing .env or docker-compose.prod.yml in deployment directory.\n' >&2; exit 2; }
command -v docker >/dev/null || { printf 'Docker is required.\n' >&2; exit 2; }
command -v flock >/dev/null || { printf 'flock (util-linux) is required.\n' >&2; exit 2; }
# Separate workflows/operators must not race an update on this host.
if [[ "${INSURANCE_UPDATE_LOCKED:-0}" != 1 ]]; then
  exec 9>.insurance-update.lock
  flock -n 9 || { printf 'Another insurance update is running.\n' >&2; exit 1; }
fi
mkdir -p documents
chmod 600 .env
export IMAGE_TAG="$tag"
compose=(docker compose --env-file .env -p insurance -f docker-compose.prod.yml)
"${compose[@]}" config --quiet
# Both pulls must succeed BEFORE any running service is recreated.
"${compose[@]}" pull backend frontend
"${compose[@]}" up -d --no-build --wait --wait-timeout 240
"${compose[@]}" ps
printf '%s\n' "$tag" > .last-deployed-tag
printf 'Insurance deployment completed: %s\n' "$tag"
# No down, volume deletion, automatic migration rollback, or global image pruning.
