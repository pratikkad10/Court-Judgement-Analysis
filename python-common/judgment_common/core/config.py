"""Reads shared AI settings exclusively from environment variables."""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Contains runtime settings with safe development defaults where possible."""

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    internal_service_key: str = ""
    llm_provider_order: str = "groq,gemini,nvidia,huggingface"
    groq_api_key: str = ""
    groq_model: str = ""
    gemini_api_key: str = ""
    gemini_model: str = ""
    nvidia_api_key: str = ""
    nvidia_model: str = ""
    hf_api_key: str = ""
    hf_model: str = ""
    llm_timeout_seconds: float = 30
    llm_circuit_cooldown_seconds: int = 60
    embedding_model: str = ""
    rag_min_similarity: float = 0.30
    rerank_enabled: bool = False
    rerank_model: str = ""
    test_fake_llm: bool = False
    database_url: str = ""
    document_service_url: str = ""

    def provider_names(self) -> list[str]:
        return [
            name.strip().lower()
            for name in self.llm_provider_order.split(",")
            if name.strip()
        ]


@lru_cache
def get_settings() -> Settings:
    """Caches environment parsing to keep dependency injection lightweight."""
    return Settings()
