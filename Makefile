.PHONY: help branch build check clean coverage dev dev-api dev-gui format install kill lint password rebase release tag test untag

MAKEFLAGS += --no-print-directory

help:
	@echo "Midas DevTools - Available commands:"
	@echo "  make branch           - create or reset a git branch from a source (prompts for names and pushes)"
	@echo "  make build            - make check, mvn package, docker buildx (both modules)"
	@echo "  make check            - verify local environment (both modules)"
	@echo "  make clean            - run mvn clean, and remove build artifacts (both modules)"
	@echo "  make coverage         - run tests with JaCoCo and generate combined coverage report"
	@echo "  make dev              - start Quarkus in dev mode (specify: make dev-api or make dev-gui)"
	@echo "  make format           - run spotless to format code (both modules)"
	@echo "  make install          - make check, mvn clean install -DskipTests (both modules)"
	@echo "  make kill             - stop/kill Quarkus and Maven processes and remove Docker containers"
	@echo "  make lint             - run quality gate plugins (both modules)"
	@echo "  make password         - generate a salt+hash for a password (for API init.sql)"
	@echo "  make rebase           - interactive git rebase against a target (defaults to origin/main)"
	@echo "  make release          - pull from origin/main, make build, make tag, and push Docker images"
	@echo "  make tag              - create, sign and push a new git tag (auto-increments latest tag suggestion)"
	@echo "  make test             - run full Maven test suite (both modules)"
	@echo "  make untag            - delete a local and remote git tag (prompts for tag to delete)"
	@echo ""
	@echo "Module-specific targets:"
	@echo "  make dev-api          - start Midas API in dev mode"
	@echo "  make dev-gui          - start Midas GUI in dev mode"

branch:
	@scripts/branch.sh

build:
	@scripts/build.sh

check:
	@scripts/check.sh

clean:
	@scripts/clean.sh

coverage:
	@scripts/coverage.sh

dev:
	@echo "Please specify which module to run in dev mode:"
	@echo "  make dev-api  - Start Midas API in dev mode"
	@echo "  make dev-gui  - Start Midas GUI in dev mode"

dev-api:
	@scripts/dev.sh api

dev-gui:
	@scripts/dev.sh gui

format:
	@scripts/format.sh

install:
	@scripts/install.sh

kill:
	@scripts/kill.sh

lint:
	@scripts/lint.sh

password:
	@scripts/password.sh api

rebase:
	@scripts/rebase.sh

release:
	@scripts/release.sh

tag:
	@scripts/tag.sh

test:
	@scripts/test.sh

untag:
	@scripts/untag.sh
