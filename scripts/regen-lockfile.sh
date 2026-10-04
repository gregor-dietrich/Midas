#!/bin/bash

# Updates the frontend lockfile to package.json with the pinned Node.js, for when the build's
# npm ci refuses a package.json that no longer matches it (a Vaadin bump, an @NpmPackage change).
# A production build runs npm install instead of npm ci: prepare-frontend updates package.json,
# build-frontend installs it, and npm changes only the lockfile entries package.json no longer
# matches. The lockfile is kept rather than recreated: a fresh one is seeded from the lockfile
# bundled with Vaadin and moves unrelated packages, some to older versions.

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"

set -e

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Updating $FRONTEND_DIR/package-lock.json..."

# Vaadin skips npm while node_modules or a compatible bundle exists, whatever the lockfile says.
rm -rf "$FRONTEND_DIR/node_modules" "$FRONTEND_DIR/src/main/bundles/prod.bundle"

${MVN_CMD} -q clean compile -pl "$FRONTEND_DIR" -Pproduction -Dvaadin.ci.build=false -Drevision="${REVISION}"

echo "Lockfile updated. Review the diff before committing."
