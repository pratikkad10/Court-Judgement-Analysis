"""Creates overlapping, section-bounded chunks suitable for hybrid retrieval."""

from __future__ import annotations
from dataclasses import dataclass
from .sections import SectionPage


@dataclass(frozen=True)
class ChunkDraft:
    """Carries a chunk and its stable citation metadata before embedding."""

    text: str
    section: str
    page_start: int
    page_end: int
    token_count: int


def chunk_sections(
    section_pages: list[SectionPage], size: int = 500, overlap: float = 0.15
) -> list[ChunkDraft]:
    """Builds 400-600 word-like token chunks without crossing detected sections."""
    if not 400 <= size <= 600:
        raise ValueError("Chunk size must stay between 400 and 600 tokens.")
    by_section: list[list[SectionPage]] = []
    for page in section_pages:
        if not by_section or by_section[-1][0].section != page.section:
            by_section.append([page])
        else:
            by_section[-1].append(page)
    chunks: list[ChunkDraft] = []
    step = max(1, size - round(size * overlap))
    for group in by_section:
        words: list[tuple[str, int]] = [
            (word, page.page_number) for page in group for word in page.text.split()
        ]
        for start in range(0, len(words), step):
            window = words[start : start + size]
            if not window:
                continue
            chunks.append(
                ChunkDraft(
                    " ".join(word for word, _ in window),
                    group[0].section,
                    window[0][1],
                    window[-1][1],
                    len(window),
                )
            )
            if start + size >= len(words):
                break
    return chunks
