"""Applies hybrid retrieval, section boosting and optional local reranking."""

from __future__ import annotations
from typing import Any
from judgment_common.core.config import get_settings
from .embeddings import encode_query
from .repository import configured_model_matches, text_candidates, vector_candidates
from .rrf import reciprocal_rank_fusion


def _boost(query: str, row: dict[str, Any]) -> float:
    """Favors final orders or reasoning only when the question requests them."""
    lower = query.lower()
    section = row["section_type"].lower()
    if any(
        term in lower for term in ("final order", "order", "disposed", "relief")
    ) and section in {"holding", "order"}:
        return 0.02
    if (
        any(term in lower for term in ("reason", "ground", "why", "analysis"))
        and section == "reasoning"
    ):
        return 0.02
    return 0


async def retrieve(query: str, document_ids: list[str]) -> list[dict[str, Any]]:
    """Returns at most six high-confidence source spans from allowed documents only."""
    if not await configured_model_matches(document_ids):
        raise ValueError(
            "Indexed documents use a different embedding model and must be re-ingested."
        )
    vector, text = await vector_candidates(
        document_ids, encode_query(query)
    ), await text_candidates(document_ids, query)
    merged = reciprocal_rank_fusion([vector, text])
    enriched = [
        {**row, "rrf_score": row["rrf_score"] + _boost(query, row)}
        for row in merged
        if float(row.get("similarity", 0)) >= get_settings().rag_min_similarity
    ]
    enriched.sort(key=lambda row: row["rrf_score"], reverse=True)
    if get_settings().rerank_enabled and enriched:
        from sentence_transformers import CrossEncoder

        rerank_model = get_settings().rerank_model
        if not rerank_model:
            raise ValueError("RERANK_MODEL is required when RERANK_ENABLED=true")
        reranker = CrossEncoder(rerank_model)
        scores = reranker.predict([(query, row["text"]) for row in enriched[:30]])
        for row, score in zip(enriched, scores):
            row["rerank_score"] = float(score)
        enriched.sort(key=lambda row: row["rerank_score"], reverse=True)
    return enriched[:6]
