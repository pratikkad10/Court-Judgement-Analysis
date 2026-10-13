"""Exposes private, grounded query and legal-research analysis endpoints."""

from __future__ import annotations
from fastapi import Depends, FastAPI, HTTPException
from judgment_common.core.logging import (
    configure_logging,
    correlation_id as log_correlation_id,
)
from fastapi.responses import JSONResponse
from judgment_common.llm_router import AllProvidersFailedError
from judgment_common.schemas import AnalysisRequest, QueryRequest
from app.core.security import require_internal_key
from app.chains.research import answer, arguments, compare, summarize

configure_logging()
app = FastAPI(title="Judgment Analyzer RAG", docs_url=None, redoc_url=None)


@app.middleware("http")
async def correlation_middleware(request, call_next):
    """Binds the propagated correlation ID to all logs produced by this request."""
    token = log_correlation_id.set(
        request.headers.get("X-Correlation-Id", "missing-correlation-id")
    )
    try:
        return await call_next(request)
    finally:
        log_correlation_id.reset(token)


@app.exception_handler(AllProvidersFailedError)
async def provider_failure(_, __):
    """Avoids leaking provider implementation errors when all fallbacks fail."""
    return JSONResponse(
        status_code=503,
        content={
            "detail": "Language-model providers are temporarily unavailable. Please try again later."
        },
    )


@app.exception_handler(ValueError)
async def input_failure(_, error: ValueError):
    """Returns safe validation errors for request and embedding-model mismatches."""
    return JSONResponse(status_code=400, content={"detail": str(error)})


@app.get("/health")
async def health() -> dict[str, str]:
    """Provides a Docker liveness response without exposing research routes."""
    return {"status": "ok"}


@app.post("/v1/query", dependencies=[Depends(require_internal_key)])
async def query(request: QueryRequest):
    """Answers one question from only pre-authorized selected judgments."""
    return (await answer(request)).model_dump(mode="json")


@app.post("/v1/summarize", dependencies=[Depends(require_internal_key)])
async def summary(request: AnalysisRequest):
    """Creates a sectioned map-reduce judgment summary."""
    return await summarize(request)


@app.post("/v1/arguments", dependencies=[Depends(require_internal_key)])
async def argument_extraction(request: AnalysisRequest):
    """Returns source-aware party arguments and court findings."""
    return await arguments(request)


@app.post("/v1/compare", dependencies=[Depends(require_internal_key)])
async def comparison(request: AnalysisRequest):
    """Compares separately retrieved reasoning from multiple judgments."""
    return await compare(request)
