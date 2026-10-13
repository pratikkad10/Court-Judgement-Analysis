"""Provides asynchronous, private endpoints for judgment ingestion."""

from __future__ import annotations
import logging
from judgment_common.core.logging import (
    configure_logging,
    correlation_id as log_correlation_id,
)
from datetime import date
import httpx
from fastapi import BackgroundTasks, Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field
from judgment_common.core.config import get_settings
from .core.security import correlation_id, require_internal_key
from .ingestion.cleaner import clean_pages
from .ingestion.chunker import chunk_sections
from .ingestion.embeddings import embed
from .ingestion.extractor import extract_pages
from .ingestion.metadata import detect_sections_with_fallback, extract_metadata
from .ingestion.repository import delete_document_chunks, replace_document_chunks

configure_logging()
logger = logging.getLogger(__name__)
app = FastAPI(title="Judgment Analyzer Ingestion", docs_url=None, redoc_url=None)


class IngestRequest(BaseModel):
    """Contains the shared-volume path selected by document-service."""

    document_id: str
    storage_path: str
    title: str = Field(min_length=1, max_length=500)


async def notify(
    document_id: str,
    status: str,
    metadata: dict[str, object],
    correlation: str,
    failure: str | None = None,
) -> None:
    """Returns a final ingest status to the owning document service."""
    settings = get_settings()
    payload = {
        "status": status,
        "court": metadata.get("court"),
        "caseNumber": metadata.get("case_number"),
        "judgmentDate": metadata.get("judgment_date"),
        "failureReason": failure,
    }
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(
            f"{settings.document_service_url}/api/documents/internal/{document_id}/status",
            headers={
                "X-Internal-Key": settings.internal_service_key,
                "X-Correlation-Id": correlation,
            },
            json=payload,
        )
        response.raise_for_status()


async def ingest(request: IngestRequest, correlation: str) -> None:
    """Runs extraction, structured enrichment, chunking and local embedding off-request."""
    metadata: dict[str, object] = {}
    try:
        pages = clean_pages(extract_pages(request.storage_path))
        full_text = "\n".join(pages)
        extracted = await extract_metadata(full_text)
        metadata = extracted.model_dump(mode="json")
        sections = await detect_sections_with_fallback(pages)
        chunks = chunk_sections(sections)
        if not chunks:
            raise ValueError("No extractable judgment text was found.")
        vectors = embed([chunk.text for chunk in chunks])
        await replace_document_chunks(
            request.document_id, request.title, chunks, vectors
        )
        await notify(request.document_id, "READY", metadata, correlation)
        logger.info(
            "ingestion_complete document_id=%s chunks=%s",
            request.document_id,
            len(chunks),
            extra={"correlation_id": correlation},
        )
    except Exception as error:
        logger.exception(
            "ingestion_failed document_id=%s",
            request.document_id,
            extra={"correlation_id": correlation},
        )
        try:
            await notify(
                request.document_id,
                "FAILED",
                metadata,
                correlation,
                "The judgment could not be indexed.",
            )
        except Exception:
            logger.exception(
                "ingestion_callback_failed document_id=%s",
                request.document_id,
                extra={"correlation_id": correlation},
            )


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


@app.get("/health")
async def health() -> dict[str, str]:
    """Returns an unauthenticated liveness response for Docker health checks."""
    return {"status": "ok"}


@app.post("/v1/ingest", dependencies=[Depends(require_internal_key)], status_code=202)
async def start_ingest(
    request: IngestRequest,
    background: BackgroundTasks,
    correlation: str = Depends(correlation_id),
) -> dict[str, str]:
    """Schedules ingestion so document upload remains responsive."""
    background.add_task(ingest, request, correlation)
    return {"status": "accepted", "document_id": request.document_id}


@app.delete("/v1/ingest/{document_id}", dependencies=[Depends(require_internal_key)])
async def remove_ingest(document_id: str) -> dict[str, str]:
    """Deletes vector and text index rows for a removed document."""
    await delete_document_chunks(document_id)
    return {"status": "deleted"}
