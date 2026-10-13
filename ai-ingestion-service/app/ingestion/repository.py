"""Writes and deletes only data owned by the ai database schema."""

from __future__ import annotations
import json
from uuid import uuid4
import asyncpg
from judgment_common.core.config import get_settings
from .chunker import ChunkDraft


async def connection() -> asyncpg.Connection:
    """Opens a short-lived connection appropriate for a background ingestion job."""
    url = get_settings().database_url.replace("postgresql+asyncpg", "postgresql")
    if not url:
        raise RuntimeError("DATABASE_URL is required")
    return await asyncpg.connect(url)


async def replace_document_chunks(
    document_id: str, title: str, chunks: list[ChunkDraft], vectors: list[list[float]]
) -> None:
    """Atomically replaces an indexed document so retries cannot duplicate chunks."""
    conn = await connection()
    try:
        async with conn.transaction():
            await conn.execute(
                "DELETE FROM ai.chunks WHERE document_id=$1", document_id
            )
            rows = [
                (
                    str(uuid4()),
                    document_id,
                    title,
                    index,
                    chunk.text,
                    chunk.section,
                    chunk.page_start,
                    chunk.page_end,
                    chunk.token_count,
                    json.dumps(vector),
                    get_settings().embedding_model,
                    chunk.text,
                )
                for index, (chunk, vector) in enumerate(zip(chunks, vectors))
            ]
            await conn.executemany(
                "INSERT INTO ai.chunks (id,document_id,document_title,chunk_index,text,section_type,page_start,page_end,token_count,embedding,embedding_model,tsv) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10::vector,$11,to_tsvector('english',$12))",
                rows,
            )
    finally:
        await conn.close()


async def delete_document_chunks(document_id: str) -> None:
    """Removes all vectors for a user-deleted document."""
    conn = await connection()
    try:
        await conn.execute("DELETE FROM ai.chunks WHERE document_id=$1", document_id)
    finally:
        await conn.close()
