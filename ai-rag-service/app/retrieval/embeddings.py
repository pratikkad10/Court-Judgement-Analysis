"""Uses the configured local sentence-transformers model for query vectors."""

from functools import lru_cache
from typing import Any
from judgment_common.core.config import get_settings


@lru_cache
def get_model() -> Any:
    """Loads exactly one configured embedding model for this process."""
    model_name = get_settings().embedding_model
    if not model_name:
        raise RuntimeError("EMBEDDING_MODEL is required")
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(model_name)


def encode_query(query: str) -> list[float]:
    """Returns one normalized query vector matching the ingestion model."""
    return (
        get_model()
        .encode(query, normalize_embeddings=True, show_progress_bar=False)
        .tolist()
    )
