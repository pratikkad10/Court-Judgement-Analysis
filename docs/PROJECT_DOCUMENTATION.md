# Project Documentation

## Problem statement

Legal researchers often need to search long court judgments, locate the exact reasoning and final order, and compare outcomes without losing the supporting page. Judgment Analyzer indexes user-supplied judgments and provides citation-grounded research. It does not decide cases or substitute for a lawyer.

## Objectives and scope

**Objectives:** secure document isolation; asynchronous PDF indexing; source-grounded questions; sectioned summaries; argument extraction; equal-coverage comparison; resilient low-cost LLM fallback; and traceable citations.

**In scope:** existing court judgment PDFs, research history, user/admin dashboards, document metadata, retrieval and evaluations.

**Out of scope:** case management, matter intake, judge/hearing management, scheduling, legal representation, filing, drafting fake documents, and personal legal advice.

## Requirements

### Functional

1. Users register, authenticate and refresh sessions securely.
2. Users upload, list, download and delete only their own PDFs.
3. The system validates, indexes and reports upload state.
4. Users ask cited questions and save chats.
5. Users generate summary, arguments and comparison analyses.
6. Administrators manage account enablement and inspect aggregate/audit information.

### Non-functional

- Java services return `{success,data,error}` envelopes.
- Internal calls carry a correlation ID and service key.
- Data is schema-isolated in one PostgreSQL instance.
- Answer citations contain document, pages, section and snippet.
- Provider failures are retried/fallen back without exposing stack traces.
- Tests cover core guardrails, providers, UI behavior and an optional deterministic end-to-end path.

## Module description

| Module | Key implementation |
|---|---|
| `api-gateway` | Spring Cloud Gateway routes, token verification, trusted headers, CORS and in-memory rate windows. |
| `auth-service` | User/refresh-token Flyway migration, BCrypt, seeded admin, role checks. |
| `document-service` | PDF magic/page/size/hash checks, shared-volume storage, ingestion/status integration. |
| `query-service` | Ownership orchestration, chats, saved analysis JSON and audit rows. |
| `python-common` | Provider adapters/router, Pydantic contracts and reusable guardrails. |
| `ai-ingestion-service` | PyMuPDF page extraction, cleaning, optional OCR, labels/chunking and local vectors. |
| `ai-rag-service` | pgvector + tsvector/RRF retrieval, chains, semantic citation verifier. |
| `frontend` | Typed React data layer, role guards, upload polling, chat/PDF viewer and analysis UI. |

## API reference

All browser-facing endpoints are under `/api` and use `Authorization: Bearer <accessToken>` unless labelled public. Java responses use `{"success":true,"data":...,"error":null}`. Common errors include `401 AUTH_REQUIRED`, `403 FORBIDDEN`, `404 ..._NOT_FOUND`, `409 DUPLICATE_DOCUMENT`, `429 RATE_LIMITED`, and `503 ..._UNAVAILABLE`.

### Auth

| Method/path | Auth | Request | Success data | Errors |
|---|---|---|---|---|
| POST `/auth/register` | Public | `{email,password}` | access/refresh tokens, user | 400, 409 |
| POST `/auth/login` | Public | `{email,password}` | access/refresh tokens, user | 401 |
| POST `/auth/refresh` | Public | `{refreshToken}` | rotated token pair | 401 |
| POST `/auth/logout` | Token | `{refreshToken}` | `null` | 401 |
| GET `/auth/me` | Token | — | user | 401, 404 |
| GET `/auth/admin/users` | Admin | — | user array | 403 |
| PATCH `/auth/admin/users/{id}/enabled?value=true` | Admin | — | user | 403, 404 |

### Documents

| Method/path | Auth | Request | Success data | Errors |
|---|---|---|---|---|
| POST `/documents` | Token | multipart `file` PDF | document | 400, 409 |
| GET `/documents` | Token | — | owned documents | 401 |
| GET `/documents/{id}` | Token | — | owned document | 404 |
| GET `/documents/{id}/download` | Token | — | PDF stream | 404 |
| DELETE `/documents/{id}` | Token | — | `null` | 404 |
| GET `/documents/internal/ownership?ids=...` | Internal | trusted user header | `{owned:{id:boolean}}` | 401 |
| POST `/documents/internal/{id}/status` | Internal | status/metadata callback | `null` | 401,404 |

A document object is `{id,title,filename,pageCount,status,court,caseNumber,judgmentDate,createdAt}`. Status is `UPLOADED`, `PROCESSING`, `READY`, or `FAILED`.

### Research and history

| Method/path | Auth | Request | Success data | Errors |
|---|---|---|---|---|
| POST `/chat/sessions` | Token | `{title}` | session | 400 |
| GET `/chat/sessions` | Token | — | sessions | 401 |
| GET `/chat/sessions/{id}/messages` | Token | — | messages | 404 |
| POST `/chat/sessions/{id}/messages` | Token | `{documentIds,question}` | cited assistant message | 400,403,503 |
| POST `/analyses/summary` | Token | `{documentIds,issue?}` | analysis | 400,403,503 |
| POST `/analyses/arguments` | Token | `{documentIds,issue?}` | analysis | 400,403,503 |
| POST `/analyses/compare` | Token | `{documentIds: [at least two],issue?}` | analysis | 400,403,503 |
| GET `/history` | Token | — | saved analyses | 401 |
| GET `/dashboard` | Token | — | `{sessions,messages,analyses}` | 401 |
| GET `/dashboard/admin` | Admin | — | aggregate counts | 403 |
| GET `/dashboard/admin/audit` | Admin | — | latest 100 audit records | 403 |

`ChatMessage.citations` is an array of `{source_id,document_id,document_title,page_start,page_end,section,snippet}`. The UI opens the cited document at `page_start`.

### Private AI endpoints

These endpoints are not host-published and require `X-Internal-Key`; their document IDs have already been authorized by query-service.

| Service / method | Path | Request | Response |
|---|---|---|---|
| Ingestion POST | `/v1/ingest` | `{document_id,storage_path,title}` | `202 accepted` |
| Ingestion DELETE | `/v1/ingest/{id}` | — | deleted vectors |
| Ingestion GET | `/health` | — | liveness |
| RAG POST | `/v1/query` | `{document_ids,question,history}` | answer/citations/provider/verification flag |
| RAG POST | `/v1/summarize` | `{document_ids,issue}` | sectioned result/citations |
| RAG POST | `/v1/arguments` | `{document_ids,issue}` | structured arguments/citations |
| RAG POST | `/v1/compare` | `{document_ids,issue}` | structured comparison/citations |
| RAG GET | `/health` | — | liveness |

## Database design

- **`auth.users`** and **`auth.refresh_tokens`**: account state and revocable sessions.
- **`documents.documents`**: owner ID, storage metadata/hash, page count, state and extracted metadata.
- **`query.chat_sessions`**, **`chat_messages`**, **`analyses`**, **`audit_logs`**: user research records; citation and result fields are JSONB.
- **`ai.chunks`**: document ID/title, section/page/token metadata, normalized `vector(384)` embedding, embedding model, and `tsvector`. HNSW accelerates cosine search and GIN accelerates FTS.

Flyway owns Java service migrations. Alembic in ingestion owns the AI schema migration. No service queries another service's schema.

## AI pipeline

1. PyMuPDF extracts page text; optional Tesseract OCR is tried only on sparse pages.
2. Cleaner removes repeated edges and joins hyphenated line breaks while retaining page identity.
3. Metadata is constrained with Pydantic structured output; explicit regex extraction is a non-inferential fallback.
4. Heading regexes identify legal sections; a structured LLM page-label fallback is used only if headings are absent.
5. Chunks are 400–600 tokens with 15% overlap and never cross a section boundary.
6. `BAAI/bge-small-en-v1.5`, configured solely by environment, creates local normalized vectors.
7. RAG gets vector top-30 and FTS top-30, merges with RRF, boosts matching sections, optionally reranks, and returns six spans.
8. The model must emit `[S#]` markers. The semantic verifier removes unsupported or unknown-marked sentences and flags partial verification.

## Guardrails

Input checks occur before logs/retrieval: empty/length, injection patterns, personal advice/fake-document requests, legal/document scope, and personal-data masking in logs. Retrieval filters strictly by supplied allowed IDs, rejects mismatched embedding models, and enforces a score threshold. Prompts delimit context and prohibit treating it as instructions. Output validation verifies markers and sentence-to-chunk embedding similarity, removes absent case citations, flags partial verification, and appends the research-aid disclaimer. Upload guards validate extension, magic bytes, byte size, page count, sanitized filename and per-user hash uniqueness.

## Testing strategy and results

- **Java:** service/JWT/controller/filter tests and an H2 repository contract test.
- **Python:** cleaner, section detector, chunker, RRF, input/output guardrails, semantic verifier and mocked `respx` provider fallback/circuit tests.
- **Frontend:** login, protected-route redirect, library status/upload target and clickable citation rendering.
- **Integration:** opt-in HTTP test with the permitted test-only fake LLM.
- **Evaluation:** 20 expected-page cases with Recall@5, MRR and citation accuracy calculation.

Run results are recorded in the final implementation summary after executing `run_all_tests.sh` in an environment with Java 17/Maven. The repository does not claim production-scale accuracy: evaluate your own jurisdiction and corpus before relying on output.

## Limitations and future work

- OCR is optional and needs system Tesseract for scanned judgments.
- The gateway limiter is in-memory; production should use Redis-backed distributed buckets.
- Local embedding/model downloads need disk/RAM and first start latency.
- Citation support is an automated research control, not a legal validation guarantee.
- Planned: jurisdiction/language-specific section rules, streaming responses, S3-compatible encrypted storage, monitoring/tracing, stronger evaluation datasets, and background-job queue durability.
