#!/usr/bin/env bash
# =============================================================================
# release.sh : cree et pousse le tag de version (declenche la livraison dans le pipeline).
#
#   ./scripts/release.sh vX.Y.Z
# =============================================================================
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="${1:-}"
if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "usage : $0 vX.Y.Z   (exemple : $0 v1.0.0)" >&2
  exit 2
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [ "$BRANCH" != "main" ]; then echo "Il faut etre sur la branche main (actuellement : $BRANCH)" >&2; exit 1; fi
if [ -n "$(git status --porcelain)" ]; then echo "Des modifications ne sont pas commitees" >&2; exit 1; fi
git fetch origin main --quiet
if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
  echo "main local n'est pas identique a origin/main : faites git pull" >&2
  exit 1
fi
if git rev-parse "$VERSION" >/dev/null 2>&1; then echo "Le tag $VERSION existe deja" >&2; exit 1; fi

./scripts/check.sh static
git tag -a "$VERSION" -m "Version $VERSION"
git push origin "$VERSION"
echo "Tag $VERSION pousse : le pipeline va livrer cette version."
