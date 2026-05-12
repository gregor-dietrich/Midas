#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Starting tests..."

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

echo "=== Testing $DISPLAY_NAME ==="
cd "$PROJECT_ROOT"

# Run Maven from the root using the reactor
${MVN_CMD} -q verify ${PL_ARG} -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}"

echo "Tests completed."
