.PHONY: dev backend frontend test e2e build down clean

dev:
	docker compose up --build

backend:
	docker compose up --build postgres backend

frontend:
	docker compose up --build frontend

test:
	docker compose --profile tools run --rm --build backend-test
	docker compose --profile tools run --rm --build frontend-test
	docker compose --profile tools run --rm --build airflow-test

e2e:
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-e2e.ps1

build:
	docker compose build

down:
	docker compose down --remove-orphans

clean:
	docker compose down --remove-orphans
	rm -rf backend/target frontend/dist frontend/coverage
