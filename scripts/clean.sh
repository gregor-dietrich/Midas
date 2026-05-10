#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}

set -e

echo "Starting cleanup..."

# Function to clean a specific module
clean_module() {
    local module_dir="$1"
    local module_name="$2"

    echo "=== Cleaning $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"

    ${MVN_CMD} -q clean

    # Remove additional build artifacts
    rm -rf logs/ node_modules/ || true
    echo

    cd - > /dev/null
}

case $MODULE in
    "api")
        clean_module "midas-api" "Midas API"
        ;;
    "gui")
        clean_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        clean_module "midas-api" "Midas API"
        clean_module "midas-gui" "Midas GUI"
        ;;
esac

echo "Cleanup completed."
