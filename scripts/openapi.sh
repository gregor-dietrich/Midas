#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"

REVISION=${REVISION:-1.0.0-SNAPSHOT}

set -e

echo "Regenerating OpenAPI contract snapshot..."

# Quarkus augmentation writes the contract to target/openapi/ during `package`; see
# quarkus.smallrye-openapi.store-schema-directory in midas-api/src/main/resources/application.properties.
${MVN_CMD} -q package -DskipTests -pl midas-api -Drevision="${REVISION}"

cp midas-api/target/openapi/openapi.yaml midas-api/openapi.yaml

echo "Wrote midas-api/openapi.yaml"
