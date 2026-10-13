from app.ingestion.cleaner import clean_pages
from app.ingestion.sections import detect_sections
from app.ingestion.chunker import chunk_sections


def test_cleaner_removes_repeated_header_and_joins_hyphenation():
    pages = [
        "HIGH COURT\nFacts\nThe appli-\ncation was filed.\nPage 1",
        "HIGH COURT\nReasoning\nThe court agrees.\nPage 2",
    ]
    cleaned = clean_pages(pages)
    assert "HIGH COURT" not in cleaned[0] and "application" in cleaned[0]


def test_section_detector_assigns_heading():
    result = detect_sections(
        ["Facts\nThe dispute began.", "Reasoning\nThe court considered evidence."]
    )
    assert [item.section for item in result] == ["Facts", "Reasoning"]


def test_chunker_never_crosses_sections():
    sections = detect_sections(
        ["Facts\n" + "word " * 600, "Reasoning\n" + "reason " * 600]
    )
    chunks = chunk_sections(sections)
    assert all(chunk.section in {"Facts", "Reasoning"} for chunk in chunks)
    assert chunks[0].section == "Facts"
