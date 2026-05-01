#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new version tag [${LATEST_VERSION}]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
    if [[ -z "$REVISION" ]]; then
        echo "Version tag is required. Exiting."
        exit 1
    fi
    export REVISION
fi

cd "$PROJECT_ROOT"

git fetch origin
git switch main
git pull

make clean
make install
make test
make build

make tag

cd - > /dev/null
