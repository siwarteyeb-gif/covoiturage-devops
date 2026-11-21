# Covoiturage Étudiant : front-end Angular (Sprints 1, 2 et 3)

Compatible avec le back-end `covoiturage-backend-sprint2-3`.

## Lancer

1. Démarrer le back-end (port 8080) et MySQL.
2. Dans ce dossier :

```bash
npm install --legacy-peer-deps
npm start
```

Ouvrir http://localhost:4200. Les appels `/api/...` sont redirigés vers http://localhost:8080
par `proxy.conf.json` (pas de problème CORS).

Tests : `npm test -- --watch=false`

## Pages

| Route | Accès | Fonctionnalités |
|---|---|---|
| `/` | public | accueil |
| `/login`, `/register` | visiteurs | F01, F02 |
| `/profile` | connecté | F03, F25 (profil, mot de passe, avis reçus) |
| `/vehicles` | connecté | F06, F07 |
| `/trips/search` | connecté | F12, F13, F14 (recherche, filtres, détour) |
| `/trips/new` | connecté | F08, F09, F10, F15 (publication, autocomplétion, prix, récurrence) |
| `/trips/mine` | connecté | mes trajets de conducteur |
| `/trips/:id` | connecté | F11, F16, F17, F18, F19, F22, F24 (carte, réservation, gestion, statuts, avis) |
| `/bookings` | connecté | F18, F19, F21, F24 (mes réservations et demandes reçues) |
| `/admin` | admin | F26, F27, F28 (tableau de bord, utilisateurs, trajets, avis) |

## Organisation (`src/app`)

- `core/` : modèles, services HTTP, intercepteur JWT, guards (connecté, visiteur, admin), utilitaires
- `shared/` : navbar, carte Leaflet, champ d'adresse avec autocomplétion, badge, formulaire d'avis
- `features/` : home, auth, profile, vehicles, trips, bookings, admin

La carte utilise Leaflet avec le fond OpenStreetMap (une connexion Internet est nécessaire dans le navigateur).

## Docker

Le `Dockerfile` construit le site et le sert avec nginx, qui transmet `/api` au service `backend`.
