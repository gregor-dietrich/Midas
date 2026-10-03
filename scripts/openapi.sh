#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Regenerating OpenAPI contract snapshot..."

cd "$PROJECT_ROOT"

# Quarkus augmentation writes the contract to target/openapi/ during `package`; see
# quarkus.smallrye-openapi.store-schema-directory in midas-api/src/main/resources/application.properties.
${MVN_CMD} -q package -DskipTests -pl midas-api -Drevision="${REVISION}"

cp midas-api/target/openapi/openapi.yaml midas-api/openapi.yaml

echo "Wrote midas-api/openapi.yaml"
