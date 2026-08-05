COMPOSE := docker compose -f redis-database.yml

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

.PHONY: format init up down logs reset dev test
