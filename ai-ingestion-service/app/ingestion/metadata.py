"""Extracts structured metadata and applies LLM section fallback only when needed."""

from __future__ import annotations
from pathlib import Path
import re
from pydantic import BaseModel, Field
from judgment_common.llm_router import AllProvidersFailedError, build_router
from judgment_common.schemas import ExtractedMetadata
from .sections import SectionPage, detect_sections

PROMPTS = Path(__file__).resolve().parents[2] / "prompts"


class SectionLabels(BaseModel):
    """Constrains model page classifications to the supported section vocabulary."""

    labels: list[str] = Field(default_factory=list)


def heuristic_metadata(text: str) -> ExtractedMetadata:
    """Extracts only explicitly printed labels when an optional LLM is unavailable."""

    def find(pattern: str) -> str | None:
        match = re.search(pattern, text, re.IGNORECASE | re.MULTILINE)
        return match.group(1).strip() if match else None

    return ExtractedMetadata(
        court=find(r"(?:in the|court of)\s+([^\n]{3,120})"),
        case_number=find(
            r"(?:case|appeal|writ)\s*(?:no\.?|number)\s*[:.-]?\s*([^\n]{2,100})"
        ),
        parties=[],
    )


async def extract_metadata(text: str) -> ExtractedMetadata:
    """Uses structured LLM extraction while retaining an explicit non-inferential fallback."""
    try:
        prompt = (PROMPTS / "metadata_v1.txt").read_text()
        router = build_router(
            __import__(
                "judgment_common.core.config", fromlist=["get_settings"]
            ).get_settings()
        )
        parsed, _ = await router.structured(
            ExtractedMetadata,
            [
                {"role": "system", "content": prompt},
                {"role": "user", "content": text[:18000]},
            ],
        )
        return parsed
    except (AllProvidersFailedError, ValueError):
        return heuristic_metadata(text)


async def detect_sections_with_fallback(pages: list[str]) -> list[SectionPage]:
    """Uses headings first and asks the LLM only for documents with no headings."""
    detected = detect_sections(pages)
    if any(page.section != "General" for page in detected):
        return detected
    try:
        prompt = (PROMPTS / "section_labels_v1.txt").read_text()
        sample = "\n\n".join(
            f"PAGE {index}: {page[:1200]}" for index, page in enumerate(pages, 1)
        )
        router = build_router(
            __import__(
                "judgment_common.core.config", fromlist=["get_settings"]
            ).get_settings()
        )
        labels, _ = await router.structured(
            SectionLabels,
            [
                {"role": "system", "content": prompt},
                {"role": "user", "content": sample},
            ],
            max_tokens=600,
        )
        allowed = {
            "Facts",
            "Issues",
            "Arguments",
            "Reasoning",
            "Holding",
            "Order",
            "General",
        }
        return [
            SectionPage(
                (
                    labels.labels[index - 1]
                    if index <= len(labels.labels)
                    and labels.labels[index - 1] in allowed
                    else "General"
                ),
                index,
                page,
            )
            for index, page in enumerate(pages, 1)
        ]
    except (AllProvidersFailedError, ValueError):
        return detected
