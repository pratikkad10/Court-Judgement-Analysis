"""Extracts PDF text page by page and uses OCR only when it is available."""

from __future__ import annotations
from pathlib import Path
import fitz


def extract_pages(storage_path: str) -> list[str]:
    """Reads each PDF page independently to preserve page citations."""
    source = Path(storage_path)
    if not source.is_file():
        raise FileNotFoundError("The uploaded PDF is not available on shared storage.")
    document = fitz.open(source)
    pages: list[str] = []
    for page in document:
        text = page.get_text("text").strip()
        if len(text) < 30:
            text = _ocr_page(page) or text
        pages.append(text)
    document.close()
    return pages


def _ocr_page(page: fitz.Page) -> str | None:
    """Uses optional pytesseract without making OCR a container requirement."""
    try:
        import pytesseract
        from PIL import Image
    except ImportError:
        return None
    pixmap = page.get_pixmap(matrix=fitz.Matrix(2, 2))
    image = Image.frombytes("RGB", [pixmap.width, pixmap.height], pixmap.samples)
    return pytesseract.image_to_string(image).strip()
