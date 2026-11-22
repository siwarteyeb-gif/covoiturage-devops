# Covoiturage Étudiant — chaîne DevOps

Application de covoiturage pour étudiants (Angular + Spring Boot), découpée en **micro-services**,
conteneurisée avec **Docker**, assemblée avec **Docker Compose**, testée et livrée par **GitHub Actions** (images publiées sur **ghcr.io**).

## Démarrer le projet en une commande

Prérequis : Docker Desktop (ou Docker Engine + Compose).

```bash
cp .env.example .env          # puis changer les mots de passe dans .env
docker compose up --build
```

Ouvrir **http://localhost:8080**. Compte administrateur : voir `ADMIN_EMAIL` / `ADMIN_PASSWORD` dans `.env`.

> Sous Windows : lancer les commandes depuis Git Bash ou WSL (les scripts `scripts/*.sh` sont en Bash).

### Mode développement (sans Docker pour les services)

```bash
docker compose up -d db                 # base seulement (ajouter ports: dans un override local si besoin)
cd map-service && ./mvnw spring-boot:run            # :8081
cd notification-service && ./mvnw spring-boot:run   # :8082
cd api && ./mvnw spring-boot:run                    # :8080 (variables DB_*, JWT_SECRET si besoin)
cd frontend && npm start                            # :4200, /api redirigé vers :8080 par proxy.conf.json
```

## Architecture

```
 Navigateur
     │  http://localhost:8080  (seul port publié)
     ▼
┌───────────────────────────┐
│ frontend  (nginx + Angular)│  /health, fichiers statiques
│  = passerelle              │  /api/*  ──────────────┐
└───────────────────────────┘                          ▼
                                            ┌─────────────────────┐
                                            │ api  (Spring Boot)  │  JWT, trajets, réservations,
                                            │      :8080          │  avis, administration
                                            └──┬────────┬─────────┘
                                   JDBC        │        │  HTTP (réseau interne)
                                  ┌────────────▼┐   ┌───▼───────────────┐   ┌──────────────────────────┐
                                  │ db (MariaDB)│   │ map-service :8081 │   │ notification-service     │
                                  │ volume      │   │ géocodage,        │   │ :8082  emails            │
                                  │ db_data     │   │ itinéraire (ORS)  │   │                          │
                                  └─────────────┘   └───────────────────┘   └──────────────────────────┘
```

| Service | Rôle | Port | Publié ? |
|---|---|---|---|
| `frontend` | Angular servi par nginx, passerelle vers l'API | 8080 | **oui** |
| `api` | cœur métier, sécurité JWT, base de données | 8080 (interne) | non |
| `map-service` | adresses et itinéraires (OpenRouteService), seul à connaître la clé API | 8081 | non |
| `notification-service` | envoi des emails | 8082 | non |
| `db` | MariaDB, données dans le volume `db_data` | 3306 | **non** |

**Résilience :** si `map-service` ou `notification-service` tombe, l'API continue de fonctionner en mode
dégradé (distance à vol d'oiseau, emails ignorés) au lieu de renvoyer une erreur.

Chaque service expose `GET /health`, utilisé par les `HEALTHCHECK` Docker, par Compose
(`depends_on: condition: service_healthy`) et par le pipeline.

## Structure du dépôt

```
api/                     micro-service principal (Spring Boot, tests JUnit)
map-service/             micro-service cartes (Spring Boot, tests JUnit)
notification-service/    micro-service emails (Spring Boot, tests JUnit)
frontend/                Angular + nginx (tests Vitest)
scripts/                 check.sh  deploy.sh  rollback.sh  smoke.sh  release.sh
docker-compose.yml       pile complète (images du registre)
docker-compose.build.yml surcouche de développement (construit les images)
.github/workflows/       pipelines GitHub Actions : ci.yml, release.yml, rollback.yml
docs/                    fiche d'architecture, scénario de soutenance
```

## Tests

```bash
cd api && ./mvnw verify                  # idem pour map-service et notification-service
cd frontend && npm ci --legacy-peer-deps && npx ng test --watch=false
./scripts/check.sh static                # contrôle du dépôt (fichiers, secrets, Dockerfiles, Compose)
```

Les tests unitaires n'ont besoin ni de MariaDB ni du réseau : base H2 en mémoire, services distants simulés
(`MockRestServiceServer`), SMTP simulé (Mockito). Le rapport de couverture JaCoCo est dans `target/site/jacoco/`.

## Les pipelines (`.github/workflows/`)

```
 Pull request / main :  check ─► test ─► build ─► smoke        (ci.yml)
 Tag vX.Y.Z          :  promote ─► release GitHub ─► deploy    (release.yml)
 Manuel              :  rollback                               (rollback.yml)
```

| Étape | Jobs | Ce qui se passe |
|---|---|---|
| **check** | `check` | `scripts/check.sh`, shellcheck, hadolint, validation Compose |
| **test** | `test-java` (3 services × JDK 17 et 21), `test-frontend` | tests unitaires, **rapports en artefacts** (JUnit et couverture JaCoCo) |
| **build** | `build` avec `needs: [check, test-java, test-frontend]` | une image **par service**, construite seulement si les tests passent, taguée `sha-xxxxxxx` |
| **smoke** | `smoke` (sur `main`) | démarre toute la pile, connexion admin puis appel `passerelle → API → map-service` |
| **promote** | `promote` | le tag `vX.Y.Z` est **ajouté aux images existantes** (pas de reconstruction) : `v1.2.0`, `1.2.0`, `1.2`, `1`, `latest` |
| **release** | `github-release` | release GitHub avec notes générées |
| **deploy** | `deploy` | mise en service sur le serveur de test + contrôle `/health` (ignoré si `DEPLOY_HOST` n'est pas défini) |

Images publiées : `ghcr.io/<utilisateur>/<depot>/api`, `/map-service`, `/notification-service`, `/frontend`.

### Correspondance avec la grille d'évaluation

| # | Exigence | Où |
|---|---|---|
| 1 | Git propre, `.gitignore`, aucun secret | `.gitignore`, `.env.example`, secrets GitHub, contrôle dans `check.sh` |
| 2 | Script Bash (0 = succès, ≠ 0 = échec) | `scripts/check.sh` (+ `deploy.sh`, `rollback.sh`, `smoke.sh`, `release.sh`) |
| 3 | Dockerfile, `.dockerignore`, tag explicite, non-root, HEALTHCHECK | `*/Dockerfile`, `*/.dockerignore` |
| 4 | Compose : app + base, nom de service, port base non publié, volume, démarrage conditionné | `docker-compose.yml` |
| 5 | CI sur pull request : tests puis image (`needs`), rapport en artefact | `ci.yml` (`test-java`, `test-frontend` → `build`) |
| 6 | Tag `vX.Y.Z`, image jamais reconstruite, mise en service + contrôle de santé | `release.yml` (`promote`, `deploy`), `scripts/deploy.sh` |
| 7 | Retour arrière | `scripts/rollback.sh`, workflow `rollback.yml` |
| Bonus | Protection de `main` | réglage GitHub (ci-dessous) |
| Bonus | Déploiement automatique sur tag | job `deploy` de `release.yml` |
| Bonus | Matrice de versions | `strategy.matrix` : JDK 17 / 21 |

## Mise en place GitHub (une seule fois)

1. **Protéger `main`** (bonus) : Settings › Branches › *Add branch protection rule* › `main` :
   cocher *Require a pull request before merging* et *Require status checks to pass before merging*,
   puis sélectionner `build` et `smoke`. Une pull request dont le CI est rouge ne peut plus être fusionnée.
2. **Packages** : après le premier push sur `main`, les images apparaissent dans l'onglet *Packages* du profil.
   Pour que le serveur puisse les tirer sans jeton, les rendre publiques (Package settings › Change visibility).
3. **Serveur de test** (pour le job `deploy`, facultatif) : une machine Linux avec Docker et Compose, et la clé publique
   correspondant à `SSH_PRIVATE_KEY` dans `~/.ssh/authorized_keys`.
   Settings › Secrets and variables › Actions :

| Type | Nom | Valeur |
|---|---|---|
| Variable | `DEPLOY_HOST` | adresse du serveur (sans cette variable, le job `deploy` est ignoré) |
| Variable | `DEPLOY_USER` | utilisateur SSH (membre du groupe `docker`) |
| Variable | `DEPLOY_DIR` | dossier sur le serveur, ex. `/opt/covoiturage` |
| Variable | `APP_PORT` | port public, ex. `8080` |
| Secret | `SSH_PRIVATE_KEY` | clé privée SSH |
| Secret | `DB_PASSWORD`, `DB_ROOT_PASSWORD` | mots de passe de la base |
| Secret | `JWT_SECRET` | secret JWT (32 caractères minimum) |
| Secret | `ADMIN_PASSWORD` | mot de passe du compte administrateur |
| Secret | `ORS_API_KEY` | clé OpenRouteService (facultatif) |

Évitez les caractères `$` et `"` dans les mots de passe.

## Flux de travail Git

```bash
git checkout -b feature/ma-modification
# ... modifier, tester ...
git add <fichiers>
git commit -m "feat(api): message clair"      # feat, fix, test, docs, ci, chore
git push -u origin feature/ma-modification    # puis ouvrir une pull request
```

Le CI de la pull request doit être vert avant la fusion dans `main`.

## Livrer une version

```bash
git checkout main && git pull
./scripts/release.sh v1.0.0        # contrôle, crée le tag, le pousse
```

Le workflow `release.yml` exécute `promote` (étiquettes sur les images déjà construites), crée la release GitHub,
puis `deploy` (mise en service + contrôle `/health`).

> Le tag doit porter sur un commit de `main` dont le CI est terminé et vert : ce sont ces images
> (`sha-xxxxxxx`) qui sont promues. Sinon `promote` s'arrête avec un message explicite.

## Revenir en arrière

- **Depuis GitHub** : Actions › *Rollback* › *Run workflow* (version vide = version précédente, ou saisir `v1.1.0`).
- **Sur le serveur** : `cd $DEPLOY_DIR && ./scripts/rollback.sh`

Le script remet en service la version précédente (mémorisée dans `.deploy/previous`) avec ses images déjà
présentes dans le registre. Rien n'est reconstruit. L'historique est dans `.deploy/history`.
Si une mise en service échoue au contrôle `/health`, `deploy.sh` revient automatiquement à la version précédente.

## Dépannage

| Symptôme | Cause probable |
|---|---|
| `DB_PASSWORD manquant` au lancement | le fichier `.env` n'existe pas : `cp .env.example .env` |
| `api` reste « unhealthy » | voir `docker compose logs api` (souvent la base n'est pas encore prête, ou `JWT_SECRET` trop court) |
| Port 8080 déjà utilisé | changer `APP_PORT` dans `.env` |
| `promote` : image introuvable | le commit taggué n'a pas de CI vert sur `main` |
| Pas d'adresses sur la carte | `ORS_API_KEY` vide : la liste locale de villes est utilisée |
