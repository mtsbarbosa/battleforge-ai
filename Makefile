.PHONY: fmt fmt-check lint test all

# Format all Clojure files
fmt:
	find src test -name '*.clj' | xargs lein with-profile dev zprint

# Check formatting without modifying files
fmt-check:
	find src test -name '*.clj' | xargs lein with-profile dev zprint -c

# Run linters (clj-kondo + eastwood)
lint:
	lein lint

# Run tests
test:
	lein test

# Run all checks
all: fmt-check lint test

