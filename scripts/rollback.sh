#!/usr/bin/env bash
# =============================================================================
# rollback.sh : retour a la version precedente (celle qui tournait avant la version actuelle).
#
#   ./scripts/rollback.sh
#
# Les images de l'ancienne version sont deja dans le registre : on les remet en service,
# sans rien reconstruire. Un deuxieme appel rebascule vers la version d'avant (aller-retour).
# =============================================================================
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

STATE_DIR=".deploy"
CURRENT="$(cat "$STATE_DIR/current" 2>/dev/null || true)"
PREVIOUS="$(cat "$STATE_DIR/previous" 2>/dev/null || true)"

if [ -z "$PREVIOUS" ]; then
  echo "Retour arriere impossible : aucune version precedente memorisee dans $STATE_DIR/previous" >&2
  exit 1
fi

echo "==> Retour arriere : ${CURRENT:-?} -> $PREVIOUS"
if ./scripts/deploy.sh "$PREVIOUS" --no-rollback; then
  # la version qu'on quitte devient la "precedente" (deploy.sh l'a deja memorisee)
  printf '%s %s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$PREVIOUS" "ROLLBACK depuis ${CURRENT:-?}" >> "$STATE_DIR/history"
  echo "==> Retour arriere termine : $PREVIOUS est en service."
else
  echo "==> ECHEC du retour arriere vers $PREVIOUS" >&2
  exit 1
fi
