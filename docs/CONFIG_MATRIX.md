# Matrice des configurations, secrets et DevSecOps (Lot 5)

## 1. Séparation des environnements

| Paramètre | Développement (`dev`) | Intégration CI (`ci`) | Environnement de Test (`test`) |
| :--- | :--- | :--- | :--- |
| Profil Spring | `dev` | `ci` | `test` |
| Base de données | PostgreSQL (`docker-compose.yml`) | H2 en mémoire (mode PostgreSQL) | PostgreSQL dédié (`docker-compose.test.yml`) |
| Port HTTP | `8080` | Éphémère (`MockMvc`) | `8081` |
| Logs | Console + `./logs/libraflow.log` (`DEBUG`) | `target/test-logs/libraflow-ci.log` | Volume Docker `libraflow_test_logs` (`INFO`) |
| Source de l'image | Build local `Dockerfile` | N/A | Registre `ghcr.io/<owner>/libraflow:<tag>` |

## 2. Variables d'environnement et secrets

> **Règle stricte** : aucun mot de passe, clé API ou fichier `.env` réel n'est versionné dans Git. Seul `.env.example` contient des valeurs factices.

| Variable | Sensible ? | Où la configurer ? | Rôle |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Non | `docker-compose*.yml` | Sélectionne le profil Spring (`dev`, `ci`, `test`) |
| `APP_VERSION` | Non | Pipeline / `.env` | Trace la version affichée dans `/actuator/info` et l'UI |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` | Non | `docker-compose*.yml` / GitHub Vars | Paramètres de connexion PostgreSQL |
| `DB_PASSWORD` | **Oui (Secret)** | `.env` local / GitHub Secret `TEST_DB_PASSWORD` | Mot de passe de la base PostgreSQL |
| `APP_ADMIN_PASSWORD` | **Oui (Secret)** | `.env` local / GitHub Secret `TEST_ADMIN_PASSWORD` | Mot de passe initial du compte `ADMIN` (si seed activé) |
| `APP_MEMBER_PASSWORD` | **Oui (Secret)** | `.env` local / GitHub Secret `TEST_MEMBER_PASSWORD` | Mot de passe initial du compte `MEMBER` (si seed activé) |
| `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` | **Oui (Secret)** | GitHub Environment `test` Secrets | Accès SSH si déploiement sur VM distante (optionnel) |
| `GITHUB_TOKEN` | **Oui (Secret)** | Fourni automatiquement par GitHub Actions | Authentification vers GHCR (`packages: write/read`) |

## 3. Contrôles de sécurité intégrés (DevSecOps)

1. **Détection de secrets (`Gitleaks`)** : exécuté à chaque push/PR sur l'historique Git complet dans `ci.yml`.
2. **Analyse des dépendances (`Trivy fs`)** : inspecte `pom.xml` et les bibliothèques tierces à la recherche de CVE connues (`trivy-fs-report.txt`).
3. **Scan de l'image conteneur (`Trivy image`)** : analyse l'image Docker construite (OS + JRE + JAR) avant publication sur GHCR (`trivy-image-report.txt`).
4. **Durcissement conteneur (`Dockerfile`)** : exécution sous utilisateur non-root (`appuser`), image multi-stage sans compilateur ni code source final.

### Démarche d'analyse et de remédiation d'alerte
Lors de chaque exécution de la CI :
1. Télécharger l'artefact `trivy-image-report` ou `trivy-fs-report` depuis l'onglet **Actions**.
2. Identifier une CVE `HIGH` ou `CRITICAL` (ou une alerte SpotBugs/Checkstyle).
3. Mettre à jour la version de la dépendance ou de l'image de base dans `pom.xml` / `Dockerfile` via une PR `fix/security-...`, ou documenter la justification si aucun correctif amont n'est disponible (ex. vulnérabilité non exploitable dans notre contexte).
