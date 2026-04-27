#!/usr/bin/env bash
# PostToolUse hook: verify checkstyle compliance on edited Java files.
# The project formatter is redhat.java (Eclipse, VS Code only — no CLI equivalent).
# Checkstyle encodes the same style rules and serves as the CLI enforcement layer.
set -euo pipefail

input=$(cat)

file=$(python3 -c "
import json, sys
data = json.load(sys.stdin)
inp = data.get('toolInput', {})
path = inp.get('filePath') or inp.get('file_path') or inp.get('path') or ''
print(path)
" <<< "$input" 2>/dev/null || echo "")

[[ "$file" == *.java ]] || exit 0
[[ -f "$file" ]] || exit 0

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

if [[ "$file" == */midas-api/* || "$file" == midas-api/* ]]; then
  cd "${REPO_ROOT}/midas-api"
  ./mvnw checkstyle:check -q --no-transfer-progress 2>&1
elif [[ "$file" == */midas-gui/* || "$file" == midas-gui/* ]]; then
  cd "${REPO_ROOT}/midas-gui"
  mvn checkstyle:check -q --no-transfer-progress 2>&1
fi
