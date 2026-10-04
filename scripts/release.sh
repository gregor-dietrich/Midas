#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/select_modules.sh" || exit
. "$DEVKIT/scripts/lib/get_maven.sh"
. scripts/lib/images.sh || exit

set -e

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    read -r -p "Enter the new version tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi
export REVISION
[[ $REVISION =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] \
    || { echo "Error: '$REVISION' is not a valid image tag; nothing was tagged or pushed." >&2; exit 1; }

# A host that cannot build every platform fails here, before the tests, the tag or any push (#59).
register_qemu
require_multiplatform_builder || { echo "Aborting the release; nothing was tagged or pushed." >&2; exit 1; }

git fetch origin
git switch main
git pull
# Run the release with the scripts just pulled, not the ones this run started from.
[[ -n "${RELEASE_REEXEC:-}" ]] || RELEASE_REEXEC=1 exec scripts/release.sh

# Run full verification via Makefile, then package the jars the images copy
make clean
make install
make test
package_modules
make tag

# buildx builds every platform and pushes the versioned tag and latest together; there is no local
# image (#59). ONLY narrows the push as it narrows the build. A failure stops the release, but the
# modules before it stay published.
for module in $SELECTED; do
    image="${IMAGE_NAMESPACE}/${module}"
    push_image "$module" "$module/$DOCKERFILE" "${image}:${REVISION}" "${image}:latest" \
        || { echo "Error: building or pushing ${image} failed; aborting the release." >&2; exit 1; }
done

echo "Release completed."
