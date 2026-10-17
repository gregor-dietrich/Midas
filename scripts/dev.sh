#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"

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
# Run from the project root (like devkit's test.sh and lint.sh) and select the module
# with -pl, rather than cd-ing into it, so .mvn/jvm.config is read even by a Maven
# that does not walk up to find it. Those JVM args carry the jdk.compiler --add-exports
# that ErrorProne needs to compile in-process; without them dev-mode auto-compile fails.
"${MVN_CMD}" -pl "$MODULE_DIR" quarkus:dev
