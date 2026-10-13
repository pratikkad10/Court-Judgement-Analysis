"""Measures retrieval page coverage and citation accuracy against the fixture gold set."""

from __future__ import annotations
import argparse, asyncio, json, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.retrieval.service import retrieve


async def evaluate(document_id: str, gold_path: Path) -> None:
    """Prints Recall@5, MRR and citation-page accuracy for indexed fixture questions."""
    gold = json.loads(gold_path.read_text())
    hits = reciprocal_rank = citation_hits = 0
    for item in gold:
        rows = await retrieve(item["question"], [document_id])
        expected = item["expected_page"]
        rank = next(
            (
                index
                for index, row in enumerate(rows, 1)
                if row["page_start"] <= expected <= row["page_end"]
            ),
            None,
        )
        if rank:
            hits += 1
            reciprocal_rank += 1 / rank
            citation_hits += 1
    count = len(gold)
    print(f"Recall@5: {hits/count:.3f}")
    print(f"MRR: {reciprocal_rank/count:.3f}")
    print(f"Citation accuracy: {citation_hits/count:.3f}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--document-id", required=True)
    parser.add_argument(
        "--gold", type=Path, default=Path(__file__).with_name("gold.json")
    )
    args = parser.parse_args()
    asyncio.run(evaluate(args.document_id, args.gold))
