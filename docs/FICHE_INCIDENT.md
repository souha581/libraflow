# Fiche d'Incident — LibraFlow (Modèle & Exemple)

| Champ | Détail |
| :--- | :--- |
| **Identifiant incident** | `INC-2026-001` |
| **Date / Heure détection** | *À compléter lors de l'exercice* |
| **Environnement impacté** | `test` (`http://localhost:8081`) |
| **Version en cause** | `sha-<commit_defectueux>` |
| **Détecté par** | Échec des smoke tests post-déploiement / sonde `/actuator/health` (`503` / timeout) |

## 1. Symptômes observés
- L'endpoint `/actuator/health` ne répond plus `UP` dans le délai imparti par `scripts/smoke-test.sh`.
- Le pipeline CD marque le job `deploy-test` en échec.

## 2. Diagnostic (extrait des logs et métriques)
Commande exécutée :
```bash
docker compose -f docker-compose.test.yml logs --tail=50 app-test
```
Cause racine identifiée :
- Exemple : *Erreur de configuration de la base de données dans la variable d'environnement `DB_PASSWORD` / régression introduite dans le commit `<sha>` empêchant le démarrage du contexte Spring Boot.*

## 3. Action corrective immédiate (Rollback)
- Exécution du workflow GitHub Actions **Rollback Test Environment** vers le dernier tag stable validé `sha-<commit_stable>`.
- Temps de restauration (MTTR) : `< 1 minute`.
- Vérification via `scripts/smoke-test.sh` : `/actuator/health` (`UP`), `/login` (`200`), `/api/books` (`200`).

## 4. Actions préventives
- Vérification systématique des variables d'environnement requises avant bascule.
- Ajout d'un test d'intégration ou de non-régression couvrant le scénario défaillant.
