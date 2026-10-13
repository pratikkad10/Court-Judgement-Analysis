"""Builds grounded prompts and executes query and analysis chains."""

from __future__ import annotations
import re
from collections import defaultdict
from pathlib import Path
from time import perf_counter
from typing import Any
from pydantic import BaseModel
from judgment_common.guardrails import check_input
from app.guardrails import SemanticCitationVerifier
from judgment_common.llm_router import AllProvidersFailedError, build_router
from judgment_common.schemas import (
    AnalysisRequest,
    ArgumentsResult,
    Citation,
    QueryRequest,
    RagAnswer,
    ComparisonResult,
)
from judgment_common.core.config import get_settings
from app.retrieval.service import retrieve
from app.retrieval.repository import document_preview

PROMPTS = Path(__file__).resolve().parents[2] / "prompts"
NOT_FOUND = "The information was not found in the selected judgments."


class SummaryResult(BaseModel):
    """Defines the stable sectioned summary returned to clients."""

    facts: str = ""
    issues: str = ""
    arguments: str = ""
    reasoning: str = ""
    holding: str = ""
    final_order: str = ""


def _prompt(name: str) -> str:
    """Loads versioned prompt text so behavioral changes remain reviewable."""
    return (PROMPTS / name).read_text(encoding="utf-8")


def _standalone_question(question: str, history: list[dict[str, str]]) -> str:
    """Makes common follow-ups comprehensible without leaking full chat history to retrieval."""
    if not history or not re.search(r"\b(it|that|this|they|those)\b", question.lower()):
        return question
    prior_questions = [
        item["content"] for item in history if item.get("role") == "user"
    ]
    return (
        f"{prior_questions[-1]} Follow-up: {question}" if prior_questions else question
    )


def _sources(rows: list[dict[str, Any]]) -> tuple[str, dict[str, str], list[Citation]]:
    """Numbers retrieval rows and produces model context plus client citation objects."""
    blocks, texts, citations = [], {}, []
    for index, row in enumerate(rows, 1):
        source_id = f"S{index}"
        texts[source_id] = row["text"]
        blocks.append(
            f"<SOURCE id=\"{source_id}\" document=\"{row['document_title']}\" pages=\"{row['page_start']}-{row['page_end']}\" section=\"{row['section_type']}\">\n{row['text']}\n</SOURCE>"
        )
        citations.append(
            Citation(
                source_id=source_id,
                document_id=row["document_id"],
                document_title=row["document_title"],
                page_start=row["page_start"],
                page_end=row["page_end"],
                section=row["section_type"],
                snippet=row["text"][:360],
            )
        )
    return "\n\n".join(blocks), texts, citations


def _used_citations(text: str, citations: list[Citation]) -> list[Citation]:
    """Sends only source spans whose markers survive answer verification."""
    used = set(re.findall(r"\[S(\d+)\]", text))
    return [
        citation
        for citation in citations
        if citation.source_id.removeprefix("S") in used
    ]


async def answer(request: QueryRequest) -> RagAnswer:
    """Runs guardrails, retrieval, LLM generation and sentence-level verification."""
    guard = check_input(
        request.question,
        await document_preview([str(item) for item in request.document_ids]),
    )
    if not guard.allowed:
        return RagAnswer(
            answer=f"{guard.refusal}\n\nThis is a research aid and not legal advice."
        )
    standalone = _standalone_question(guard.query_for_processing, request.history[-6:])
    rows = await retrieve(standalone, [str(item) for item in request.document_ids])
    if not rows:
        return RagAnswer(answer=NOT_FOUND)
    context, source_texts, citations = _sources(rows)
    router = build_router(get_settings())
    started = perf_counter()
    result = await router.generate(
        [
            {"role": "system", "content": _prompt("query_v1.txt")},
            {
                "role": "user",
                "content": f"Question: {standalone}\n\nContext:\n{context}",
            },
        ],
        max_tokens=900,
        temperature=0.1,
    )
    verified_answer, partial = SemanticCitationVerifier().verify(
        result.text, source_texts
    )
    return RagAnswer(
        answer=verified_answer,
        citations=_used_citations(verified_answer, citations),
        provider_used=result.provider,
        latency_ms=int((perf_counter() - started) * 1000),
        partially_verified=partial,
    )


async def summarize(request: AnalysisRequest) -> dict[str, Any]:
    """Maps retrieved sections independently and merges them into a fixed legal structure."""
    rows = await retrieve(
        request.issue
        or "Summarize the judgment facts issues arguments reasoning holding and final order",
        [str(item) for item in request.document_ids],
    )
    if not rows:
        return {"result": {"message": NOT_FOUND}, "citations": []}
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        grouped[row["section_type"]].append(row)
    router = build_router(get_settings())
    section_summaries: list[str] = []
    all_citations: list[Citation] = []
    for section, section_rows in grouped.items():
        context, _, citations = _sources(section_rows)
        generated = await router.generate(
            [
                {"role": "system", "content": _prompt("summary_map_v1.txt")},
                {"role": "user", "content": f"Section: {section}\n{context}"},
            ],
            max_tokens=500,
            temperature=0.0,
        )
        section_summaries.append(f"{section}: {generated.text}")
        all_citations.extend(citations)
    merged, provider = await router.structured(
        SummaryResult,
        [
            {"role": "system", "content": _prompt("summary_merge_v1.txt")},
            {"role": "user", "content": "\n\n".join(section_summaries)},
        ],
        max_tokens=1000,
    )
    return {
        "result": merged.model_dump(),
        "citations": [item.model_dump(mode="json") for item in all_citations],
        "provider_used": provider.provider,
    }


async def arguments(request: AnalysisRequest) -> dict[str, Any]:
    """Extracts parties' positions and findings as a schema-validated JSON object."""
    rows = await retrieve(
        request.issue
        or "What were the petitioner arguments respondent arguments and court findings?",
        [str(item) for item in request.document_ids],
    )
    if not rows:
        return {"result": {"message": NOT_FOUND}, "citations": []}
    context, _, citations = _sources(rows)
    parsed, provider = await build_router(get_settings()).structured(
        ArgumentsResult,
        [
            {"role": "system", "content": _prompt("arguments_v1.txt")},
            {"role": "user", "content": context},
        ],
        max_tokens=1000,
    )
    return {
        "result": parsed.model_dump(),
        "citations": [item.model_dump(mode="json") for item in citations],
        "provider_used": provider.provider,
    }


async def compare(request: AnalysisRequest) -> dict[str, Any]:
    """Retrieves each judgment separately so one long document cannot dominate comparison."""
    if len(request.document_ids) < 2:
        raise ValueError("Select at least two judgments to compare.")
    issue = request.issue or "the key legal issue and the court's reasoning"
    summaries, citations = [], []
    for document_id in request.document_ids:
        rows = await retrieve(issue, [str(document_id)])
        if rows:
            context, _, local_citations = _sources(rows)
            summaries.append(f"Document {document_id}:\n{context}")
            citations.extend(local_citations)
    if not summaries:
        return {"result": {"message": NOT_FOUND}, "citations": []}
    parsed, provider = await build_router(get_settings()).structured(
        ComparisonResult,
        [
            {"role": "system", "content": _prompt("compare_v1.txt")},
            {"role": "user", "content": f"Issue: {issue}\n\n" + "\n\n".join(summaries)},
        ],
        max_tokens=1200,
    )
    return {
        "result": parsed.model_dump(),
        "citations": [item.model_dump(mode="json") for item in citations],
        "provider_used": provider.provider,
    }
