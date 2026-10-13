"""Secures private FastAPI endpoints and preserves correlation IDs."""

from __future__ import annotations
from fastapi import Header, HTTPException, Request
from judgment_common.core.config import get_settings


def require_internal_key(x_internal_key: str = Header(default="")) -> None:
    """Rejects requests that do not originate from a trusted service."""
    key = get_settings().internal_service_key
    if not key:
        raise RuntimeError("INTERNAL_SERVICE_KEY is required")
    if x_internal_key != key:
        raise HTTPException(
            status_code=401, detail="Service authentication is required."
        )


def correlation_id(request: Request) -> str:
    """Gets the propagated correlation ID or creates a request-local fallback."""
    return request.headers.get("X-Correlation-Id", "missing-correlation-id")
