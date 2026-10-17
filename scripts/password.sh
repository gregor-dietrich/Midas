#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"

MODULE=${1:-"api"}

set -e

echo "Starting password hash generation..."

case $MODULE in
    "api")
        echo "=== Generating password hash for Midas API ==="

        # Interactive: prompt twice with hidden input. Otherwise read the first line of stdin.
        # IFS= keeps leading and trailing whitespace as part of the password.
        if [ -t 0 ]; then
            echo -n "Enter password to hash: "
            IFS= read -r -s PASSWORD
            echo
            echo -n "Confirm password: "
            IFS= read -r -s CONFIRM
            echo

            if [ "$PASSWORD" != "$CONFIRM" ]; then
                echo "Passwords do not match. Aborting." >&2
                exit 1
            fi
        else
            IFS= read -r PASSWORD || true
        fi
        [ -n "$PASSWORD" ] || { echo "Password must not be empty. Aborting." >&2; exit 1; }

        # The password reaches PasswordUtil on stdin (printf is a builtin), never on a command line.
        printf '%s\n' "$PASSWORD" | "${MVN_CMD}" -q -pl midas-api -Dexec.mainClass="de.vptr.midas.api.util.PasswordUtil" compile exec:java

        ;;
    *)
        echo "Usage: $0 api"
        echo "  api - Generate password hash for Midas API (prompts, or reads the password from stdin)"
        exit 1
        ;;
esac

echo "Password hash generation completed."
