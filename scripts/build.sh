#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

PLATFORMS="linux/amd64,linux/arm64"
MODULE=${1:-"both"}

set -e

echo "Starting build..."

# Run environment check first
"$DIR/check.sh" "$MODULE"

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi

# Function to build a specific module
build_module() {
    local module_dir="$1"
    local module_name="$2"
    local image_name="gregordietrich/${module_dir}"
    local dockerfile="src/main/docker/Dockerfile.jvm-ubuntu"
    local tag="${image_name}:${REVISION}"

    echo "=== Building $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"

    # Package the application
    local mvn_profiles=""
    if [[ "$module_dir" == "midas-gui" ]]; then
        mvn_profiles="-Pproduction"
    fi
    ${MVN_CMD} -q clean package -DskipTests ${mvn_profiles} -Drevision=${REVISION}

    # Docker buildx if available
    if docker buildx inspect default >/dev/null 2>&1; then
        echo "Using buildx 'default' builder for $module_name image..."

        # QEMU logic
        _native_arch="$(uname -m)"
        _binfmt_install_targets=""
        for _platform in ${PLATFORMS//,/ }; do
            _arch="${_platform#linux/}"
            case "$_arch" in
                amd64) _native_equiv="x86_64";  _qemu_entry="qemu-x86_64"  ;;
                arm64) _native_equiv="aarch64"; _qemu_entry="qemu-aarch64" ;;
                *)     continue ;;
            esac
            [[ "$_native_arch" == "$_native_equiv" ]] && continue
            grep -q "enabled" "/proc/sys/fs/binfmt_misc/${_qemu_entry}" 2>/dev/null && continue
            _binfmt_install_targets="${_binfmt_install_targets} ${_arch}"
        done
        _binfmt_install_targets="${_binfmt_install_targets# }"

        if [[ -n "$_binfmt_install_targets" ]]; then
            echo "Registering QEMU binfmt handlers for: ${_binfmt_install_targets}"
            docker run --privileged --rm \
                tonistiigi/binfmt:qemu-v10.2.1@sha256:d3b963f787999e6c0219a48dba02978769286ff61a5f4d26245cb6a6e5567ea3 \
                --install "${_binfmt_install_targets}" >/dev/null 2>&1 || echo "Warning: failed to install QEMU binfmt handlers."
        fi

        if docker buildx build --builder default --platform "$PLATFORMS" -t "$tag" -f "$dockerfile" .; then
            echo "buildx multi-platform build finished for $module_name."
        else
            echo "buildx multi-platform build failed; attempting single-platform local build with --load."
            if ! docker buildx build --load --platform linux/amd64 -t "$tag" -f "$dockerfile" .; then
                echo "--load build failed; falling back to plain 'docker build'."
                docker build -t "$tag" -f "$dockerfile" .
            fi
        fi
    else
        echo "buildx 'default' builder not available; performing plain docker build for $module_name."
        docker build -t "$tag" -f "$dockerfile" .
    fi

    echo
    cd - > /dev/null
}

case $MODULE in
    "api")
        build_module "midas-api" "Midas API"
        ;;
    "gui")
        build_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        build_module "midas-api" "Midas API"
        build_module "midas-gui" "Midas GUI"
        ;;
esac

echo "Build completed."
