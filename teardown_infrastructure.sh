#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/.infra/docker-compose.yml"

log() {
  echo "[teardown_infrastructure] $*"
}

log "Undeploying infrastructure and removing volumes..."
docker compose -f "$COMPOSE_FILE" down -v

log "Tear down complete."
