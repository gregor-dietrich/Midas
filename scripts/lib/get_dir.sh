#!/bin/bash

# Directory of the script that sourced this lib
DIR="$(dirname "$0")"

# Project root is always two levels up from lib/get_dir.sh
LIB_DIR="$(dirname "${BASH_SOURCE[0]}")"
export PROJECT_ROOT="$(realpath "$LIB_DIR/../..")"
