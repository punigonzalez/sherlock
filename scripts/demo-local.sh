#!/usr/bin/env bash
# Seeded MVP demo: file fixture → incidents → fake AI investigation (JUnit E2E).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1 && /usr/libexec/java_home -v 25 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
  elif [[ -d /Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home ]]; then
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
  fi
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "ERROR: JAVA_HOME for JDK 25 is required." >&2
  exit 1
fi

echo "==> JAVA_HOME=$JAVA_HOME"
echo "==> Running seeded E2E (file → incidents → fake AI)"
./mvnw -q -Dtest=LocalSeededEndToEndIT test
echo "==> OK — see docs in Cursor store: docs/local-demo.md for full app run"
