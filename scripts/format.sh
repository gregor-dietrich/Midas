#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Formatting Java sources.."

# Function to lint a specific module
format_module() {
    local module_dir="$1"
    local module_name="$2"

    echo "=== Formatting $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"

    ${MVN_CMD} -q spotless:apply -Drevision="${REVISION}"
    echo

    cd - > /dev/null
}

case $MODULE in
    "api")
        format_module "midas-api" "Midas API"
        ;;
    "gui")
        format_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        format_module "midas-api" "Midas API"
        format_module "midas-gui" "Midas GUI"
        ;;
esac

echo "Java source formatting completed."
