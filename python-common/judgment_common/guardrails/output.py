"""Verifies source markers and removes sentences unsupported by cited chunks."""

from __future__ import annotations
import re
from dataclasses import dataclass

MARKER = re.compile(r"\[S(\d+)\]")
CASE_CITATION = re.compile(r"\b[A-Z][A-Za-z .]+ v\. [A-Z][A-Za-z .]+\b")
DISCLAIMER = "This is a research aid and not legal advice."


@dataclass(frozen=True)
class VerificationResult:
    """Contains a cleaned answer and whether any claim had to be removed."""

    answer: str
    partially_verified: bool


class CitationVerifier:
    """Uses lexical support as a transparent final check before display."""

    def verify(
        self, answer: str, sources: dict[str, str], threshold: float = 0.18
    ) -> VerificationResult:
        sentences = re.split(r"(?<=[.!?])\s+", answer.strip())
        kept: list[str] = []
        partial = False
        context = " ".join(sources.values()).lower()
        for sentence in sentences:
            markers = MARKER.findall(sentence)
            if not markers or any(f"S{marker}" not in sources for marker in markers):
                partial = True
                continue
            source_text = " ".join(sources[f"S{marker}"] for marker in markers)
            if self._similarity(sentence, source_text) < threshold:
                partial = True
                continue
            without_unknown = CASE_CITATION.sub(
                lambda match: (
                    match.group(0) if match.group(0).lower() in context else ""
                ),
                sentence,
            )
            kept.append(without_unknown.strip())
        final = " ".join(item for item in kept if item)
        if not final:
            final = "The information was not found in the selected judgments."
            partial = True
        if not final.endswith(DISCLAIMER):
            final = f"{final}\n\n{DISCLAIMER}"
        return VerificationResult(final, partial)

    def _similarity(self, sentence: str, source: str) -> float:
        words = set(re.findall(r"[a-z]{3,}", sentence.lower())) - {
            "this",
            "that",
            "with",
            "from",
            "were",
            "have",
            "has",
            "the",
            "and",
            "for",
        }
        support = set(re.findall(r"[a-z]{3,}", source.lower()))
        return len(words & support) / max(1, len(words))
