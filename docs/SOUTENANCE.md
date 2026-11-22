# Scénario de soutenance (10 minutes)

## Préparation (la veille)

- Serveur de test accessible, une première version `v1.0.0` déjà livrée, puis une `v1.1.0` prête.
- Deux branches prêtes : `demo/verte` (change un texte) et `demo/rouge` (un test casse volontairement).
- Onglets ouverts : GitHub (pull requests, Actions, Packages), l'application, un terminal.

### Fabriquer la branche qui échoue

Dans `api/src/test/java/com/example/covoiturage/service/PricingServiceTest.java`, changer une valeur attendue
(par exemple un `assertEquals` avec un mauvais nombre), puis :

```bash
git checkout -b demo/rouge
git commit -am "test: cas qui echoue volontairement"
git push -u origin demo/rouge        # ouvrir la pull request
```

## Déroulé

| Min | Action | À dire |
|---|---|---|
| 0-1 | Schéma d'architecture (README) | 4 services, 1 port publié, la base n'est pas exposée |
| 1-3 | **PR verte** : montrer le CI vert, les étapes, la matrice JDK 17/21, l'artefact de rapport de tests | « les images ne sont construites qu'après les tests (`needs`) » |
| 3-5 | **PR rouge** : ouvrir le job `test-java (api, 17)` en échec, montrer la **première erreur utile** dans le journal (`FAIL`, `expected`, `but was`) ; montrer que le bouton *Merge* est bloqué (règle de protection de `main`) | « `main` est protégée : CI rouge = fusion impossible » |
| 5-7 | Fusionner la PR verte, lancer `./scripts/release.sh v1.1.0`, montrer `promote` (même digest) puis `deploy` et le `/health` | « l'image n'est jamais reconstruite, elle reçoit seulement l'étiquette de version » |
| 7-9 | **Retour arrière** : workflow *Rollback* (Run workflow), puis `cat .deploy/current` | « les images de la version précédente sont déjà dans le registre » |
| 9-10 | `./scripts/check.sh static` (code retour avec `echo $?`) | « 0 = succès, non nul = échec : le même script sert au pipeline » |

## Questions probables

- **Pourquoi des micro-services ici ?** Les cartes et les emails dépendent de services externes (clé API, SMTP) : on les isole,
  on protège les secrets, et l'API reste disponible si l'un tombe (mode dégradé).
- **Pourquoi ne pas reconstruire l'image sur le tag ?** Ce qui a été testé est exactement ce qui est livré (même digest).
- **À quoi sert le `HEALTHCHECK` ?** Docker sait si le service répond vraiment, et Compose attend `healthy` avant de démarrer la suite.
- **Pourquoi la base n'a-t-elle pas de port publié ?** Seuls les services du réseau Compose en ont besoin : moins de surface d'attaque.
- **Où sont les secrets ?** Variables CI/CD masquées, fichier `.env` créé sur le serveur, jamais dans Git (`check.sh` le vérifie).
- **Que se passe-t-il si la mise en service échoue ?** `deploy.sh` contrôle `/health` et revient seul à la version précédente.
