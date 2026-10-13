"""Adds a correlation ID field to every Python service log record."""

from contextvars import ContextVar
import logging

correlation_id: ContextVar[str] = ContextVar("correlation_id", default="missing")


class CorrelationFilter(logging.Filter):
    """Supplies a request correlation ID even for library log records."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.correlation_id = correlation_id.get()
        return True


def configure_logging() -> None:
    """Installs one structured-readable console formatter on root handlers."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s correlation_id=%(correlation_id)s %(name)s %(message)s",
    )
    for handler in logging.getLogger().handlers:
        handler.addFilter(CorrelationFilter())
