#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}

set -e

echo "Starting clean..."

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
${MVN_CMD} -q clean ${PL_ARG}

echo "Clean completed."
