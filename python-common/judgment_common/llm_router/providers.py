"""Plain-http adapters for the supported model providers."""

from __future__ import annotations
import time
from typing import Any
import httpx
from .base import BaseProvider, ProviderError, ProviderResult


class OpenAiCompatibleProvider(BaseProvider):
    """Implements common Chat Completions request and response handling."""

    def __init__(self, name: str, api_key: str, model: str, url: str, timeout: float):
        self.name, self.api_key, self.model, self.url, self.timeout = (
            name,
            api_key,
            model,
            url,
            timeout,
        )

    @property
    def configured(self) -> bool:
        return bool(self.api_key and self.model)

    async def generate(
        self,
        messages: list[dict[str, str]],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
    ) -> ProviderResult:
        payload: dict[str, Any] = {
            "model": self.model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
        }
        if json_mode:
            payload["response_format"] = {"type": "json_object"}
        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    self.url,
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                    },
                    json=payload,
                )
        except (httpx.TimeoutException, httpx.NetworkError) as error:
            raise ProviderError("network or timeout error") from error
        latency = int((time.perf_counter() - start) * 1000)
        if response.status_code >= 400:
            retry_after = response.headers.get("Retry-After")
            raise ProviderError(
                "provider returned an error",
                response.status_code,
                int(retry_after) if retry_after and retry_after.isdigit() else None,
            )
        try:
            body = response.json()
            text = body["choices"][0]["message"]["content"]
        except (ValueError, KeyError, IndexError, TypeError) as error:
            raise ProviderError("provider returned an invalid response") from error
        if not isinstance(text, str) or not text.strip():
            raise ProviderError("provider returned an empty response")
        return ProviderResult(text.strip(), self.name, self.model, latency, body)


class GroqProvider(OpenAiCompatibleProvider):
    """Calls Groq's OpenAI-compatible chat endpoint."""

    def __init__(self, api_key: str, model: str, timeout: float):
        super().__init__(
            "groq",
            api_key,
            model,
            "https://api.groq.com/openai/v1/chat/completions",
            timeout,
        )


class HuggingFaceProvider(OpenAiCompatibleProvider):
    """Calls Hugging Face Inference Providers through its OpenAI-compatible router."""

    def __init__(self, api_key: str, model: str, timeout: float):
        super().__init__(
            "huggingface",
            api_key,
            model,
            "https://router.huggingface.co/v1/chat/completions",
            timeout,
        )


class NvidiaProvider(OpenAiCompatibleProvider):
    """Calls NVIDIA's OpenAI-compatible NIM hosted endpoint."""

    def __init__(self, api_key: str, model: str, timeout: float):
        super().__init__(
            "nvidia",
            api_key,
            model,
            "https://integrate.api.nvidia.com/v1/chat/completions",
            timeout,
        )


class GeminiProvider(BaseProvider):
    """Calls Gemini REST directly because its request shape differs from OpenAI chat."""

    name = "gemini"

    def __init__(self, api_key: str, model: str, timeout: float):
        self.api_key, self.model, self.timeout = api_key, model, timeout

    @property
    def configured(self) -> bool:
        return bool(self.api_key and self.model)

    async def generate(
        self,
        messages: list[dict[str, str]],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
    ) -> ProviderResult:
        system = "\n".join(
            item["content"] for item in messages if item["role"] == "system"
        )
        contents = [
            {
                "role": "model" if item["role"] == "assistant" else "user",
                "parts": [{"text": item["content"]}],
            }
            for item in messages
            if item["role"] != "system"
        ]
        body: dict[str, Any] = {
            "contents": contents,
            "generationConfig": {
                "maxOutputTokens": max_tokens,
                "temperature": temperature,
            },
        }
        if system:
            body["systemInstruction"] = {"parts": [{"text": system}]}
        if json_mode:
            body["generationConfig"]["responseMimeType"] = "application/json"
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"
        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(url, json=body)
        except (httpx.TimeoutException, httpx.NetworkError) as error:
            raise ProviderError("network or timeout error") from error
        latency = int((time.perf_counter() - start) * 1000)
        if response.status_code >= 400:
            retry_after = response.headers.get("Retry-After")
            raise ProviderError(
                "provider returned an error",
                response.status_code,
                int(retry_after) if retry_after and retry_after.isdigit() else None,
            )
        try:
            payload = response.json()
            text = payload["candidates"][0]["content"]["parts"][0]["text"]
        except (ValueError, KeyError, IndexError, TypeError) as error:
            raise ProviderError("provider returned an invalid response") from error
        if not isinstance(text, str) or not text.strip():
            raise ProviderError("provider returned an empty response")
        return ProviderResult(text.strip(), self.name, self.model, latency, payload)


class FakeProvider(BaseProvider):
    """Supplies deterministic text only when explicit test configuration enables it."""

    name = "fake"
    model = "test-only"

    def __init__(self, enabled: bool):
        self.enabled = enabled

    @property
    def configured(self) -> bool:
        return self.enabled

    async def generate(
        self,
        messages: list[dict[str, str]],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
    ) -> ProviderResult:
        import time

        prompt = "\n".join(message["content"] for message in messages)
        if json_mode:
            if "petitioner_arguments" in prompt:
                text = '{"petitioner_arguments":[],"respondent_arguments":[],"court_findings":[]}'
            elif "per_document" in prompt:
                text = '{"per_document":{},"similarities":[],"differences":[],"conclusion":"Fixture comparison."}'
            elif "facts" in prompt.lower() and "final_order" in prompt:
                text = '{"facts":"Fixture facts [S1].","issues":"Fixture issue [S1].","arguments":"Fixture arguments [S1].","reasoning":"Fixture reasoning [S1].","holding":"Fixture holding [S1].","final_order":"The licensing order is set aside [S1]."}'
            else:
                text = '{"court":"High Court of Sample State","case_number":"42 of 2024","judgment_date":null,"parties":[]}'
        else:
            text = "The licensing order is set aside and the matter is remanded [S1]."
        return ProviderResult(text, self.name, self.model, 1, {})
