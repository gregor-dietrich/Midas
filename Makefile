PROJECT      := midas2
JAVA_VERSION := 25
MODULES      := midas-api midas-gui
FRONTEND_DIR := midas-gui
DEVKIT := $(shell ./devkitw path)
ifeq ($(DEVKIT),)
$(error devkitw failed; see its message above)
endif
include .devkit/make/common.mk
include .devkit/make/java-maven.mk

.PHONY: build dev-api dev-gui openapi password regen-lockfile release

build: check ## mvn package, docker buildx (ONLY=<module> limits it to one)
	@scripts/build.sh

dev-api: ## start Midas API in Quarkus dev mode (port 8080)
	@scripts/dev.sh api

dev-gui: ## start Midas GUI in Quarkus dev mode (port 8081)
	@scripts/dev.sh gui

openapi: ## regenerate the committed OpenAPI contract snapshot
	@scripts/openapi.sh

password: ## bcrypt-hash a password, read from a prompt or stdin (demo data, break-glass reset)
	@scripts/password.sh api

regen-lockfile: ## update the frontend lockfile to package.json once npm ci rejects it
	@scripts/regen-lockfile.sh

release: ## pull from origin/main, test, make tag, and buildx-push multi-platform images
	@scripts/release.sh
