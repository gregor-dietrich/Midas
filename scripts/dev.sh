#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1}

if [[ -z "$MODULE" ]]; then
    echo "Usage: make dev-api or make dev-gui"
    exit 1
fi

case $MODULE in
    "api")
        MODULE_DIR="midas-api"
        ;;
    "gui")
        MODULE_DIR="midas-gui"
        ;;
    *)
        echo "Invalid module: $MODULE. Use 'api' or 'gui'."
        exit 1
        ;;
esac

echo "Starting Quarkus dev mode for $MODULE..."
cd "$PROJECT_ROOT/$MODULE_DIR"

${MVN_CMD} quarkus:dev
