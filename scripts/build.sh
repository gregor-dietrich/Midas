#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

# Function to build a specific module
build_module() {
    local module_dir="$1"
    local module_name="$2"
    
    echo "=== Building $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"
    
    # Run check first
    "$PROJECT_ROOT/scripts/check.sh" "${module_dir#midas-}"
    
    # Package the application
    if [[ "$module_dir" == "midas-gui" ]]; then
        ${MVN_CMD} package -Pproduction -DskipTests -Drevision=${REVISION}
    else
        ${MVN_CMD} package -DskipTests -Drevision=${REVISION}
    fi
    
    # Docker buildx if available
    if command -v docker &> /dev/null; then
        echo "Building Docker image for $module_name..."
        if [[ "$module_dir" == "midas-api" ]]; then
            docker buildx build . -f src/main/docker/Dockerfile.jvm-ubuntu -t gregordietrich/midas-api:${REVISION} --load
        else
            docker buildx build . -f src/main/docker/Dockerfile.jvm-ubuntu -t gregordietrich/midas-gui:${REVISION} --load
        fi
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
