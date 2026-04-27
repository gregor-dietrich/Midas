#!/bin/bash

set -e

echo "Killing Quarkus and Maven processes..."

# Kill Quarkus dev processes
pkill -f -9 "quarkus:dev" || true

# Kill Maven processes that might be hanging
pkill -f -9 "org.codehaus.plexus.classworlds.launcher.Launcher" || true

# Stop and remove all Docker containers
if command -v docker &> /dev/null; then
    echo "Stopping and removing Docker containers..."
    docker compose -f "$(dirname "$0")/../docker-compose.yml" down || true
fi

echo "Process cleanup complete."
