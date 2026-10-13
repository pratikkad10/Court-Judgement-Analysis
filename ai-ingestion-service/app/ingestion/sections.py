"""Finds common legal judgment sections using transparent heading rules first."""

from __future__ import annotations
import re
from dataclasses import dataclass

SECTION_PATTERNS = {
    "Facts": r"^(facts?|background|brief facts?)\b",
    "Issues": r"^(issues?|questions? (for |of )?determination)\b",
    "Arguments": r"^(submissions?|arguments?|contentions?)\b",
    "Reasoning": r"^(reasoning|analysis|discussion|consideration)\b",
    "Holding": r"^(holding|decision|findings?)\b",
    "Order": r"^(order|operative part|decree)\b",
}


@dataclass(frozen=True)
class SectionPage:
    """Associates a page's text with the most recently recognised section."""

    section: str
    page_number: int
    text: str


def detect_sections(pages: list[str]) -> list[SectionPage]:
    """Assigns every page to a section so chunks can never cross boundaries."""
    current = "General"
    result: list[SectionPage] = []
    for page_number, page in enumerate(pages, start=1):
        first_lines = "\n".join(page.splitlines()[:6]).lower()
        for section, pattern in SECTION_PATTERNS.items():
            if re.search(pattern, first_lines, re.MULTILINE | re.IGNORECASE):
                current = section
                break
        result.append(SectionPage(current, page_number, page))
    return result
