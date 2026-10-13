.PHONY: up down test logs reset
up:
	docker compose up --build
down:
	docker compose down
logs:
	docker compose logs -f
reset:
	docker compose down -v --remove-orphans
	rm -rf data/uploads/*
test:
	./run_all_tests.sh
