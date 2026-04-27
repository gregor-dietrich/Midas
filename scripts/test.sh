#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"both"}
REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

# Function to test a specific module
test_module() {
    local module_dir="$1"
    local module_name="$2"
    
    echo "=== Testing $module_name ==="
    cd "$PROJECT_ROOT/$module_dir"
    
    ${MVN_CMD} test -Drevision=${REVISION}
    echo
}

case $MODULE in
    "api")
        test_module "midas-api" "Midas API"
        ;;
    "gui")
        test_module "midas-gui" "Midas GUI"
        ;;
    "both"|*)
        test_module "midas-api" "Midas API"
        test_module "midas-gui" "Midas GUI"
        ;;
esac

cd "$PROJECT_ROOT" > /dev/null