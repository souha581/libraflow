#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/deploy-test.sh <full_image_ref> <app_version>
IMAGE_REF="${1:?Usage: deploy-test.sh <image_ref> <app_version>}"
APP_VERSION_TAG="${2:-unknown}"
BASE_URL="${TEST_BASE_URL:-http://localhost:8081}"

export LIBRAFLOW_IMAGE="${IMAGE_REF}"
export APP_VERSION="${APP_VERSION_TAG}"

echo "[deploy-test] Pulling image ${LIBRAFLOW_IMAGE}..."
docker compose -f docker-compose.test.yml pull app-test

echo "[deploy-test] Starting test stack..."
docker compose -f docker-compose.test.yml up -d

echo "[deploy-test] Running post-deployment smoke tests..."
bash scripts/smoke-test.sh "${BASE_URL}"

mkdir -p deployments
echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") | DEPLOY | image=${IMAGE_REF} | version=${APP_VERSION_TAG} | status=SUCCESS" >> deployments/history.log
echo "[deploy-test] Deployment recorded in deployments/history.log"
