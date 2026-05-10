#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"api"}

set -e

echo "Starting development mode..."

case $MODULE in
    "api")
        echo "=== Starting Midas API in dev mode ==="
        cd "$PROJECT_ROOT/midas-api"
        ${MVN_CMD} quarkus:dev
        cd - > /dev/null
        ;;
    "gui")
        echo "=== Starting Midas GUI in dev mode ==="
        cd "$PROJECT_ROOT/midas-gui"
        ${MVN_CMD} quarkus:dev
        cd - > /dev/null
        ;;
    *)
        echo "Usage: $0 [api|gui]"
        echo "  api - Start Midas API in dev mode (default)"
        echo "  gui - Start Midas GUI in dev mode"
        exit 1
        ;;
esac

echo "Development mode stopped."
