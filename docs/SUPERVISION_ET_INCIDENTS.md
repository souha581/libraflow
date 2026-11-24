# Guide de supervision, diagnostic d'incident et rollback (Lot 7)

## 1. Points de supervision disponibles

| Outil / Endpoint | URL ou commande | Utilité |
| :--- | :--- | :--- |
| **Sonde de santé** | `GET http://localhost:8081/actuator/health` | Vérifie l'état de l'application et la connexion PostgreSQL (`status: UP/DOWN`) |
| **Trace de version** | `GET http://localhost:8081/actuator/info` | Affiche la version déployée et le profil actif |
| **Métriques applicatives** | `GET http://localhost:8081/actuator/metrics` | Temps de réponse HTTP, JVM, connexions HikariCP, compteurs `libraflow.loans.created` / `returned` |
| **Tableau de bord Prometheus** | `http://localhost:9090` (stack test) | Visualisation des métriques scrapées toutes les 15s (`http_server_requests_seconds_count`, `jvm_memory_used_bytes`, `up`) |
| **Logs applicatifs** | `docker compose -f docker-compose.test.yml logs -f app-test` | Consultation en direct des événements métier et erreurs techniques |
| **Historique des déploiements** | `deployments/history.log` | Journal horodaté des déploiements et rollbacks réussis |

---

## 2. Scénario de simulation d'incident contrôlé (Démonstration soutenance)

Pour démontrer la détection, le diagnostic et le retour arrière lors de la soutenance :

### Étape A — Déployer une version saine `v1` (état de référence)
1. Sur `main`, vérifier que l'image stable (ex. `sha-<bon_commit>`) est déployée et que `scripts/smoke-test.sh` passe.

### Étape B — Introduire un incident contrôlé
Option 1 (configuration erronée) :
- Modifier `DB_PASSWORD` ou `DB_HOST` avec une valeur invalide dans l'environnement `test` et redémarrer `app-test`.
Option 2 (version applicative défectueuse) :
- Pousser une modification volontairement cassée sur une branche de démonstration ou lancer `app-test` avec un port/profil erroné.

### Étape C — Observer et diagnostiquer
1. **Sonde de santé & Smoke tests** :
   ```bash
   curl -i http://localhost:8081/actuator/health
   # Retourne HTTP 503 ("status":"DOWN") ou connexion refusee
   ```
2. **Logs du conteneur** :
   ```bash
   docker compose -f docker-compose.test.yml logs --tail=80 app-test
   ```
   On observe l'erreur exacte (ex. `FATAL: password authentication failed for user "libraflow_test"` ou `Unable to acquire JDBC Connection`).
3. **État des conteneurs** :
   ```bash
   docker compose -f docker-compose.test.yml ps
   ```

---

## 3. Procédure de Rollback vers une version stable

### Via GitHub Actions (recommandé)
1. Ouvrir **Actions → Rollback Test Environment → Run workflow**.
2. Saisir `target_image_tag` avec le tag stable précédent (ex. `sha-<ancien_commit>`).
3. Saisir le motif dans `reason` (ex. `Incident DB sur deploiement test`).
4. Valider l'approbation manuelle sur l'environnement `test` si activée.
5. Le workflow relance `scripts/rollback.sh`, exécute automatiquement les smoke tests et archive la preuve dans `deployments/history.log`.

### Via ligne de commande (environnement local / serveur test)
```bash
bash scripts/rollback.sh ghcr.io/<VOTRE_ORG>/libraflow:sha-<ancien_commit> <ancien_commit>
```

### Vérification post-rollback
- `curl http://localhost:8081/actuator/health` retourne `{"status":"UP",...}`
- `curl http://localhost:8081/actuator/info` confirme le retour à l'ancienne version stable
- `deployments/history.log` contient une nouvelle ligne `ROLLBACK | status=SUCCESS`.
