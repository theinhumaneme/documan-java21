# The datastore compose file lives in the deployment repository, one level up,
# because two of the three topologies belong to the deployment rather than to this
# service. Resolved rather than hardcoded so a standalone clone of this repository
# — where there is no parent — fails with "no compose file" instead of silently
# starting nothing.
DATASTORES := $(firstword $(wildcard ../docker-compose.datastores.yml docker-compose.datastores.yml))
COMPOSE := docker compose -f $(DATASTORES)

.PHONY: format init up down dev

format:
	@mvn spotless:apply
init:
	@pre-commit install

# --- local development stack ------------------------------------------------
up:
	@$(COMPOSE) up -d
	@$(COMPOSE) ps
down:
	@$(COMPOSE) down
logs:
	@$(COMPOSE) logs -f
# Removes the stack and its data volumes; the next `up` starts from an empty database.
reset:
	@$(COMPOSE) down -v
	@rm -rf persist

# Runs against the stack above via application-dev.yml.
dev:
	@mvn spring-boot:run -Pdev

# Requires Docker: every database test runs on real PostgreSQL via Testcontainers.
test:
	@mvn -B verify

# --- generated artefacts ----------------------------------------------------
# Both are produced from the code, never edited by hand. Commit the result.
openapi:
	@./extract-openapi-json.sh
schema:
	@./extract-schema-sql.sh
# What CI runs: fails if either committed artefact is behind the code.
verify-generated:
	@./extract-openapi-json.sh --check

.PHONY: format init up down logs reset dev test openapi schema verify-generated
