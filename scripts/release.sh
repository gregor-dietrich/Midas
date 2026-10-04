#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/select_modules.sh" || exit
. "$DEVKIT/scripts/lib/get_maven.sh"

set -e

echo "Starting release..."
RELEASE_START=$(date +%s)

# Fail unless $1 exists locally and was created during this release: buildx without --load leaves
# the local store alone, so the tag could still name an older build (#45).
require_fresh_image() {
    local created stamp
    created=$(docker image inspect -f '{{.Created}}' "$1" 2>/dev/null) \
        || { echo "Error: no local image $1 for module $2; the buildx builder probably did not load it; aborting." >&2
            return 1; }
    stamp=$(date -d "$created" +%s) \
        || { echo "Error: cannot parse the creation time '$created' of $1 (GNU date needed); aborting." >&2; return 1; }
    if (( stamp < RELEASE_START )); then
        echo "Error: local image $1 for module $2 was created $created, before this release started; the buildx" \
            "builder probably did not load it (a docker-container driver?). Use the default builder; aborting." >&2
        return 1
    fi
}

if [[ -z "$REVISION" ]]; then
    read -r -p "Enter the new version tag [1.0.0-SNAPSHOT]: " REVISION
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
# Check every image before tagging or pushing anything, so a stale one cannot leave a half-published release
for module in $SELECTED; do
    require_fresh_image "gregordietrich/${module}:${REVISION}" "$module" || exit 1
done
make tag

# Docker push: the images `make build` built, so ONLY narrows the push as it narrowed the build.
# Every versioned tag goes first; :latest moves only once all of them are in the registry.
for module in $SELECTED; do
    TAG="gregordietrich/${module}:${REVISION}"
    echo "Pushing $TAG..."
    docker push "$TAG" || { echo "Error: failed to push $TAG; aborting before moving latest." >&2; exit 1; }
done
latest_failed=0
for module in $SELECTED; do
    IMAGE_NAME="gregordietrich/${module}"
    docker tag "${IMAGE_NAME}:${REVISION}" "${IMAGE_NAME}:latest"
    docker push "${IMAGE_NAME}:latest" || { echo "Error: failed to push ${IMAGE_NAME}:latest" >&2; latest_failed=1; }
done
if (( latest_failed )); then
    echo "Release published, but some :latest tags were not moved; push them by hand." >&2
    exit 1
fi

echo "Release completed."
