#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if /usr/libexec/java_home -v 25 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
  fi
fi

echo "==> Running seeded E2E (file → incidents → fake AI)"
./mvnw -q -Dtest=LocalSeededEndToEndIT test
echo "==> OK"
