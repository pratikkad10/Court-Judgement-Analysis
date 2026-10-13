# Judgment Analyzer

A privacy-conscious legal research assistant for analysing **existing court judgments**. Users can upload a judgment PDF, wait for its indexed status, and ask grounded questions, generate a sectioned summary, extract arguments, or compare selected judgments. Every returned claim is accompanied by a document/page/section citation.

> This application is a research aid, not legal advice. It does not manage cases, hearings, scheduling, or judicial decisions.

## Highlights

- JWT authentication with rotating, hashed refresh tokens and USER/ADMIN role guards.
- PDF-only upload validation, SHA-256 duplicate detection, isolated shared file volume, and asynchronous ingestion.
- Per-page extraction, optional OCR, section-aware chunks, local `BAAI/bge-small-en-v1.5` embeddings, pgvector, and PostgreSQL full-text hybrid retrieval.
- Provider-neutral LLM router with Groq, Gemini, NVIDIA NIM, and Hugging Face HTTP adapters, retries, circuit breakers, and structured-output validation.
- Guardrails for injection, scope, personal legal advice, redaction in logs, retrieval thresholds, and citation verification.
- React research workspace with PDF citation jumps, document polling, analysis pages, history, and admin controls.

## Tech stack

React 18, Vite, TypeScript, Tailwind, React Query, react-pdf; Java 17/Spring Boot 3/Spring Cloud Gateway/JPA/Flyway; Python 3.11/FastAPI/Pydantic v2/PyMuPDF; PostgreSQL 16 + pgvector; Docker Compose and Nginx.

## Quick start (five commands)

```bash
cp .env.example .env
# Edit .env: at minimum set strong JWT_SECRET, INTERNAL_SERVICE_KEY, PostgreSQL password and one LLM key.
docker compose up --build
# Browse http://localhost; the public API is http://localhost:8080 when using the optional host mapping.
./run_all_tests.sh
make logs
```

The gateway is intentionally internal-only in the production Compose topology: Nginx proxies `/api` to it. For direct local API work, use the local-service instructions in [HOW_TO_RUN](docs/HOW_TO_RUN.md).

## Architecture

> Screenshot placeholder — Document Library: drag-and-drop upload, status badges, and document metadata.
>
> Screenshot placeholder — Research Chat: selected judgments, citation chips, and page-jumping PDF panel.

See [Architecture](docs/ARCHITECTURE.md), [running the system](docs/HOW_TO_RUN.md), and [complete project documentation](docs/PROJECT_DOCUMENTATION.md).

## Folder structure

```text
api-gateway/             Spring Cloud Gateway public boundary
{auth,document,query}-service/  independent Spring Boot services
ai-ingestion-service/    FastAPI PDF ingestion pipeline
ai-rag-service/          FastAPI retrieval and answer pipeline
python-common/           shared llm_router, guardrails, db and schemas
frontend/                React application
postman/                 importable collection and environment
docs/                    operational and technical documentation
```

## Useful commands

`make up`, `make down`, `make test`, `make logs`, and `make reset` are available. Use `./run_all_tests.sh` (or `run_all_tests.bat` on Windows) to run all suites.

- [Contributing](CONTRIBUTING.md)
- [Code style](CODE_STYLE.md)
