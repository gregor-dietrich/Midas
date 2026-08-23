#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

# Run environment check first
"$DIR/check.sh" "$MODULE"

echo "Starting install..."

case $MODULE in
    "api")
        PL_ARG="-pl midas-api"
        ;;
    "gui")
        PL_ARG="-pl midas-gui"
        ;;
    "both"|*)
        PL_ARG=""
        ;;
esac

cd "$PROJECT_ROOT"
${MVN_CMD} -q clean install ${PL_ARG} -DskipTests -Drevision="${REVISION}"

# The Vaadin jars are resolved by the build above, which is what the gate reads.
if [ "$MODULE" != "api" ]; then
    python3 "$PROJECT_ROOT/scripts/check_frontend_deps.py" midas-gui
fi

echo "Install completed."
