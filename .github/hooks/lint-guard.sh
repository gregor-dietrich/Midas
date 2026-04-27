#!/usr/bin/env bash
# PreToolUse hook: block Maven invocations that skip quality gates.
# Rules:
#   1. Block -Dcheckstyle.skip=true  — checkstyle must always run.
#   2. Block -Dspotbugs.skip=true    — SpotBugs must always run.
#   3. Block reverting checkstyle severity back to "warning".
set -euo pipefail

input=$(cat)

command=$(python3 -c "
import json, sys
data = json.load(sys.stdin)
inp = data.get('toolInput', {})
print(inp.get('command', '') or inp.get('cmd', ''))
" <<< "$input" 2>/dev/null || echo "")

new_content=$(python3 -c "
import json, sys
data = json.load(sys.stdin)
inp = data.get('toolInput', {})
print(inp.get('new_str', '') or inp.get('content', '') or inp.get('newContent', ''))
" <<< "$input" 2>/dev/null || echo "")

file=$(python3 -c "
import json, sys
data = json.load(sys.stdin)
inp = data.get('toolInput', {})
print(inp.get('path', '') or inp.get('filePath', '') or inp.get('file_path', ''))
" <<< "$input" 2>/dev/null || echo "")

deny() {
  python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput': {'hookEventName': 'PreToolUse', 'permissionDecision': 'deny', 'permissionDecisionReason': sys.argv[1]}}))" "$1"
  exit 0
}

# Rule 1: block skipping checkstyle
if [[ "$command" =~ -Dcheckstyle\.skip=true ]]; then
  deny "Checkstyle must not be skipped. Remove -Dcheckstyle.skip=true and fix any violations instead."
fi

# Rule 2: block skipping SpotBugs
if [[ "$command" =~ -Dspotbugs\.skip=true ]]; then
  deny "SpotBugs must not be skipped. Remove -Dspotbugs.skip=true and fix any violations instead."
fi

# Rule 3: block reverting checkstyle severity to warning
if [[ "$file" == *checkstyle.xml ]] && [[ "$new_content" =~ severity.*warning ]]; then
  deny "Do not revert checkstyle severity to 'warning'. Severity must remain 'error' so violations fail the build."
fi
