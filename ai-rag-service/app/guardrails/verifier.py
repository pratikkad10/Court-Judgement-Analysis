"""Applies local embedding support checks to every displayed cited sentence."""

from __future__ import annotations
import re
import numpy as np
from judgment_common.guardrails.output import CASE_CITATION, DISCLAIMER, MARKER
from app.retrieval.embeddings import get_model


class SemanticCitationVerifier:
    """Removes claims whose cited chunks are not semantically similar enough."""

    def verify(
        self, answer: str, sources: dict[str, str], threshold: float = 0.42
    ) -> tuple[str, bool]:
        sentences = re.split(r"(?<=[.!?])\s+", answer.strip())
        kept: list[str] = []
        partial = False
        context = " ".join(sources.values()).lower()
        for sentence in sentences:
            markers = MARKER.findall(sentence)
            if not markers or any(f"S{marker}" not in sources for marker in markers):
                partial = True
                continue
            reference = " ".join(sources[f"S{marker}"] for marker in markers)
            vectors = get_model().encode(
                [sentence, reference],
                normalize_embeddings=True,
                show_progress_bar=False,
            )
            if float(np.dot(vectors[0], vectors[1])) < threshold:
                partial = True
                continue
            kept.append(
                CASE_CITATION.sub(
                    lambda match: (
                        match.group(0) if match.group(0).lower() in context else ""
                    ),
                    sentence,
                ).strip()
            )
        result = " ".join(item for item in kept if item)
        if not result:
            result, partial = (
                "The information was not found in the selected judgments.",
                True,
            )
        if not result.endswith(DISCLAIMER):
            result = f"{result}\n\n{DISCLAIMER}"
        return result, partial
