"""Input and output guardrails for grounded legal research."""

from .input import check_input, mask_personal_data, InputDecision
from .output import CitationVerifier, VerificationResult, DISCLAIMER
