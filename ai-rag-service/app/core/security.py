"""Defines private-service authentication dependencies."""

from fastapi import Header, HTTPException
from judgment_common.core.config import get_settings


def require_internal_key(x_internal_key: str = Header(default="")) -> None:
    """Allows only mesh calls carrying the configured internal key."""
    settings = get_settings()
    if not settings.internal_service_key:
        raise RuntimeError("INTERNAL_SERVICE_KEY is required")
    if x_internal_key != settings.internal_service_key:
        raise HTTPException(
            status_code=401, detail="Service authentication is required."
        )
