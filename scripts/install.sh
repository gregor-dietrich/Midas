#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

# Function to install a specific module
install_module() {
    local module_dir="$1"
    local module_name="$2"
    
    echo "=== Installing $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"
    
    # Run check first
    "$PROJECT_ROOT/scripts/check.sh" "${module_dir#midas-}"
    
    ${MVN_CMD} clean install -DskipTests -Drevision=${REVISION}
    echo
    
    cd - > /dev/null
}

case $MODULE in
    "api")
        install_module "midas-api" "Midas API"
        ;;
    "gui")
        install_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        install_module "midas-api" "Midas API"
        install_module "midas-gui" "Midas GUI"
        ;;
esac
