#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Starting lint checks..."

# Map module names to Maven project list
case $MODULE in
    "api")
        PL_ARG="-pl midas-api"
        DISPLAY_NAME="Midas API"
        ;;
    "gui")
        PL_ARG="-pl midas-gui"
        DISPLAY_NAME="Midas GUI"
        ;;
    "both"|*)
        PL_ARG=""
        DISPLAY_NAME="All Modules"
        ;;
esac

echo "=== Linting $DISPLAY_NAME ==="
cd "$PROJECT_ROOT"

# Run Maven from the root using the reactor
${MVN_CMD} -q compile test-compile spotless:check checkstyle:check spotbugs:check pmd:check pmd:cpd-check ${PL_ARG} -Drevision="${REVISION}"

echo "Lint checks completed."
