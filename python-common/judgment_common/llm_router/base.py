"""Defines the minimal provider contract used by the fallback router."""

from __future__ import annotations
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class ProviderResult:
    """Keeps generated text and provider-neutral diagnostic metadata together."""

    text: str
    provider: str
    model: str
    latency_ms: int
    raw: dict[str, Any]


class ProviderError(Exception):
    """Describes a provider failure without leaking keys or request content."""

    def __init__(
        self,
        message: str,
        status_code: int | None = None,
        retry_after: int | None = None,
    ):
        super().__init__(message)
        self.status_code = status_code
        self.retry_after = retry_after


class BaseProvider(ABC):
    """Makes every external LLM look alike to routing code."""

    name: str
    model: str

    @property
    @abstractmethod
    def configured(self) -> bool:
        """Returns whether this adapter can make authenticated requests."""

    @abstractmethod
    async def generate(
        self,
        messages: list[dict[str, str]],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
    ) -> ProviderResult:
        """Generates one complete response or raises ProviderError."""
