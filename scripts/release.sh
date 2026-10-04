#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/select_modules.sh" || exit
. "$DEVKIT/scripts/lib/get_maven.sh"

set -e

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new version tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi
export REVISION

git fetch origin
git switch main
git pull

# Run full verification via Makefile
make clean
make install
make test
make build
make tag

# Docker push: the images `make build` built, so ONLY narrows the push as it narrowed the build
if command -v docker &> /dev/null; then
    for module in $SELECTED; do
        IMAGE_NAME="gregordietrich/${module}"
        TAG="${IMAGE_NAME}:${REVISION}"
        
        echo "Pushing Docker images for $module..."
        docker push "$TAG" || echo "Warning: Failed to push $TAG"
        
        # Tag and push latest
        docker tag "$TAG" "${IMAGE_NAME}:latest"
        docker push "${IMAGE_NAME}:latest" || echo "Warning: Failed to push ${IMAGE_NAME}:latest"
    done
fi

echo "Release completed."
