"""Reads hybrid candidates only from pre-authorized AI schema chunks."""

from __future__ import annotations
import asyncpg
from judgment_common.core.config import get_settings


async def _connection() -> asyncpg.Connection:
    """Opens a request-scoped database connection."""
    url = get_settings().database_url.replace("postgresql+asyncpg", "postgresql")
    if not url:
        raise RuntimeError("DATABASE_URL is required")
    return await asyncpg.connect(url)


async def configured_model_matches(document_ids: list[str]) -> bool:
    """Rejects retrieval when stored vectors use a different embedding model."""
    conn = await _connection()
    try:
        models = await conn.fetch(
            "SELECT DISTINCT embedding_model FROM ai.chunks WHERE document_id = ANY($1::text[])",
            document_ids,
        )
        return not models or all(
            row["embedding_model"] == get_settings().embedding_model for row in models
        )
    finally:
        await conn.close()


async def vector_candidates(document_ids: list[str], vector: list[float]) -> list[dict]:
    """Returns the closest 30 chunks using pgvector cosine distance."""
    conn = await _connection()
    try:
        rows = await conn.fetch(
            "SELECT id,document_id,document_title,text,section_type,page_start,page_end,1-(embedding <=> $2::vector) AS similarity FROM ai.chunks WHERE document_id = ANY($1::text[]) ORDER BY embedding <=> $2::vector LIMIT 30",
            document_ids,
            str(vector),
        )
        return [dict(row) for row in rows]
    finally:
        await conn.close()


async def text_candidates(document_ids: list[str], query: str) -> list[dict]:
    """Returns the strongest 30 PostgreSQL full-text matches."""
    conn = await _connection()
    try:
        rows = await conn.fetch(
            "SELECT id,document_id,document_title,text,section_type,page_start,page_end,ts_rank_cd(tsv, websearch_to_tsquery('english',$2)) AS similarity FROM ai.chunks WHERE document_id = ANY($1::text[]) AND tsv @@ websearch_to_tsquery('english',$2) ORDER BY similarity DESC LIMIT 30",
            document_ids,
            query,
        )
        return [dict(row) for row in rows]
    finally:
        await conn.close()


async def document_preview(document_ids: list[str]) -> str:
    """Returns a bounded vocabulary sample for the lightweight document-scope guardrail."""
    conn = await _connection()
    try:
        rows = await conn.fetch(
            "SELECT text FROM ai.chunks WHERE document_id = ANY($1::text[]) ORDER BY chunk_index LIMIT 12",
            document_ids,
        )
        return " ".join(row["text"][:1200] for row in rows)
    finally:
        await conn.close()
