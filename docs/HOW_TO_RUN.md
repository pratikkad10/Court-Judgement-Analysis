# How to run Judgment Analyzer

## Prerequisites

- Docker Engine 24+ and Docker Compose v2
- 8 GB RAM minimum; 12 GB is recommended because the first local embedding-model download is substantial
- For local development: Java 17, Maven 3.9+, Python 3.11, Node.js 20+, npm 10+, PostgreSQL 16 with pgvector, and optionally Tesseract OCR
- One or more LLM API keys. Embeddings are local and need no API key.

## Get a provider key

The router needs only **one** configured provider. It skips blank keys and transparently moves to the next provider on eligible failures.

| Provider | Signup/key page | Environment variables |
|---|---|---|
| Groq | <https://console.groq.com> | `GROQ_API_KEY`, `GROQ_MODEL` |
| Google Gemini | <https://aistudio.google.com/apikey> | `GEMINI_API_KEY`, `GEMINI_MODEL` |
| Hugging Face | <https://huggingface.co/settings/tokens> | `HF_API_KEY`, `HF_MODEL` |
| NVIDIA NIM | <https://build.nvidia.com> | `NVIDIA_API_KEY`, `NVIDIA_MODEL` |

Model catalogs and free quotas change. The documented defaults in `.env.example` were checked on 2026-10-12; use the provider catalog to replace an unavailable model ID. Model IDs are environment variables, never source-code constants.

## Docker Compose start

```bash
cp .env.example .env
# Edit .env: set POSTGRES_PASSWORD, JWT_SECRET (32+ characters), INTERNAL_SERVICE_KEY,
# ADMIN_EMAIL, ADMIN_PASSWORD (12+ characters), and at least one provider key/model.
docker compose up --build
open http://localhost
```

Nginx is the browser-facing entry point at port 80. It proxies `/api` to the gateway; the gateway and all backend services have no host port mappings. Register a normal user, sign in, upload a PDF, and wait for `READY` before researching it. The configured admin account is seeded on the auth service's first start only when both `ADMIN_EMAIL` and `ADMIN_PASSWORD` are valid.

## Run each service locally

Start PostgreSQL with pgvector and create schemas using `db/init/01-schemas.sql`. Copy the root `.env` into your shell. Keep `DOCUMENT_STORAGE_DIR` shared between document service and ingestion service.

```bash
# Java services (separate terminals)
cd auth-service && mvn spring-boot:run
cd document-service && mvn spring-boot:run
cd query-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run

# Python services
python3.11 -m venv .venv && source .venv/bin/activate
pip install -e python-common
pip install -r ai-ingestion-service/requirements.txt
(cd ai-ingestion-service && alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8091)
pip install -r ai-rag-service/requirements.txt
(cd ai-rag-service && PYTHONPATH=../python-common uvicorn app.main:app --host 0.0.0.0 --port 8092)

# Browser app; configure a local proxy or use the Docker Nginx proxy for /api.
cd frontend && npm ci && npm run dev
```

For direct local service calls, include `X-Internal-Key: $INTERNAL_SERVICE_KEY`; private user routes must also include valid gateway-generated `X-User-Id` and `X-User-Role` headers. In normal use clients never send those headers: the gateway does.

## Tests

```bash
./run_all_tests.sh
# Windows
run_all_tests.bat
```

The script runs Maven tests, all Python unit tests, the frontend suite, and leaves the network integration test skipped unless explicitly requested. The normal toolchain requirement is Java 17 and Maven 3.9+.

### Deterministic integration test

The test-only fake provider is deliberately disabled by default and is never available in normal operation.

```bash
TEST_FAKE_LLM=true LLM_PROVIDER_ORDER=fake docker compose up --build -d
RUN_INTEGRATION=true JUDGMENT_ANALYZER_BASE_URL=http://localhost \
  python3 -m pytest -q tests/integration
```

It uploads `integration/sample_judgment.pdf`, waits for ingestion, asks a question, and asserts a page citation.

### Evaluation

After the sample document is indexed, obtain its UUID from `GET /api/documents` and run:

```bash
cd ai-rag-service
PYTHONPATH=../python-common DATABASE_URL="$DATABASE_URL" EMBEDDING_MODEL="$EMBEDDING_MODEL" \
  python evaluation/evaluate.py --document-id YOUR_DOCUMENT_UUID
```

The script evaluates 20 fixture questions in `evaluation/gold.json` and prints Recall@5, MRR, and citation-page accuracy.

### Postman and Newman

Import `postman/JudgmentAnalyzer.postman_collection.json` and `postman/local.postman_environment.json`. After Compose is running:

```bash
newman run postman/JudgmentAnalyzer.postman_collection.json \
  -e postman/local.postman_environment.json
```

Set the environment `baseUrl` to `http://localhost/api`. The collection saves login and refresh tokens automatically. File-upload requests require selecting `integration/sample_judgment.pdf` in the Postman UI; Newman supports it when run from repository root.

## Common errors

| Symptom | Fix |
|---|---|
| Service exits with `JWT_SECRET` or internal-key message | Copy `.env.example`, set non-empty secrets, and use the same `INTERNAL_SERVICE_KEY` everywhere. |
| Document stays `FAILED` | Check `docker compose logs ai-ingestion-service`; verify the shared `uploads` volume, PDF readability, page limit, and embedding-model download. |
| Answer says providers are unavailable | Set a valid provider key/model or inspect router logs. A 400/401 is a configuration error; 429/timeout/5xx trigger fallback. |
| Embedding model mismatch | Keep `EMBEDDING_MODEL` unchanged after ingest, or delete/re-upload documents so chunks use one vector size/model. |
| Nginx returns 502 during startup | Wait for gateway health, then use `docker compose logs api-gateway`. |
| OCR has no output | OCR is optional. Install Tesseract and `pytesseract` only for scanned PDFs; text PDFs do not need it. |

## Reset the database and files

```bash
make reset
# Equivalent: docker compose down -v --remove-orphans && rm -rf data/uploads/*
```

This permanently removes accounts, refresh tokens, documents, chunks, chats and analyses.
