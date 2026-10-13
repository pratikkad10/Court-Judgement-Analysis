"""Combines vector and full-text rankings using reciprocal rank fusion."""

from __future__ import annotations
from collections import defaultdict
from typing import Any


def reciprocal_rank_fusion(
    rankings: list[list[dict[str, Any]]], k: int = 60
) -> list[dict[str, Any]]:
    """Merges rank lists by ID while retaining the best result metadata."""
    scores: dict[str, float] = defaultdict(float)
    rows: dict[str, dict[str, Any]] = {}
    for ranking in rankings:
        for rank, row in enumerate(ranking, start=1):
            key = str(row["id"])
            scores[key] += 1 / (k + rank)
            rows[key] = {**rows.get(key, {}), **row}
    return [
        {**rows[key], "rrf_score": score}
        for key, score in sorted(scores.items(), key=lambda item: item[1], reverse=True)
    ]
