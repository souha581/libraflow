#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
MAX_RETRIES="${MAX_RETRIES:-24}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

echo "[smoke-test] Checking health probe at ${BASE_URL}/actuator/health ..."
for i in $(seq 1 "${MAX_RETRIES}"); do
  if curl -fsS "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
    echo "[smoke-test] Health check PASSED (attempt ${i}/${MAX_RETRIES})."
    break
  fi
  if [ "${i}" -eq "${MAX_RETRIES}" ]; then
    echo "[smoke-test] ERROR: Health check failed after ${MAX_RETRIES} attempts."
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
done

echo "[smoke-test] Checking application info endpoint..."
curl -fsS "${BASE_URL}/actuator/info" | head -c 500
echo ""

echo "[smoke-test] Checking login page availability..."
HTTP_LOGIN=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/login")
if [ "${HTTP_LOGIN}" != "200" ]; then
  echo "[smoke-test] ERROR: /login returned HTTP ${HTTP_LOGIN}"
  exit 1
fi

echo "[smoke-test] Checking public API /api/books..."
HTTP_BOOKS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/books")
if [ "${HTTP_BOOKS}" != "200" ]; then
  echo "[smoke-test] ERROR: /api/books returned HTTP ${HTTP_BOOKS}"
  exit 1
fi

echo "[smoke-test] ALL SMOKE TESTS PASSED on ${BASE_URL}"
