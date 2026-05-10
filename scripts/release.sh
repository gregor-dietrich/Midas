#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new version tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi
export REVISION

cd "$PROJECT_ROOT"

git fetch origin
git switch main
git pull

# Run full verification via Makefile
make clean
make install
make test
make build
make tag

# Docker push
if command -v docker &> /dev/null; then
    IMAGE_PREFIX="gregordietrich/midas"
    for module in api gui; do
        IMAGE_NAME="${IMAGE_PREFIX}-${module}"
        TAG="${IMAGE_NAME}:${REVISION}"
        
        echo "Pushing Docker images for $module..."
        docker push "$TAG" || echo "Warning: Failed to push $TAG"
        
        # Tag and push latest
        docker tag "$TAG" "${IMAGE_NAME}:latest"
        docker push "${IMAGE_NAME}:latest" || echo "Warning: Failed to push ${IMAGE_NAME}:latest"
    done
fi

echo "Release completed."

cd - > /dev/null
