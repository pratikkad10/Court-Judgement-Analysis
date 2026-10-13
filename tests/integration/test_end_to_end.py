"""Exercises registration, upload, ingestion and cited chat through the Nginx API boundary."""

from __future__ import annotations
import os, time
from pathlib import Path
import pytest

requests = pytest.importorskip("requests")
pytestmark = pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION") != "true",
    reason="Start compose with TEST_FAKE_LLM=true and set RUN_INTEGRATION=true",
)


def test_upload_ingest_and_cited_query():
    base = os.getenv("JUDGMENT_ANALYZER_BASE_URL", "http://localhost") + "/api"
    email = f"integration-{int(time.time())}@example.com"
    auth = requests.post(
        f"{base}/auth/register",
        json={"email": email, "password": "integration-password"},
        timeout=20,
    ).json()["data"]
    headers = {"Authorization": f"Bearer {auth['accessToken']}"}
    fixture = Path(__file__).parents[1] / "integration" / "sample_judgment.pdf"
    uploaded = requests.post(
        f"{base}/documents",
        headers=headers,
        files={
            "file": ("sample_judgment.pdf", fixture.read_bytes(), "application/pdf")
        },
        timeout=30,
    )
    assert uploaded.status_code == 201, uploaded.text
    document_id = uploaded.json()["data"]["id"]
    for _ in range(90):
        docs = requests.get(f"{base}/documents", headers=headers, timeout=20).json()[
            "data"
        ]
        status = next(item["status"] for item in docs if item["id"] == document_id)
        if status == "READY":
            break
        assert status != "FAILED"
        time.sleep(2)
    assert status == "READY"
    session = requests.post(
        f"{base}/chat/sessions",
        headers=headers,
        json={"title": "fixture research"},
        timeout=20,
    ).json()["data"]
    response = requests.post(
        f"{base}/chat/sessions/{session['id']}/messages",
        headers=headers,
        json={"documentIds": [document_id], "question": "What was the final order?"},
        timeout=40,
    )
    assert response.status_code == 200, response.text
    message = response.json()["data"]
    assert message["citations"] and message["citations"][0]["page_start"] >= 1
