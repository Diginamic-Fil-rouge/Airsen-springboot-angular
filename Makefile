# Example: make dev, make prod, make test

.PHONY: help dev dev-backend dev-frontend prod test down logs clean rebuild status health

# Default target - show help
help:
	@echo "  Airsen Docker Management Commands";
	@echo "";
	@echo "Development Commands:";
	@echo "  make dev              - Start full development stack (backend + frontend + db + redis)";
	@echo "  make dev-backend      - Start backend only with infrastructure";
	@echo "  make dev-frontend     - Start frontend only";
	@echo "";
	@echo "Production Commands:";
	@echo "  make prod             - Start production stack locally";
	@echo "  make prod-build       - Build production images";
	@echo "";
	@echo "Testing Commands:";
	@echo "  make test             - Run all tests (backend + frontend)";
	@echo "  make test-backend     - Run backend tests only";
	@echo "  make test-frontend    - Run frontend tests only";
	@echo "";
	@echo "Utility Commands:";
	@echo "  make down             - Stop all services";
	@echo "  make logs             - View logs from all services";
	@echo "  make logs-backend     - View backend logs only";
	@echo "  make logs-frontend    - View frontend logs only";
	@echo "  make clean            - Stop and remove all containers, volumes, and networks";
	@echo "  make rebuild          - Clean rebuild of development environment";
	@echo "  make status           - Show running containers status";
	@echo "  make health           - Check health of all services";
	@echo "";

# DEVELOPMENT COMMANDS

# Start full development stack
dev:
	@echo "Starting full development stack...";
	docker-compose --env-file .env.local --profile dev up -d
	@echo "Development stack started!";
	@echo "   Backend:  http://localhost:8080";
	@echo "   Frontend: http://localhost:4200";
	@echo "   Swagger:  http://localhost:8080/swagger-ui.html";
	@echo "   Redis UI: http://localhost:8082";

# Start backend only (for frontend development on host)
dev-backend:
	@echo "Starting backend services...";
	docker-compose --env-file .env.local up -d mariadb redis api-dev
	@echo "Backend started!";
	@echo "   API: http://localhost:8080";

# Start frontend only (requires backend running)
dev-frontend:
	@echo "Starting frontend service...";
	docker-compose --env-file .env.local up -d web-dev
	@echo "Frontend started!";
	@echo "   App: http://localhost:4200";

# PRODUCTION COMMANDS

# Build production images
prod-build:
	@echo "Building production images...";
	docker-compose --env-file .env.prod --profile prod build --no-cache
	@echo "Production images built successfully!";

# Start production stack locally
prod:
	@echo "Starting production stack locally...";
	docker-compose --env-file .env.prod --profile prod up -d
	@echo "Production stack started!";
	@echo "   Application: http://localhost";

# TESTING COMMANDS

# Run all tests
test:
	@echo "Running all tests...";
	@echo "Building test images...";
	docker-compose --env-file .env.test build --target test
	@echo "Running backend tests...";
	docker-compose --env-file .env.test run --rm api-dev
	@echo "Running frontend tests...";
	docker-compose --env-file .env.test run --rm web-dev
	@echo "All tests completed!";

# Run backend tests only
test-backend:
	@echo "Running backend tests...";
	docker-compose --env-file .env.test build --target test airsen-backend
	docker-compose --env-file .env.test run --rm api-dev mvn test
	@echo "Backend tests completed!";

# Run frontend tests only
test-frontend:
	@echo "Running frontend tests...";
	docker-compose --env-file .env.test build --target test airsen-frontend
	docker-compose --env-file .env.test run --rm web-dev npm run test:ci
	@echo "Frontend tests completed!";

# UTILITY COMMANDS

# Stop all services
down:
	@echo "Stopping all services...";
	docker-compose down
	@echo "All services stopped!";

# View logs from all services
logs:
	@echo "Showing logs from all services (Ctrl+C to exit)...";
	docker-compose logs -f

# View backend logs only
logs-backend:
	@echo "Showing backend logs (Ctrl+C to exit)...";
	docker-compose logs -f api-dev api

# View frontend logs only
logs-frontend:
	@echo "Showing frontend logs (Ctrl+C to exit)...";
	docker-compose logs -f web-dev web

# Clean up everything (containers, volumes, networks)
clean:
	@echo "Cleaning up Docker resources...";
	docker-compose down -v
	docker system prune -f
	@echo "Cleanup completed!";

# Rebuild development environment from scratch
rebuild:
	@echo "Rebuilding development environment...";
	docker-compose down -v
	docker-compose --env-file .env.local --profile dev build --no-cache
	docker-compose --env-file .env.local --profile dev up -d
	@echo "Rebuild completed!";

# Show status of running containers
status:
	@echo "Docker containers status:";
	@docker-compose ps

# Check health of all services
health:
	@echo "Checking services health...";
	@echo "";
	@echo "MariaDB:";
	@docker-compose exec mariadb mariadb-admin ping -h 127.0.0.1 -u root -p$$DB_ROOT_PASSWORD --silent && echo "Healthy" || echo "Unhealthy";
	@echo "";
	@echo "Redis:";
	@docker-compose exec redis redis-cli ping && echo "Healthy" || echo "Unhealthy";
	@echo "";
	@echo "Backend API:";
	@curl -sf http://localhost:8080/actuator/health > /dev/null && echo "Healthy" || echo "Unhealthy";
	@echo "";




