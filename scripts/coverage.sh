#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
OPEN_REPORT=false
REVISION=${REVISION:-1.0.0-SNAPSHOT}

# Shift if module was provided as $1
if [[ "$1" != -* && -n "$1" ]]; then
    shift
fi

while getopts "o" opt; do
    case "$opt" in
        o) OPEN_REPORT=true ;;
    esac
done

set -e

echo "Starting coverage report generation..."

REPORT="COVERAGE.md"
maven_status=0

# Map module names to Maven project list and CSV targets
case $MODULE in
    "api")
        PL_ARG="-pl midas-api"
        DISPLAY_NAME="Midas API"
        CSV_FILES="midas-api/target/site/jacoco/jacoco.csv"
        ;;
    "gui")
        PL_ARG="-pl midas-gui"
        DISPLAY_NAME="Midas GUI"
        CSV_FILES="midas-gui/target/site/jacoco/jacoco.csv"
        ;;
    "both"|*)
        PL_ARG=""
        DISPLAY_NAME="All Modules"
        CSV_FILES="midas-api/target/site/jacoco/jacoco.csv midas-gui/target/site/jacoco/jacoco.csv"
        ;;
esac

echo "=== Running tests with JaCoCo for $DISPLAY_NAME ==="
cd "$PROJECT_ROOT"

# Run Maven from the root using the reactor
${MVN_CMD} -q verify ${PL_ARG} -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}" -Dmaven.test.failure.ignore=true || maven_status=$?

echo "Generating combined coverage report..."

# Filter CSV_FILES to only include existing ones
EXISTING_CSVS=""
for csv in $CSV_FILES; do
    if [ -f "$csv" ]; then
        EXISTING_CSVS="$EXISTING_CSVS $csv"
    fi
done

if [ -z "$EXISTING_CSVS" ]; then
    echo "ERROR: No JaCoCo CSV files found. Coverage report could not be generated." >&2
    exit 1
fi

python3 "scripts/coverage.py" "$REPORT" $EXISTING_CSVS

if [ "$OPEN_REPORT" = true ]; then
    xdg-open "$REPORT" 2>/dev/null || open "$REPORT" 2>/dev/null || echo "Cannot open $REPORT" >&2
fi

echo "Coverage report generated at $REPORT."

exit $maven_status
