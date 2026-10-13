# Contributing

1. Create a focused branch and keep changes inside the service that owns the behavior.
2. Copy `.env.example`; never commit `.env`, secrets, documents, tokens or provider responses.
3. Add a migration for schema changes (Flyway for Java schemas, Alembic for `ai`). Do not query another service schema.
4. Add or update tests with every behavior change. Run `./run_all_tests.sh` before a pull request.
5. Keep prompts versioned as plain text files. Keep provider model IDs and all secrets in environment configuration.
6. Preserve source citations and guardrails. Do not weaken ownership filtering, inject user text into system prompts, or log full document content.
7. Describe API, database, security or evaluation changes in the relevant `docs/` file.

Use conventional, descriptive commits such as `feat(document): reject duplicate SHA-256 uploads`.
