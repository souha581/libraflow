# Guide de contribution et stratégie Git (Lot 1)

Ce document définit les règles de gestion du code source et de collaboration pour le binôme.

## 1. Structure du dépôt

- `src/main/java` : code source Spring Boot (`model`, `repository`, `service`, `security`, `controller`, `config`)
- `src/main/resources` : configurations par environnement (`application*.yml`), CSS et templates Thymeleaf
- `src/test` : tests unitaires, tests d'intégration/API et profil CI (`application-ci.yml`)
- `config/` : configurations Checkstyle, SpotBugs et Prometheus
- `scripts/` : scripts d'automatisation (smoke tests, déploiement, rollback)
- `.github/workflows/` : pipelines CI, CD et Rollback as Code
- `docs/` : livrables documentaires DevOps

## 2. Stratégie de branches

| Branche | Rôle | Protection recommandée sur GitHub |
| :--- | :--- | :--- |
| `main` | Version stable prête pour l'environnement `test` | Pull Request obligatoire + 1 revue du binôme + CI verte |
| `develop` | Branche d'intégration continue du binôme | Pull Request obligatoire + CI verte |
| `feature/<nom>` | Développement d'une fonctionnalité ou tâche DevOps | Fusionnée vers `develop` via PR |
| `fix/<nom>` | Correction de bug ou d'alerte sécurité | Fusionnée vers `develop` ou `main` via PR |

### Configuration de la protection sur GitHub (à activer dans `Settings → Branches`)
1. Ajouter une règle sur `main` (et `develop`).
2. Cocher **Require a pull request before merging** (`Required approvals: 1`).
3. Cocher **Require status checks to pass before merging** et sélectionner :
   - `Build, Lint, Static Analysis & Tests`
   - `DevSecOps (Secrets & Dependencies)`
4. Cocher **Do not allow bypassing the above settings**.

## 3. Convention de commits (Conventional Commits)

Chaque message de commit doit suivre le format `<type>(<portée>): <description>` :
- `feat(loans): ajout de la verification de stock lors de l'emprunt`
- `fix(security): mise a jour d'une dependance vulnerable`
- `ci(actions): ajout du scan Trivy sur l'image Docker`
- `test(api): ajout du test d'integration emprunt et retour`
- `docs(ops): redaction de la procedure de rollback`

## 4. Workflow Pull Request en binôme

1. Créer une branche depuis `develop` : `git checkout -b feature/ma-tache`
2. Développer et tester localement : `mvn checkstyle:check && mvn verify -Dspring.profiles.active=ci`
3. Pousser la branche et ouvrir une Pull Request vers `develop` (ou `main` pour une release).
4. L'autre membre du binôme relit le code via la checklist du template PR et approuve la fusion après passage de la CI.
