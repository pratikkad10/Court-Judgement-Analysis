"""Loads exactly one local embedding model and exposes normalized vectors."""

from __future__ import annotations
from functools import lru_cache
import numpy as np
from typing import Any
from judgment_common.core.config import get_settings


@lru_cache
def model() -> Any:
    """Loads the configured local model lazily to keep health checks inexpensive."""
    name = get_settings().embedding_model
    if not name:
        raise RuntimeError("EMBEDDING_MODEL is required")
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(name)


def embed(texts: list[str]) -> list[list[float]]:
    """Returns normalized vectors from the one configured model."""
    vectors = model().encode(texts, normalize_embeddings=True, show_progress_bar=False)
    return np.asarray(vectors).tolist()
