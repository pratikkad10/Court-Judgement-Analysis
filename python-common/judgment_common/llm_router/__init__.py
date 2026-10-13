"""LLM provider adapters and fallback routing."""

from .base import BaseProvider, ProviderError, ProviderResult
from .providers import (
    FakeProvider,
    GeminiProvider,
    GroqProvider,
    HuggingFaceProvider,
    NvidiaProvider,
)
from .router import AllProvidersFailedError, LlmRouter


def build_router(settings):
    """Builds the ordered configured adapters from the injected settings object."""
    available = {
        "groq": GroqProvider(
            settings.groq_api_key, settings.groq_model, settings.llm_timeout_seconds
        ),
        "gemini": GeminiProvider(
            settings.gemini_api_key, settings.gemini_model, settings.llm_timeout_seconds
        ),
        "nvidia": NvidiaProvider(
            settings.nvidia_api_key, settings.nvidia_model, settings.llm_timeout_seconds
        ),
        "huggingface": HuggingFaceProvider(
            settings.hf_api_key, settings.hf_model, settings.llm_timeout_seconds
        ),
        "fake": FakeProvider(settings.test_fake_llm),
    }
    return LlmRouter(
        [available[name] for name in settings.provider_names() if name in available],
        settings.llm_circuit_cooldown_seconds,
    )
