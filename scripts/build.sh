#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

PLATFORMS="linux/amd64,linux/arm64"
MODULE=${1:-"both"}

# Detect native platform
NATIVE_ARCH_RAW="$(uname -m)"
if [[ "$NATIVE_ARCH_RAW" == "x86_64" ]]; then
    NATIVE_PLATFORM="linux/amd64"
elif [[ "$NATIVE_ARCH_RAW" == "aarch64" || "$NATIVE_ARCH_RAW" == "arm64" ]]; then
    NATIVE_PLATFORM="linux/arm64"
else
    NATIVE_PLATFORM="linux/amd64" # Default fallback
fi

set -e

# Run environment check first
"$DIR/check.sh" "$MODULE"

# Register QEMU binfmt handlers only for non-native target platforms
if docker buildx version >/dev/null 2>&1; then
    _binfmt_install_targets=""
    for _platform in ${PLATFORMS//,/ }; do
        _arch="${_platform#linux/}"
        case "$_arch" in
            amd64) _qemu_entry="qemu-x86_64"  ;;
            arm64) _qemu_entry="qemu-aarch64" ;;
            *)     continue ;;
        esac
        [[ "$NATIVE_PLATFORM" == "$_platform" ]] && continue
        grep -q "enabled" "/proc/sys/fs/binfmt_misc/${_qemu_entry}" 2>/dev/null && continue
        _binfmt_install_targets="${_binfmt_install_targets} ${_arch}"
    done
    _binfmt_install_targets="${_binfmt_install_targets# }"

    if [[ -n "$_binfmt_install_targets" ]]; then
        echo "Registering QEMU binfmt handlers for: ${_binfmt_install_targets}"
        # Image is pinned to a specific digest to prevent supply chain attacks from a mutable tag.
        if docker run --privileged --rm \
            tonistiigi/binfmt:qemu-v10.2.1@sha256:d3b963f787999e6c0219a48dba02978769286ff61a5f4d26245cb6a6e5567ea3 \
            --install "${_binfmt_install_targets}" >/dev/null 2>&1; then
            echo "QEMU binfmt handlers installed."
        else
            echo "Warning: failed to install QEMU binfmt handlers; ${_binfmt_install_targets} builds may fail on this host."
        fi
    else
        echo "QEMU binfmt handlers already registered or not required."
    fi
fi

echo "Starting build..."

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi

# Map module names to Maven project list
case $MODULE in
    "api")
        PL_ARG="-pl midas-api"
        MODULES_TO_DOCKER="midas-api"
        ;;
    "gui")
        PL_ARG="-pl midas-gui"
        MODULES_TO_DOCKER="midas-gui"
        ;;
    "both"|*)
        PL_ARG=""
        MODULES_TO_DOCKER="midas-api midas-gui"
        ;;
esac

echo "=== Packaging Application ==="
cd "$PROJECT_ROOT"

# Determine production profile for GUI
GUI_PROFILE=""
if [[ "$MODULE" == "gui" || "$MODULE" == "both" ]]; then
    GUI_PROFILE="-Pproduction"
    # Build the frontend from the committed lockfile (#15): Vaadin reuses a "compatible" bundle and
    # skips npm install when package.json is unchanged, ignoring the lockfile in both checks, so a
    # lockfile-only bump would otherwise ship the old dependency.
    rm -rf midas-gui/src/main/bundles/prod.bundle midas-gui/node_modules
fi

${MVN_CMD} -q clean package ${PL_ARG} -DskipTests ${GUI_PROFILE} -Drevision=${REVISION}

# Function to build Docker image for a specific module
build_docker() {
    local module_dir="$1"
    local module_name="${module_dir#midas-}"
    local image_name="gregordietrich/${module_dir}"
    local dockerfile="src/main/docker/Dockerfile.jvm-alpine"
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

for m in $MODULES_TO_DOCKER; do
    build_docker "$m"
done

echo "Build completed."
