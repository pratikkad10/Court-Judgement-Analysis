"""Pydantic request and response contracts shared by internal AI endpoints."""

from __future__ import annotations
from pydantic import BaseModel, Field, field_validator
from uuid import UUID
from datetime import date
from typing import Literal


class Citation(BaseModel):
    """Maps a stable source marker to an indexed judgment span."""

    source_id: str
    document_id: UUID
    document_title: str
    page_start: int = Field(ge=1)
    page_end: int = Field(ge=1)
    section: str
    snippet: str


class QueryRequest(BaseModel):
    """Accepts a question plus pre-authorized document IDs from query-service."""

    document_ids: list[UUID] = Field(min_length=1)
    question: str = Field(min_length=1, max_length=8000)
    history: list[dict[str, str]] = Field(default_factory=list, max_length=6)


class RagAnswer(BaseModel):
    """Returns a grounded answer and verifiable citations to the orchestrator."""

    answer: str
    citations: list[Citation] = Field(default_factory=list)
    provider_used: str | None = None
    latency_ms: int | None = None
    partially_verified: bool = False


class AnalysisRequest(BaseModel):
    """Accepts selected, pre-authorized judgments for a saved analysis."""

    document_ids: list[UUID] = Field(min_length=1)
    issue: str = Field(default="", max_length=4000)


class ExtractedMetadata(BaseModel):
    """Constrains metadata emitted by the document extraction model."""

    court: str | None = None
    case_number: str | None = None
    judgment_date: date | None = None
    parties: list[str] = Field(default_factory=list)


class ArgumentItem(BaseModel):
    """Pairs one extracted argument with a source marker."""

    text: str
    source_id: str


class ArgumentsResult(BaseModel):
    """Constrains argument extraction so clients can render it safely."""

    petitioner_arguments: list[ArgumentItem] = Field(default_factory=list)
    respondent_arguments: list[ArgumentItem] = Field(default_factory=list)
    court_findings: list[ArgumentItem] = Field(default_factory=list)


class ComparisonResult(BaseModel):
    """Constrains comparison output to a compact, citation-aware shape."""

    per_document: dict[str, str]
    similarities: list[str]
    differences: list[str]
    conclusion: str
