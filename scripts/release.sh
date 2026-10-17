#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/select_modules.sh" || exit
. "$DEVKIT/scripts/lib/get_maven.sh"
. "$DEVKIT/scripts/lib/release_tag.sh" || exit
. scripts/lib/images.sh || exit

set -e

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    # End of input (Ctrl-D, or no terminal) is no answer.
    read -r -p "Enter the new version tag: " REVISION || [[ -n "$REVISION" ]] \
        || { echo "No answer; nothing was tagged or pushed." >&2; exit 1; }
fi
export REVISION
# make tag refuses any other shape, so check before the build rather than after it. The tag is also
# the Maven version and the image tag, both bare by convention, so no v prefix; the length bound is
# the image tag's, which make tag does not check.
[[ $REVISION =~ $TAG_RE && -z ${BASH_REMATCH[1]} && ${#REVISION} -le 128 ]] \
    || { echo "Error: '$REVISION' is not a release tag: X.Y.Z, optionally followed by rcN or -rcN, without leading zeros or a v prefix, at most 128 characters; nothing was tagged or pushed." >&2; exit 1; }
# A release candidate pushes only its own image tag; latest stays on the last final release.
rc=${BASH_REMATCH[5]}

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

# buildx builds every platform and pushes the versioned tag and, for a final release, latest
# together; there is no local image (#59). ONLY narrows the push as it narrows the build. A failure
# stops the release, but the modules before it stay published.
for module in $SELECTED; do
    image="${IMAGE_NAMESPACE}/${module}"
    tags=("${image}:${REVISION}")
    [[ -n $rc ]] || tags+=("${image}:latest")
    push_image "$module" "$module/$DOCKERFILE" "${tags[@]}" \
        || { echo "Error: building or pushing ${image} failed; aborting the release." >&2; exit 1; }
done

echo "Release completed."
