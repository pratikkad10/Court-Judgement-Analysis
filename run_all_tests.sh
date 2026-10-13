#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
run_java() { local service="$1"; echo "==> $service"; (cd "$service" && mvn -q test); }
if ! command -v mvn >/dev/null; then
  echo "Maven 3.9+ with Java 17 is required to run Java tests. See docs/HOW_TO_RUN.md." >&2
  exit 127
fi
run_java auth-service
run_java document-service
run_java query-service
run_java api-gateway
python3 -m pip install -q -e "./python-common[test]" requests
( cd python-common && PYTHONPATH=. python3 -m pytest -q tests )
( cd ai-ingestion-service && PYTHONPATH=../python-common python3 -m pytest -q tests )
( cd ai-rag-service && PYTHONPATH=../python-common python3 -m pytest -q tests )
python3 -m pytest -q tests/integration
( cd frontend && npm ci && npm test )
echo "All test suites passed."
