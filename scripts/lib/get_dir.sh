#!/bin/bash

# Directory of the script that is currently running
DIR="$(dirname "$0")"

# Library directory (where get_dir.sh lives)
LIB_DIR="$(dirname "${BASH_SOURCE[0]}")"

# Project root is always two levels up from lib/get_dir.sh
PROJECT_ROOT="$(realpath "$LIB_DIR/../..")"
