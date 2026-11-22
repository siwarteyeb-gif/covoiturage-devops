# Fiche d'une page — Covoiturage Étudiant

**Étudiant :** ........................  **Groupe :** ........................  **Dépôt :** ........................

## 1. Architecture

Application web de covoiturage entre étudiants, découpée en **4 services** et une base, lancés par Docker Compose :

- `frontend` : Angular servi par nginx, **seul port publié** (8080), passerelle vers l'API (`/api`).
- `api` : Spring Boot, logique métier et sécurité JWT, accède à la base MariaDB.
- `map-service` : géocodage et itinéraires (OpenRouteService). `notification-service` : emails.
- `db` : MariaDB, **port non publié**, volume persistant `db_data`.

Les services se joignent par leur **nom** sur le réseau Compose, démarrent dans l'ordre grâce aux
`healthcheck` (`service_healthy`) et exposent tous `/health`. Si un micro-service tombe, l'API passe en mode dégradé.

**Chaîne CI/CD (GitHub Actions) :** `check → test → build → smoke` sur chaque pull request, puis sur tag `vX.Y.Z` :
`promote` (l'image du commit reçoit l'étiquette de version, **sans reconstruction**, publiée sur ghcr.io) → `deploy` (mise en service et contrôle `/health`).
Tests Java sur JDK 17 et 21 (matrice), rapports de tests en artefacts, `main` protégée (CI vert obligatoire).

## 2. Version mise en service

| | |
|---|---|
| Version | `v.....` |
| Date et heure | ........................ |
| Adresse de l'application | `http://...........:....` |
| Exécution réussie (Actions) | https://........................ |
| Exécution en échec (démonstration) | https://........................ |
| Empreinte des images (digest) | ........................ |

## 3. Déroulé du retour arrière

Situation : la version `v.....` (en service) est défectueuse, la version précédente est `v.....`.

1. Constater le problème (`/health` en erreur, ou comportement incorrect constaté dans l'application).
2. Lancer le retour arrière : workflow manuel *Rollback* dans GitHub Actions, ou `./scripts/rollback.sh` sur le serveur.
3. Le script lit `.deploy/previous`, tire les images de la version précédente depuis le registre (aucune reconstruction)
   et relance `docker compose up -d --wait`.
4. Contrôle automatique de `/health` : réponse 200 attendue.
5. Vérification : `cat .deploy/current` affiche `v.....` ; l'historique `.deploy/history` contient la ligne `ROLLBACK`.

Durée constatée : ........ secondes.
