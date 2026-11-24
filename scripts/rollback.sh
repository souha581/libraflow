#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/rollback.sh <previous_stable_image_ref> <previous_version>
TARGET_IMAGE="${1:?Usage: rollback.sh <previous_stable_image_ref> <previous_version>}"
TARGET_VERSION="${2:-rollback}"
BASE_URL="${TEST_BASE_URL:-http://localhost:8081}"

echo "[rollback] Reverting test environment to stable image: ${TARGET_IMAGE}"
export LIBRAFLOW_IMAGE="${TARGET_IMAGE}"
export APP_VERSION="${TARGET_VERSION}"

docker compose -f docker-compose.test.yml pull app-test
docker compose -f docker-compose.test.yml up -d --force-recreate app-test

echo "[rollback] Verifying restored service via smoke tests..."
bash scripts/smoke-test.sh "${BASE_URL}"

mkdir -p deployments
echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") | ROLLBACK | image=${TARGET_IMAGE} | version=${TARGET_VERSION} | status=SUCCESS" >> deployments/history.log
echo "[rollback] Rollback completed and recorded in deployments/history.log"
