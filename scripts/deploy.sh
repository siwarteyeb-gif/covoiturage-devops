#!/usr/bin/env bash
# =============================================================================
# deploy.sh : met en service une version de l'application avec Docker Compose.
#
#   ./scripts/deploy.sh vX.Y.Z [--no-rollback]
#
# - les images vX.Y.Z sont TIREES du registre, jamais reconstruites ;
# - la version precedente est memorisee dans .deploy/previous (pour le retour arriere) ;
# - apres le demarrage, la route /health est controlee ;
# - en cas d'echec, retour automatique a la version precedente (sauf --no-rollback).
# =============================================================================
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="${1:-}"
NO_ROLLBACK="${2:-}"
if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "usage : $0 vX.Y.Z [--no-rollback]   (exemple : $0 v1.2.0)" >&2
  exit 2
fi

STATE_DIR=".deploy"
mkdir -p "$STATE_DIR"
CURRENT="$(cat "$STATE_DIR/current" 2>/dev/null || true)"

# Le fichier .env peut definir COMPOSE_FILE (mode developpement) : on impose ici le fichier de production
unset COMPOSE_FILE COMPOSE_PATH_SEPARATOR
COMPOSE=(docker compose -f docker-compose.yml)
export IMAGE_TAG="$VERSION"

APP_PORT="${APP_PORT:-$(sed -n 's/^APP_PORT=//p' .env 2>/dev/null | tail -n 1)}"
APP_PORT="${APP_PORT:-8080}"

log() { printf '%s %s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" "$2" >> "$STATE_DIR/history"; }

echo "==> Mise en service de $VERSION (version actuelle : ${CURRENT:-aucune})"
"${COMPOSE[@]}" pull
if "${COMPOSE[@]}" up -d --remove-orphans --wait --wait-timeout 180 \
   && ./scripts/check.sh health "http://localhost:${APP_PORT}/health" 60; then
  if [ -n "$CURRENT" ] && [ "$CURRENT" != "$VERSION" ]; then
    echo "$CURRENT" > "$STATE_DIR/previous"
  fi
  echo "$VERSION" > "$STATE_DIR/current"
  log "$VERSION" "OK"
  echo "==> $VERSION est en service."
  exit 0
fi

echo "==> ECHEC de la mise en service de $VERSION" >&2
"${COMPOSE[@]}" ps >&2 || true
log "$VERSION" "ECHEC"
if [ "$NO_ROLLBACK" != "--no-rollback" ] && [ -n "$CURRENT" ] && [ "$CURRENT" != "$VERSION" ]; then
  echo "==> Retour automatique a $CURRENT" >&2
  exec ./scripts/deploy.sh "$CURRENT" --no-rollback
fi
exit 1
