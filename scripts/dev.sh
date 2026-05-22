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
# Run from the project root (like scripts/test.sh and lint.sh) and select the module
# with -pl, rather than cd-ing into it. Maven only reads .mvn/jvm.config from the
# directory it is launched in, and the Ubuntu-packaged /usr/bin/mvn does not walk up
# to find it. Those JVM args carry the jdk.compiler --add-exports that ErrorProne
# needs to compile in-process; without them dev-mode auto-compile fails.
cd "$PROJECT_ROOT"

${MVN_CMD} -pl "$MODULE_DIR" quarkus:dev
