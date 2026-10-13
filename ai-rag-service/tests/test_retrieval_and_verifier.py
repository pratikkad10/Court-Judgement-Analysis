from app.retrieval.rrf import reciprocal_rank_fusion
from judgment_common.guardrails.output import CitationVerifier


def test_rrf_merges_rankings_and_rewards_shared_result():
    results = reciprocal_rank_fusion(
        [[{"id": "a"}, {"id": "b"}], [{"id": "b"}, {"id": "c"}]]
    )
    assert results[0]["id"] == "b" and len(results) == 3


def test_citation_verifier_rejects_missing_marker():
    verified = CitationVerifier().verify(
        "Unsupported assertion.", {"S1": "source text"}
    )
    assert verified.partially_verified and "not found" in verified.answer.lower()
