#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
# ONLY=<module>[,<module>...] narrows the build like devkit's targets; unset, every module.
. "${DEVKIT:?run this through make}/scripts/lib/select_modules.sh" || exit
. "$DEVKIT/scripts/lib/get_maven.sh"

. scripts/lib/images.sh || exit

set -e

register_qemu

echo "Starting build..."

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi

package_modules

# Function to build Docker image for a specific module
build_docker() {
    local module_dir="$1"
    local image_name="${IMAGE_NAMESPACE}/${module_dir}"
    local dockerfile="$DOCKERFILE"
    local tag="${image_name}:${REVISION}"

    echo "=== Building Docker Image for $module_dir ==="
    cd "$PROJECT_ROOT/$module_dir"

    # Docker buildx if available
    if docker buildx version >/dev/null 2>&1; then
        echo "Using buildx to build $module_dir image..."
        if docker buildx build --platform "$PLATFORMS" -t "$tag" -f "$dockerfile" .; then
            echo "buildx multi-platform build finished for $module_dir."
        else
            echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch ($NATIVE_PLATFORM)."
            # Try loading a single-platform image into local docker. If that fails, fallback to plain docker build.
            if ! docker buildx build --load --platform "$NATIVE_PLATFORM" -t "$tag" -f "$dockerfile" .; then
                echo "--load build failed; falling back to plain 'docker build' (single-platform)."
                docker build -t "$tag" -f "$dockerfile" .
            fi
        fi
    else
        echo "buildx not available; performing plain docker build for $module_dir."
        docker build -t "$tag" -f "$dockerfile" .
    fi

    echo
    cd - > /dev/null
}

for m in $SELECTED; do
    build_docker "$m"
done

echo "Build completed."
