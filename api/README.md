# Covoiturage Étudiant : back-end (Sprints 1, 2 et 3)

API REST Spring Boot 4 / Java 17 / MySQL.

## Lancer

1. Démarrer MySQL (la base `covoiturage_db` est créée automatiquement).
2. Si le mot de passe MySQL n'est pas vide : `$env:DB_PASSWORD="..."` (PowerShell).
3. `.\mvnw spring-boot:run`  →  http://localhost:8080

Tests (sans MySQL, base H2 en mémoire) : `.\mvnw test`

Avec Docker : `docker compose up --build` (MySQL + back-end).

## Variables d'environnement (toutes facultatives)

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | accès MySQL | `root` / vide |
| `JWT_SECRET` | secret du JWT (32 caractères minimum) | valeur de développement |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | compte admin créé au démarrage | `admin@covoiturage.tn` / `Admin@12345` |
| `ORS_API_KEY` | clé OpenRouteService (https://openrouteservice.org) | vide = mode démo |
| `MAIL_ENABLED` | envoyer de vrais emails | `false` (affichés dans la console) |

**Mode démo sans clé OpenRouteService** : l'autocomplétion utilise un petit annuaire de villes
tunisiennes et la distance est estimée avec la formule de Haversine. Avec une clé, l'itinéraire réel
est utilisé. Le champ `source` de `/api/maps/route` indique lequel est utilisé.

## Swagger (facultatif)

`.\mvnw spring-boot:run -Pswagger` puis http://localhost:8080/swagger-ui.html
(profil optionnel : la dépendance n'est pas chargée par défaut).

## Routes

| Sprint | Routes |
|---|---|
| 1 | `POST /api/auth/register`, `/login` ; `GET/PUT /api/users/me`, `PUT /api/users/me/password` ; `/api/vehicles` |
| 2 | `GET /api/maps/geocode`, `GET /api/maps/route` ; `POST /api/trips`, `GET /api/trips/search`, `/me`, `/{id}`, `PUT /{id}`, `PATCH /{id}/status` |
| 3 | `POST/GET /api/trips/{id}/bookings`, `GET /api/bookings/me`, `/received`, `PATCH /api/bookings/{id}/accept\|reject\|cancel`, `POST /api/bookings/{id}/review`, `GET /api/users/{id}/reviews`, `/api/admin/**` |

Toutes les requêtes de test sont dans `http/sprint1.http` et `http/sprint2-3.http`.
