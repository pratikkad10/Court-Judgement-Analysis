"""Routes calls through configured providers with bounded retries and circuits."""

from __future__ import annotations
import asyncio
import json
import logging
import time
from dataclasses import dataclass
from typing import Any, TypeVar
from pydantic import BaseModel, ValidationError
from .base import BaseProvider, ProviderError, ProviderResult

log = logging.getLogger(__name__)
T = TypeVar("T", bound=BaseModel)


class AllProvidersFailedError(RuntimeError):
    """Signals that no configured provider could safely serve a request."""


@dataclass
class Circuit:
    """Tracks consecutive failures and a monotonic cooldown deadline."""

    failures: int = 0
    blocked_until: float = 0.0


class LlmRouter:
    """Attempts configured providers in order without vendor SDK dependencies."""

    def __init__(
        self,
        providers: list[BaseProvider],
        cooldown_seconds: int = 60,
        clock=time.monotonic,
    ):
        self.providers = providers
        self.cooldown_seconds = cooldown_seconds
        self.clock = clock
        self.circuits = {provider.name: Circuit() for provider in providers}

    async def generate(
        self,
        messages: list[dict[str, str]],
        max_tokens: int = 800,
        temperature: float = 0.1,
        json_mode: bool = False,
    ) -> ProviderResult:
        errors: list[str] = []
        for provider in self.providers:
            circuit = self.circuits[provider.name]
            if not provider.configured:
                continue
            if circuit.blocked_until > self.clock():
                errors.append(f"{provider.name}: circuit open")
                continue
            for attempt in (1, 2):
                started = self.clock()
                try:
                    result = await provider.generate(
                        messages, max_tokens, temperature, json_mode
                    )
                    if json_mode:
                        json.loads(result.text)
                    circuit.failures = 0
                    circuit.blocked_until = 0
                    log.info(
                        "llm_call provider=%s model=%s latency_ms=%s attempt=%s",
                        provider.name,
                        provider.model,
                        int((self.clock() - started) * 1000),
                        attempt,
                    )
                    return result
                except (ProviderError, json.JSONDecodeError) as error:
                    status = (
                        error.status_code if isinstance(error, ProviderError) else None
                    )
                    retry_after = (
                        error.retry_after if isinstance(error, ProviderError) else None
                    )
                    log.warning(
                        "llm_call_failed provider=%s model=%s attempt=%s status=%s",
                        provider.name,
                        provider.model,
                        attempt,
                        status,
                    )
                    if status in (400, 401):
                        errors.append(f"{provider.name}: configuration error")
                        break
                    if attempt == 1:
                        await asyncio.sleep(0.1)
                        continue
                    circuit.failures += 1
                    if circuit.failures >= 3:
                        circuit.blocked_until = self.clock() + (
                            retry_after or self.cooldown_seconds
                        )
                    errors.append(f"{provider.name}: unavailable")
        raise AllProvidersFailedError(
            "No configured language-model provider is currently available. Please try again later."
        )

    async def structured(
        self, schema: type[T], messages: list[dict[str, str]], max_tokens: int = 800
    ) -> tuple[T, ProviderResult]:
        """Validates model JSON and makes one focused repair request when needed."""
        result = await self.generate(
            messages, max_tokens=max_tokens, temperature=0.0, json_mode=True
        )
        try:
            return schema.model_validate_json(result.text), result
        except ValidationError:
            repair = messages + [
                {
                    "role": "user",
                    "content": "Return only valid JSON that conforms exactly to the requested schema. Do not add commentary.",
                }
            ]
            repaired = await self.generate(
                repair, max_tokens=max_tokens, temperature=0.0, json_mode=True
            )
            return schema.model_validate_json(repaired.text), repaired
