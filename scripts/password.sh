#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

MODULE=${1:-"api"}

set -e

case $MODULE in
    "api")
        echo "=== Generating password hash for Midas API ==="
        cd "$PROJECT_ROOT/midas-api"
        
        # Usage: ./password.sh [password]
        # If a password is provided as the first argument, use it non-interactively.
        # Otherwise prompt the user (hidden input) and ask for confirmation.
        
        if [ "$#" -ge 2 ] && [ -n "$2" ]; then
            PASSWORD="$2"
        else
            # Prompt for password (hidden)
            echo -n "Enter password to generate salt+hash: "
            read -s PASS1
            echo
            echo -n "Confirm password: "
            read -s PASS2
            echo
        
            if [ "$PASS1" != "$PASS2" ]; then
                echo "Passwords do not match. Aborting." >&2
                cd - > /dev/null
                exit 1
            fi
        
            PASSWORD="$PASS1"
        fi
        
        # Pass the password as the second token after "generate" (the Java CLI expects args[1])
        ${MVN_CMD} -q -Dexec.mainClass="de.vptr.midas.util.PasswordUtility" -Dexec.args="generate ${PASSWORD}" exec:java

        cd - > /dev/null
        ;;
    *)
        echo "Usage: $0 api [password]"
        echo "  api - Generate password hash for Midas API"
        echo "  password - Optional password (will prompt if not provided)"
        exit 1
        ;;
esac
