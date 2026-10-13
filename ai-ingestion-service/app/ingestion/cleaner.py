"""Normalises extracted pages while preserving their legal source position."""

from __future__ import annotations
import re
from collections import Counter


def clean_pages(pages: list[str]) -> list[str]:
    """Drops repeated edge headers/footers and repairs split-word hyphenation."""
    edge_lines = [
        line.strip()
        for page in pages
        for line in (page.splitlines()[:2] + page.splitlines()[-2:])
        if line.strip()
    ]
    repeated = {line for line, count in Counter(edge_lines).items() if count >= 2}
    cleaned: list[str] = []
    for page in pages:
        lines = [
            line.strip()
            for line in page.splitlines()
            if line.strip() and line.strip() not in repeated
        ]
        text = "\n".join(lines)
        text = re.sub(r"(\w)-\s*\n\s*(\w)", r"\1\2", text)
        text = re.sub(r"[ \t]+", " ", text)
        text = re.sub(r"\n{3,}", "\n\n", text).strip()
        cleaned.append(text)
    return cleaned
