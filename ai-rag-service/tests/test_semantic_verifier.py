import numpy as np
from app.guardrails.verifier import SemanticCitationVerifier


class FakeModel:
    def encode(self, texts, **kwargs):
        return np.array([[1.0, 0.0], [1.0, 0.0]])


def test_semantic_verifier_keeps_supported_cited_sentence(monkeypatch):
    monkeypatch.setattr("app.guardrails.verifier.get_model", lambda: FakeModel())
    answer, partial = SemanticCitationVerifier().verify(
        "The court dismissed the appeal [S1].",
        {"S1": "The court dismissed the appeal."},
    )
    assert "dismissed" in answer and not partial
