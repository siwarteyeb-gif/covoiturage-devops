#!/usr/bin/env bash
# =============================================================================
# smoke.sh : test de bout en bout apres demarrage de toute la pile.
#
#   ./scripts/smoke.sh BASE_URL ADMIN_EMAIL ADMIN_PASSWORD
#
# 1. la passerelle repond sur /health ;
# 2. connexion de l'administrateur (API + base de donnees) ;
# 3. appel authentifie /api/maps/geocode : passerelle -> API -> map-service.
# =============================================================================
set -euo pipefail

BASE="${1:-}"; EMAIL="${2:-}"; PASSWORD="${3:-}"
if [ -z "$BASE" ] || [ -z "$EMAIL" ] || [ -z "$PASSWORD" ]; then
  echo "usage : $0 BASE_URL ADMIN_EMAIL ADMIN_PASSWORD" >&2
  exit 2
fi

echo "1/3 sante de la passerelle"
curl -fsS --max-time 10 "$BASE/health" >/dev/null

echo "2/3 connexion de l'administrateur"
LOGIN="$(curl -fsS --max-time 15 -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" "$BASE/api/auth/login")"
TOKEN="$(printf '%s' "$LOGIN" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
if [ -z "$TOKEN" ]; then echo "Pas de token dans la reponse de connexion" >&2; exit 1; fi

echo "3/3 appel authentifie via la chaine passerelle -> API -> map-service"
RESULT="$(curl -fsS --max-time 15 -H "Authorization: Bearer $TOKEN" "$BASE/api/maps/geocode?query=sousse")"
if ! printf '%s' "$RESULT" | grep -q 'Sousse'; then
  echo "Reponse inattendue du geocodage : $RESULT" >&2
  exit 1
fi
echo "Smoke test reussi."
