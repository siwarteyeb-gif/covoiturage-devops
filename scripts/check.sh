#!/usr/bin/env bash
# =============================================================================
# check.sh : script de controle du depot et de l'application.
#
#   ./scripts/check.sh static            controles du depot (fichiers, secrets, Dockerfiles, Compose)
#   ./scripts/check.sh compose           valide docker-compose.yml (necessite Docker)
#   ./scripts/check.sh health URL [s]    attend que URL reponde 200 (defaut : 60 secondes)
#
# Codes de sortie :  0 = tout est bon   1 = au moins un controle a echoue   2 = mauvaise utilisation
# =============================================================================
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2

FAILURES=0
PASSES=0

ok()   { printf '  [ OK ] %s\n' "$1"; PASSES=$((PASSES + 1)); }
fail() { printf '  [FAIL] %s\n' "$1"; FAILURES=$((FAILURES + 1)); }
skip() { printf '  [SKIP] %s\n' "$1"; }
title() { printf '\n== %s ==\n' "$1"; }

DOCKERFILES=(api/Dockerfile map-service/Dockerfile notification-service/Dockerfile frontend/Dockerfile)

# --------------------------------------------------------------------------- 1
check_required_files() {
  title "Fichiers obligatoires"
  local f
  for f in README.md .gitignore .env.example docker-compose.yml docker-compose.build.yml \
           .github/workflows/ci.yml .github/workflows/release.yml .github/workflows/rollback.yml \
           scripts/check.sh scripts/deploy.sh scripts/rollback.sh "${DOCKERFILES[@]}"; do
    if [ -f "$f" ]; then ok "$f"; else fail "fichier manquant : $f"; fi
  done
  for f in "${DOCKERFILES[@]}"; do
    d="$(dirname "$f")"
    if [ -f "$d/.dockerignore" ]; then ok "$d/.dockerignore"; else fail "$d/.dockerignore manquant"; fi
  done
}

# --------------------------------------------------------------------------- 2
check_git_hygiene() {
  title "Git propre"
  if grep -qxF ".env" .gitignore 2>/dev/null; then ok ".env est dans .gitignore"; else fail ".env doit etre dans .gitignore"; fi
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    if git ls-files --error-unmatch .env >/dev/null 2>&1; then fail ".env est suivi par Git (secret commite)"; else ok ".env n'est pas suivi par Git"; fi
    local tracked
    tracked="$(git ls-files | grep -E '(^|/)(target|node_modules|dist)/' || true)"
    if [ -z "$tracked" ]; then ok "aucun dossier genere (target, node_modules, dist) dans Git"; else fail "fichiers generes suivis par Git : $(echo "$tracked" | head -3 | tr '\n' ' ')"; fi
  else
    skip "pas de depot Git ici"
  fi
}

# --------------------------------------------------------------------------- 3
check_secrets() {
  title "Aucun secret dans le code"
  local files patterns hits
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    files="$(git ls-files)"
  else
    files="$(find . -type f -not -path './.git/*' -not -path '*/node_modules/*' -not -path '*/target/*' -not -name '.env')"
  fi
  patterns='-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|glpat-[A-Za-z0-9_-]{20,}|ghp_[A-Za-z0-9]{36}|xox[baprs]-[A-Za-z0-9-]{10,}'
  # shellcheck disable=SC2086
  hits="$(echo "$files" | grep -v -E '^(scripts/check\.sh|package-lock\.json)$' | xargs -r grep -I -n -E "$patterns" 2>/dev/null || true)"
  if [ -z "$hits" ]; then ok "aucune cle privee ni jeton detecte"; else fail "secret probable :"; echo "$hits" | head -5; fi

  # Les mots de passe ne doivent pas etre ecrits en dur dans les fichiers Compose / CI
  # shellcheck disable=SC2016
  hits="$(grep -n -E '^\s*(DB_PASSWORD|DB_ROOT_PASSWORD|JWT_SECRET|ADMIN_PASSWORD|MARIADB_(ROOT_)?PASSWORD):\s*[A-Za-z0-9]' docker-compose*.yml .github/workflows/*.yml 2>/dev/null | grep -v '\${' || true)"
  if [ -z "$hits" ]; then ok "mots de passe lus depuis l'environnement (pas en dur)"; else fail "mot de passe en dur :"; echo "$hits"; fi
}

# --------------------------------------------------------------------------- 4
check_dockerfiles() {
  title "Dockerfiles"
  local f
  for f in "${DOCKERFILES[@]}"; do
    [ -f "$f" ] || continue
    if grep -qE '^USER ' "$f"; then ok "$f : utilisateur non root (USER)"; else fail "$f : pas d'instruction USER"; fi
    if grep -qE '^USER (root|0)\b' "$f"; then fail "$f : USER root"; fi
    if grep -qE '^HEALTHCHECK ' "$f"; then ok "$f : HEALTHCHECK"; else fail "$f : pas de HEALTHCHECK"; fi
    # chaque FROM doit avoir un tag explicite et different de latest
    if grep -E '^FROM ' "$f" | grep -vE '^FROM [^ ]+:[^ ]+' | grep -qvE '^FROM [a-z]+ AS '; then
      fail "$f : image de base sans tag explicite"
    elif grep -E '^FROM ' "$f" | grep -qE ':latest'; then
      fail "$f : tag :latest interdit"
    else
      ok "$f : tags de base explicites"
    fi
  done
}

# --------------------------------------------------------------------------- 5
service_block() {
  # Affiche le bloc d'un service dans docker-compose.yml (de "  nom:" jusqu'au service suivant)
  awk -v name="$1" '
    $0 ~ "^  "name":" { on = 1; print; next }
    on && /^  [a-zA-Z0-9_-]+:/ { exit }
    on && /^[a-z]/ { exit }
    on { print }
  ' docker-compose.yml
}

check_compose_rules() {
  title "docker-compose.yml"
  local db
  db="$(service_block db)"
  if [ -z "$db" ]; then fail "service db introuvable"; return; fi
  if echo "$db" | grep -qE '^\s+ports:'; then fail "le port de la base est publie (ports: sur db)"; else ok "port de la base non publie"; fi
  if echo "$db" | grep -qE 'db_data:/var/lib/'; then ok "volume persistant pour la base"; else fail "pas de volume persistant sur db"; fi
  if echo "$db" | grep -qE '^\s+healthcheck:'; then ok "healthcheck sur la base"; else fail "pas de healthcheck sur db"; fi
  if grep -qE 'condition:\s*service_healthy' docker-compose.yml; then ok "demarrage conditionne a la sante (service_healthy)"; else fail "aucun depends_on avec service_healthy"; fi
  if grep -qE '@?db:3306' docker-compose.yml; then ok "connexion a la base par nom de service (db)"; else fail "l'API doit joindre la base par le nom de service db"; fi
  if grep -E 'image:' docker-compose.yml | grep -qE ':latest'; then fail "tag :latest dans docker-compose.yml"; else ok "aucun tag :latest"; fi
}

# --------------------------------------------------------------------------- 6
check_scripts() {
  title "Scripts Bash"
  local s
  for s in scripts/*.sh; do
    if bash -n "$s" 2>/dev/null; then ok "syntaxe $s"; else fail "erreur de syntaxe dans $s"; fi
  done
}

# --------------------------------------------------------------------------- compose
cmd_compose() {
  title "Validation de Compose"
  if ! command -v docker >/dev/null 2>&1; then skip "Docker n'est pas installe"; return; fi
  # variables factices : seule la structure du fichier est verifiee
  if DB_ROOT_PASSWORD=x DB_PASSWORD=x JWT_SECRET=0123456789012345678901234567890123 ADMIN_PASSWORD=x \
     docker compose -f docker-compose.yml config -q 2>/dev/null; then
    ok "docker-compose.yml est valide"
  else
    fail "docker-compose.yml est invalide (docker compose config)"
  fi
  if DB_ROOT_PASSWORD=x DB_PASSWORD=x JWT_SECRET=0123456789012345678901234567890123 ADMIN_PASSWORD=x \
     docker compose -f docker-compose.yml -f docker-compose.build.yml config -q 2>/dev/null; then
    ok "surcouche docker-compose.build.yml valide"
  else
    fail "docker-compose.build.yml est invalide"
  fi
}

# --------------------------------------------------------------------------- health
cmd_health() {
  local url="${1:-}" timeout="${2:-60}" waited=0 code
  if [ -z "$url" ]; then echo "usage : $0 health URL [secondes]" >&2; exit 2; fi
  title "Sante de $url"
  while [ "$waited" -lt "$timeout" ]; do
    if command -v curl >/dev/null 2>&1; then
      code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$url" 2>/dev/null || true)"
    else
      code="$(wget -q -O /dev/null --timeout=3 "$url" 2>/dev/null && echo 200 || echo 000)"
    fi
    if [ "$code" = "200" ]; then ok "HTTP 200 apres ${waited}s"; return; fi
    sleep 2
    waited=$((waited + 2))
  done
  fail "pas de reponse 200 apres ${timeout}s (dernier code : ${code:-aucun})"
}

# --------------------------------------------------------------------------- main
MODE="${1:-}"
case "$MODE" in
  static)
    check_required_files
    check_git_hygiene
    check_secrets
    check_dockerfiles
    check_compose_rules
    check_scripts
    ;;
  compose) cmd_compose ;;
  health)  shift; cmd_health "$@" ;;
  *)
    echo "usage : $0 {static|compose|health URL [secondes]}" >&2
    exit 2
    ;;
esac

printf '\nResultat : %d controle(s) reussi(s), %d echec(s)\n' "$PASSES" "$FAILURES"
if [ "$FAILURES" -gt 0 ]; then exit 1; fi
exit 0
