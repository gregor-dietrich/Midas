#!/usr/bin/env bash
# Stop hook: run checkstyle + SpotBugs + tests before the session ends.
# Only triggers when Java source files were modified in this session.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

changed=$(git diff --name-only HEAD 2>/dev/null; git diff --name-only 2>/dev/null) || true

api_changed=$(echo "$changed" | grep '^midas-api/.*\.java$' || true)
gui_changed=$(echo "$changed" | grep '^midas-gui/.*\.java$' || true)

[[ -n "$api_changed" || -n "$gui_changed" ]] || exit 0

if [[ -n "$api_changed" ]]; then
  echo "==> [midas-api] running checkstyle + SpotBugs + tests..." >&2
  cd "${REPO_ROOT}/midas-api"
  ./mvnw checkstyle:check spotbugs:check test --no-transfer-progress 2>&1
  cd "${REPO_ROOT}"
fi

if [[ -n "$gui_changed" ]]; then
  echo "==> [midas-gui] running checkstyle + SpotBugs + tests..." >&2
  cd "${REPO_ROOT}/midas-gui"
  mvn checkstyle:check com.github.spotbugs:spotbugs-maven-plugin:check test --no-transfer-progress 2>&1
  cd "${REPO_ROOT}"
fi
