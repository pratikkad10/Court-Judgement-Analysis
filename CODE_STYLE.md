# Code Style

- Prefer small functions with one responsibility and meaningful names.
- Java uses controller → service → repository/entity/dto/exception layers. Controllers validate/map only. Use DTOs and standard response envelopes.
- Python uses typed Pydantic/FastAPI models and dependency injection; keep API, core, retrieval/ingestion, database, guardrail and router responsibilities separate.
- React keeps API types/hooks/components close to feature folders. Avoid copying request and error logic across pages.
- Put one short, plain-English why-comment above each class and non-obvious function. Do not add banners, decorative comments or code emojis.
- Never hardcode secrets, provider model IDs or operational URLs. Document environment configuration.
- Do not log passwords, raw tokens, API keys or full source texts. Include the correlation ID in operational logs.
- Do not add custom annotations/decorators. Framework-required annotations/decorators are allowed.

## Formatting

Run formatters before committing:

```bash
# Java (Google Java Format 1.24)
google-java-format --replace $(find api-gateway auth-service document-service query-service -name '*.java')

# Python (Black)
black python-common/judgment_common ai-ingestion-service/app ai-rag-service/app

# Frontend (Prettier)
cd frontend && npm run format && npm run format:check
```
