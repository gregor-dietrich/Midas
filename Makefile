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

.PHONY: build dev-api dev-gui openapi password release

build: check ## mvn package, docker buildx (ONLY=<module> limits it to one)
	@scripts/build.sh

dev-api: ## start Midas API in Quarkus dev mode (port 8080)
	@scripts/dev.sh api

dev-gui: ## start Midas GUI in Quarkus dev mode (port 8081)
	@scripts/dev.sh gui

openapi: ## regenerate the committed OpenAPI contract snapshot
	@scripts/openapi.sh

password: ## generate a salt+hash for a password (for API init.sql)
	@scripts/password.sh api

release: ## pull from origin/main, make build, make tag, and push Docker images
	@scripts/release.sh
