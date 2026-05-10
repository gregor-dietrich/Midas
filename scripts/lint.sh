#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Starting lint checks..."

# Function to lint a specific module
lint_module() {
    local module_dir="$1"
    local module_name="$2"
    
    echo "=== Linting $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"
    
    ${MVN_CMD} -q compile test-compile spotless:apply checkstyle:check spotbugs:check pmd:check pmd:cpd-check -Drevision="${REVISION}"
    echo
    
    cd - > /dev/null
}

case $MODULE in
    "api")
        lint_module "midas-api" "Midas API"
        ;;
    "gui")
        lint_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        lint_module "midas-api" "Midas API"
        lint_module "midas-gui" "Midas GUI"
        ;;
esac

echo "Lint checks completed."
