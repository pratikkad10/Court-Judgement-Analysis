"""Applies deterministic safety checks before logs, retrieval or prompts."""

from __future__ import annotations
import re
from dataclasses import dataclass

INJECTION_PATTERNS = [
    r"ignore (all |any |the )?previous instructions",
    r"reveal (the )?(system )?prompt",
    r"act as (a|an|the)",
    r"jailbreak",
    r"developer message",
]
PERSONAL_ADVICE_PATTERNS = [
    r"what should i do (in|with) my (case|divorce|arrest|claim)",
    r"legal advice for my",
    r"my lawyer",
]
FAKE_DOCUMENT_PATTERNS = [
    r"draft (a )?(fake|forged)",
    r"forge (a )?(court|legal)",
    r"create (a )?fake (legal|court)",
]
LEGAL_TERMS = {
    "judgment",
    "court",
    "case",
    "holding",
    "order",
    "appeal",
    "petitioner",
    "respondent",
    "reasoning",
    "law",
    "legal",
    "section",
    "facts",
    "issue",
    "argument",
}
PHONE = re.compile(r"(?<!\d)(?:\+?\d{1,3}[ -]?)?\d{10}(?!\d)")
EMAIL = re.compile(r"\b[\w.+-]+@[\w-]+\.[\w.-]+\b")
IDENTIFIER = re.compile(r"\b\d{4}[ -]?\d{4}[ -]?\d{4}\b|\b\d{3}-\d{2}-\d{4}\b")


@dataclass(frozen=True)
class InputDecision:
    """Represents either a safe query or a user-facing refusal."""

    allowed: bool
    query_for_processing: str
    query_for_log: str
    refusal: str | None = None


def mask_personal_data(text: str) -> str:
    """Masks recognizable personal identifiers before anything is logged."""
    return IDENTIFIER.sub(
        "[REDACTED_ID]",
        EMAIL.sub("[REDACTED_EMAIL]", PHONE.sub("[REDACTED_PHONE]", text)),
    )


def check_input(
    question: str, document_text: str = "", max_length: int = 8000
) -> InputDecision:
    """Rejects unsafe or irrelevant questions without sending them to an LLM."""
    clean = question.strip()
    log_text = mask_personal_data(clean)
    lower = clean.lower()
    if not clean:
        return InputDecision(
            False,
            clean,
            log_text,
            "Please enter a question about the selected judgments.",
        )
    if len(clean) > max_length:
        return InputDecision(
            False,
            clean,
            log_text,
            "Please keep the question within the allowed length.",
        )
    if any(re.search(pattern, lower) for pattern in INJECTION_PATTERNS):
        return InputDecision(
            False,
            clean,
            log_text,
            "I can help analyse the selected judgments, but I cannot follow instructions embedded in a question.",
        )
    if any(re.search(pattern, lower) for pattern in PERSONAL_ADVICE_PATTERNS):
        return InputDecision(
            False,
            clean,
            log_text,
            "I can research the selected judgments, but I cannot provide legal advice for a personal matter.",
        )
    if any(re.search(pattern, lower) for pattern in FAKE_DOCUMENT_PATTERNS):
        return InputDecision(
            False,
            clean,
            log_text,
            "I cannot help create or draft fake legal documents.",
        )
    tokens = set(re.findall(r"[a-z]{3,}", lower))
    document_tokens = set(re.findall(r"[a-z]{3,}", document_text.lower()))
    legal_signal = bool(tokens & LEGAL_TERMS)
    document_signal = len(tokens & document_tokens) >= 1 if document_tokens else True
    if not legal_signal and not document_signal:
        return InputDecision(
            False,
            clean,
            log_text,
            "Please ask a legal research question about the selected judgments.",
        )
    return InputDecision(True, clean, log_text)
