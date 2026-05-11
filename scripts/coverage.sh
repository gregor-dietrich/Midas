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

REPORT=".coverage.md"
maven_status=0

# Function to run tests with coverage for a module
run_tests() {
    local module_dir="$1"
    local module_name="$2"

    echo "=== Running tests with JaCoCo for $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"

    ${MVN_CMD} -q verify -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}" -Dmaven.test.failure.ignore=true || maven_status=$?
}

case $MODULE in
    "api")
        run_tests "midas-api" "Midas API"
        CSV_FILES="midas-api/target/site/jacoco/jacoco.csv"
        ;;
    "gui")
        run_tests "midas-gui" "Midas GUI"
        CSV_FILES="midas-gui/target/site/jacoco/jacoco.csv"
        ;;
    "both"|*)
        run_tests "midas-api" "Midas API"
        run_tests "midas-gui" "Midas GUI"
        CSV_FILES="midas-api/target/site/jacoco/jacoco.csv midas-gui/target/site/jacoco/jacoco.csv"
        ;;
esac

cd "$PROJECT_ROOT"

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
