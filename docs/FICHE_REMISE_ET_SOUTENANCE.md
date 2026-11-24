# Guide de soutenance & Fiche synthétique de remise

## 1. Plan de démonstration finale (Section 10 de l'énoncé)

1. **Présentation & Git** : montrer le dépôt GitHub, les branches (`main`, `develop`, `feature/*`), la protection de `main` et une Pull Request relue par le binôme.
2. **Modification en direct** : modifier un texte ou ajouter un test sur une branche, pousser / ouvrir une PR.
3. **Déclenchement CI** : montrer dans **GitHub Actions** les 3 jobs (`quality-and-build`, `security-scan`, `container-build-and-publish`).
4. **Tests & qualité** : montrer les logs Checkstyle, SpotBugs, JUnit/MockMvc et l'artefact de couverture JaCoCo.
5. **Artefact & Registre** : montrer l'artefact JAR versionné dans l'onglet Actions et l'image taguée publiée dans **GitHub Packages (GHCR)**.
6. **Déploiement en test** : montrer le job `cd.yml` (approbation manuelle sur l'environnement `test`), l'application démarrée et le passage de `scripts/smoke-test.sh`.
7. **Sécurité (DevSecOps)** : présenter le rapport Gitleaks (0 secret détecté) et un rapport Trivy (`trivy-image-report.txt` / `trivy-fs-report.txt`) en expliquant une alerte analysée ou corrigée.
8. **Incident & Rollback** : déclencher l'incident contrôlé, afficher les logs d'erreur (`docker compose -f docker-compose.test.yml logs app-test`), exécuter le rollback via `rollback.yml` (ou `scripts/rollback.sh`) et vérifier le retour à `UP` sur `/actuator/health`.
9. **Bilan** : expliquer les choix techniques, les indicateurs relevés et les améliorations possibles (ex. SonarQube, Kubernetes, alerting Slack/Email).

---

## 2. Indicateurs de suivi DevOps (Section 13)

| Indicateur | Valeur relevée | Commentaire |
| :--- | :--- | :--- |
| Taux de réussite des pipelines CI | *ex. 92 %* | Relevé sur l'historique GitHub Actions |
| Durée moyenne d'exécution du pipeline | *ex. ~2 min 30 s* | Grâce au cache Maven & Docker Buildx |
| Taux de couverture des tests (JaCoCo) | *ex. > 70 %* | Seuil minimum imposé à 60 % |
| Défauts détectés avant déploiement | *ex. Checkstyle/SpotBugs* | Bloqués avant fusion dans `main` |
| Vulnérabilités détectées / traitées | *ex. Trivy + Gitleaks* | Voir rapports archivés |
| Délai de restauration (Rollback) | *ex. < 60 s* | Via image taguée immuable dans GHCR |

---

## 3. Fiche synthétique de remise (Section 15)

| Information | À compléter par le binôme |
| :--- | :--- |
| **Nom(s) et prénom(s)** | Étudiant A : ... / Étudiant B : ... |
| **Intitulé de l'application** | LibraFlow — Gestion d'une bibliothèque universitaire |
| **Lien du dépôt Git** | `https://github.com/<VOTRE_COMPTE>/libraflow` |
| **Technologies de l'application** | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, Thymeleaf, PostgreSQL, Maven |
| **Plateforme CI/CD utilisée** | GitHub Actions |
| **Registre d'images / artefacts** | GitHub Container Registry (`ghcr.io`) & GitHub Actions Artifacts |
| **Environnement de test et procédure d'accès** | Stack Docker Compose (`docker-compose.test.yml`) sur port `8081` + Prometheus `9090` |
| **Contrôle(s) de sécurité intégré(s)** | Gitleaks (secrets), Trivy (dépendances & image Docker), SpotBugs (analyse statique) |
| **Procédure de rollback** | Workflow `rollback.yml` + `scripts/rollback.sh` vers le tag stable précédent |
| **Date de remise** | *JJ/MM/2026* |
