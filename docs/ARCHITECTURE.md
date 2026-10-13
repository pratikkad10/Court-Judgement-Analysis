# Architecture

## System diagram

```mermaid
flowchart LR
  B[Browser] --> N[Nginx / React]
  N --> G[API Gateway :8080]
  G --> A[Auth service :8081]
  G --> D[Document service :8082]
  G --> Q[Query service :8083]
  D --> V[(Shared uploads volume)]
  D --> I[AI ingestion :8091]
  I --> V
  I --> P[(PostgreSQL 16 + pgvector)]
  Q --> D
  Q --> R[AI RAG :8092]
  R --> P
  I --> L[LLM router]
  R --> L
  L --> Groq & Gemini & NIM[NVIDIA NIM] & HF[Hugging Face]
```

Only Nginx is host-published. The gateway is reachable from Nginx on the edge network and every service communicates over the internal Docker network. PostgreSQL contains separate `auth`, `documents`, `query`, and `ai` schemas.

## Responsibilities

| Component | Responsibility |
|---|---|
| Gateway | JWT validation, trusted identity headers, CORS, correlation ID and user/AI route limits. |
| Auth | Registration, BCrypt credentials, JWT issuance, refresh token hashing/rotation, account admin. |
| Documents | PDF validation, hashing, file storage, ownership, status and metadata callbacks. |
| Query | Owns sessions/messages/analyses/audit records; validates document ownership before RAG calls. |
| Ingestion | Extracts, cleans, OCR-falls-back, labels, chunks and locally embeds PDF text. |
| RAG | Applies guardrails, hybrid retrieval, answer/analysis chains and citation verification. |

## Request flows

### Upload and ingestion

```mermaid
sequenceDiagram
  participant U as User
  participant G as Gateway
  participant D as Document service
  participant I as Ingestion
  participant DB as PostgreSQL
  U->>G: POST /api/documents (Bearer, PDF)
  G->>D: X-User-Id, X-Internal-Key, correlation ID
  D->>D: magic bytes, filename, size/pages, SHA-256 duplicate checks
  D->>I: POST /v1/ingest (shared path)
  D-->>U: PROCESSING document
  I->>I: extract, clean, OCR optional, metadata, sections, chunks
  I->>DB: ai.chunks + vector/HNSW + tsvector/GIN
  I->>D: status READY/FAILED + metadata
```

### Query

```mermaid
sequenceDiagram
  participant U as User
  participant G as Gateway
  participant Q as Query service
  participant D as Document service
  participant R as RAG service
  U->>G: question + selected document IDs
  G->>Q: verified identity and correlation ID
  Q->>D: ownership check for every selected ID
  D-->>Q: owned map
  Q->>R: question, history, allowed IDs only
  R->>R: input guardrails, hybrid retrieval, output verifier
  R-->>Q: answer, citations, provider metadata
  Q->>Q: persist messages/audit
  Q-->>U: source-grounded response envelope
```

### Compare

```mermaid
sequenceDiagram
  participant Q as Query service
  participant R as RAG service
  Q->>R: issue + allowed document IDs
  loop each document
    R->>R: retrieve top chunks for one ID only
  end
  R->>R: compare equal-coverage reasoning summaries
  R-->>Q: structured similarities, differences, conclusion, citations
```

## ER diagram

```mermaid
erDiagram
  USERS ||--o{ REFRESH_TOKENS : owns
  USERS ||--o{ CHAT_SESSIONS : owns
  CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains
  USERS ||--o{ ANALYSES : saves
  USERS ||--o{ AUDIT_LOGS : generates
  DOCUMENTS { uuid id PK uuid owner_id string file_hash string status }
  CHUNKS { string id PK uuid document_id vector embedding tsvector tsv }
```

The diagram intentionally shows logical ownership rather than foreign keys across schemas: services do not use cross-schema SQL.

## LLM fallback

```mermaid
flowchart TD
  A[Request] --> B{Configured & circuit closed?}
  B -- no --> N[Next provider]
  B -- yes --> C[Attempt]
  C -- success and valid JSON --> O[Return provider metadata]
  C -- 400/401 --> N
  C -- timeout/429/5xx/network/empty/invalid JSON --> D[Short backoff and one retry]
  D --> E{retry succeeded?}
  E -- yes --> O
  E -- no --> F[Increment failures]
  F --> G{3 failures?}
  G -- yes --> H[Open circuit / Retry-After cooldown]
  G -- no --> N
  H --> N
  N --> B
  N --> Z[All exhausted: 503 friendly error]
```

## Guardrail pipeline

```mermaid
flowchart LR
  Q[Question] --> I[Length, empty, injection, advice/fake-document, scope]
  I -->|refusal| X[Polite response]
  I --> R[Ownership + similarity threshold]
  R --> C[Delimited context + grounded prompt]
  C --> O[LLM output]
  O --> V[Marker + semantic citation verifier]
  V --> S[Strip unsupported citations, flag partial, disclaimer]
```

## Security design

- Gateway validates a 15-minute HMAC JWT and strips/replaces trusted user headers.
- Service mesh calls require the same `X-Internal-Key`; only health checks bypass it.
- Refresh tokens are random, BCrypt-hashed, expired after seven days and revoked/rotated per use.
- PostgreSQL schema ownership aligns with service ownership; no service queries a different schema.
- Files use generated UUID paths, not user-provided paths. A filename is sanitized and PDFs are checked before storage.
- User document and chat isolation is enforced by document-service and query-service before RAG receives IDs.
- Logs use correlation IDs and deliberately exclude credentials, tokens, keys and full document text.

## Design decisions

- **Microservices:** permits independent Java business domains and Python AI scale/runtime choices while retaining explicit ownership boundaries.
- **One local embedding model:** pgvector dimensions and similarity scores remain consistent; mixed embedding indexes silently corrupt retrieval quality.
- **Hybrid search:** dense vectors handle paraphrases while PostgreSQL FTS preserves exact legal terms, party names and statutory wording. RRF avoids fragile score-scale mixing.
- **Section-bounded chunks:** context remains legally interpretable and citations do not mix a holding with unrelated facts.
- **Provider-neutral HTTP adapters:** API fallback is transparent and does not couple core chains to a vendor SDK.
